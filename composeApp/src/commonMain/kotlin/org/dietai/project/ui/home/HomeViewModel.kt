package org.dietai.project.ui.home



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import dev.gitlive.firebase.firestore.DocumentSnapshot // Bu satır safeGetDouble hatasını çözer
class HomeViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        loadUserData()
    }

    // Firebase'den sayısal verileri (Long veya Double gelebilir) güvenli bir şekilde çeken yardımcı fonksiyon
    private fun safeGetDouble(doc: dev.gitlive.firebase.firestore.DocumentSnapshot, field: String): Double {
        return try {
            doc.get<Double>(field)
        } catch (e: Exception) {
            try {
                doc.get<Long>(field).toDouble()
            } catch (e2: Exception) {
                0.0
            }
        }
    }

    private fun loadUserData() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val doc = db.collection("users").document(user.uid).get()

                    // Diyet listesi işlemleri
                    val dietText = try { doc.get<String>("diyetListesi") } catch (e: Exception) { "" }
                    val dietLines = dietText.split("\n").filter { it.isNotBlank() }
                    
                    val savedChecklist = try { doc.get<List<Boolean>>("diyetYapilanlar") } catch (e: Exception) { emptyList() }
                    val checklist = if (savedChecklist.size == dietLines.size) savedChecklist else List(dietLines.size) { false }

                    // Diyetisyen Listesi Çek ve Kayıtlı Olanı Bul
                    val allUsersSnapshot = db.collection("users").get()
                    val dietitiansList = allUsersSnapshot.documents.mapNotNull { d ->
                        try {
                            if (d.get<String>("rol") == "Diyetisyen") {
                                DietitianModel(d.id, try { d.get<String>("adSoyad") } catch(e: Exception) { "İsimsiz Diyetisyen" })
                            } else null
                        } catch (e: Exception) { null }
                    }
                    val assignedDid = try { doc.get<String>("diyetisyenId") } catch(e: Exception) { null }
                    val assignedDName = dietitiansList.find { it.id == assignedDid }?.name

                    // Verileri UI State'e aktarırken güvenli yöntem kullanıyoruz
                    _uiState.value = _uiState.value.copy(
                        name = try { doc.get<String>("adSoyad") } catch (e: Exception) { "Kullanıcı" },
                        // Su verisi Firebase'de sayı olduğu için güvenli alıyoruz
                        waterIntake = try { doc.get<Long>("icilenSu").toInt() } catch (e: Exception) {
                            try { doc.get<Double>("icilenSu").toInt() } catch (e: Exception) { 0 }
                        },
                        dietLines = dietLines,
                        dietChecklist = checklist,
                        boy = safeGetDouble(doc, "boy").let { if(it > 0) it.toInt().toString() else "" },
                        kilo = safeGetDouble(doc, "kilo").let { if(it > 0) it.toString() else "" },
                        vki = safeGetDouble(doc, "vki"),
                        diyetisyenId = assignedDid,
                        diyetisyenName = assignedDName,
                        availableDietitians = dietitiansList
                    )

                    loadWeightHistory(user.uid)
                } catch (e: Exception) {
                    println("Veri yükleme hatası: ${e.message}")
                    _uiState.value = _uiState.value.copy(name = "Hata oluştu")
                }
            }
        }
    }

    private fun loadWeightHistory(uid: String) {
        viewModelScope.launch {
            try {
                val historySnapshot = db.collection("users").document(uid).collection("weightHistory").get()

                val history = historySnapshot.documents.map { doc ->
                    val weight = safeGetDouble(doc, "weight")
                    val date = try { doc.get<String>("date") } catch (e: Exception) { "Bilinmiyor" }
                    WeightEntry(date, weight)
                }.sortedByDescending { it.date }

                _uiState.value = _uiState.value.copy(weightHistory = history)
            } catch (e: Exception) {
                println("Geçmiş yükleme hatası: ${e.message}")
            }
        }
    }

    fun saveWeight(newWeightStr: String) {
        val newWeight = newWeightStr.toDoubleOrNull() ?: return
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                db.collection("users").document(user.uid).update(mapOf("kilo" to newWeight))

                val entry = mapOf("weight" to newWeight, "date" to "Bugün")
                db.collection("users").document(user.uid).collection("weightHistory").add(entry)

                _uiState.value = _uiState.value.copy(kilo = newWeightStr)
                loadWeightHistory(user.uid)
            }
        }
    }

    fun updateWater(amount: Int) {
        val newAmount = if (amount < 0) 0 else amount
        // Önce UI'ı hemen güncelle ki donma hissi olmasın
        _uiState.value = _uiState.value.copy(waterIntake = newAmount)

        // Veritabanını arka planda güncelle
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    db.collection("users").document(user.uid).update(mapOf("icilenSu" to newAmount))
                } catch (e: Exception) {
                    println("Su güncelleme hatası: ${e.message}")
                }
            }
        }
    }

    fun toggleDietItem(index: Int, isChecked: Boolean) {
        val currentList = _uiState.value.dietChecklist.toMutableList()
        if (index in currentList.indices) {
            currentList[index] = isChecked
            _uiState.value = _uiState.value.copy(dietChecklist = currentList)
            updateDb("diyetYapilanlar", currentList)
        }
    }

    fun calculateAndSaveVki(boyStr: String, kiloStr: String, yasStr: String, cinsiyet: String) {
        viewModelScope.launch {
            try {
                val boy = boyStr.toDouble()
                val kilo = kiloStr.toDouble()
                val boyMetre = boy / 100.0
                val vki = kilo / (boyMetre * boyMetre)

                _uiState.value = _uiState.value.copy(vki = vki, boy = boyStr, kilo = kiloStr)

                val user = auth.currentUser
                if (user != null) {
                    db.collection("users").document(user.uid).update(
                        mapOf(
                            "boy" to boy,
                            "kilo" to kilo,
                            "vki" to vki
                        )
                    )
                }
            } catch (e: Exception) {
                println("VKI hesaplama hatası: ${e.message}")
            }
        }
    }

    private fun updateDb(field: String, value: Any) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    db.collection("users").document(user.uid).update(mapOf(field to value))
                } catch (e: Exception) {
                    println("DB güncelleme hatası ($field): ${e.message}")
                }
            }
        }
    }
    
    fun selectDietitian(dId: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    db.collection("users").document(user.uid).update(mapOf("diyetisyenId" to dId))
                    val dName = _uiState.value.availableDietitians.find { it.id == dId }?.name
                    _uiState.value = _uiState.value.copy(diyetisyenId = dId, diyetisyenName = dName)
                } catch (e: Exception) {
                    println("Diyetisyen seçme hatası: ${e.message}")
                }
            }
        }
    }
}

data class WeightEntry(val date: String, val weight: Double)
data class DietitianModel(val id: String, val name: String)

data class HomeUiState(
    val name: String = "Yükleniyor...",
    val waterIntake: Int = 0,
    val dietLines: List<String> = emptyList(),
    val dietChecklist: List<Boolean> = emptyList(),
    val boy: String = "",
    val kilo: String = "",
    val vki: Double = 0.0,
    val weightHistory: List<WeightEntry> = emptyList(),
    val diyetisyenId: String? = null,
    val diyetisyenName: String? = null,
    val availableDietitians: List<DietitianModel> = emptyList()
)