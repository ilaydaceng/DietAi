package org.dietai.project.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    init {
        loadUserProfile()
        loadWeeklyStats()
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                _uiState.value = _uiState.value.copy(email = user.email ?: "")
                try {
                    val doc = db.collection("users").document(user.uid).get()
                    val name = try { doc.get<String>("adSoyad") } catch (e: Exception) { "Kullanıcı" }
                    val role = try { doc.get<String>("rol") } catch (e: Exception) { "Danışan" }
                    val height = try { doc.get<Double>("boy").toString() } catch (e: Exception) { "-" }
                    val weight = try { doc.get<Double>("kilo").toString() } catch (e: Exception) { "-" }

                    _uiState.value = _uiState.value.copy(
                        name = name,
                        role = role,
                        height = height,
                        weight = weight
                    )
                } catch (e: Exception) {
                    // Handle error
                }
            }
        }
    }

    private fun loadWeeklyStats() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val mealsParams = db.collection("users").document(user.uid).collection("meals").get()
                    val exercisesParams = db.collection("users").document(user.uid).collection("exercises").get()

                    val intakeMap = mutableMapOf<String, Float>()
                    mealsParams.documents.forEach { d ->
                        val cal = try { d.get<Long>("calories").toFloat() } catch (e: Exception) { try { d.get<Int>("calories").toFloat() } catch (e: Exception) { 0f } }
                        val date = try { d.get<String>("timestamp") } catch (e: Exception) { "" }
                        if (date.isNotBlank()) {
                            intakeMap[date] = (intakeMap[date] ?: 0f) + cal
                        }
                    }

                    val burnedMap = mutableMapOf<String, Float>()
                    exercisesParams.documents.forEach { d ->
                        val isComp = try { d.get<Boolean>("isCompleted") } catch (e: Exception) { false }
                        if (isComp) {
                            val burned = try { d.get<Long>("burnedCalories").toFloat() } catch (e: Exception) { try { d.get<Int>("burnedCalories").toFloat() } catch (e: Exception) { 0f } }
                            val date = try { d.get<String>("timestamp") } catch (e: Exception) { "" }
                            if (date.isNotBlank()) {
                                burnedMap[date] = (burnedMap[date] ?: 0f) + burned
                            }
                        }
                    }

                    // Basitlik için son 7 günü manuel veya var olan tarihlerden en yenilerini alabiliriz.
                    // Şimdilik Firebase'deki tüm verileri toplayıp listeye aktaracağız (Örnek olarak)
                    val allDates = (intakeMap.keys + burnedMap.keys).toSet().sorted()
                    // Haftalık (Son 7 gün)
                    val recentDates = allDates.takeLast(7)
                    val wDays = mutableListOf<String>()
                    val wIntake = mutableListOf<Float>()
                    val wBurned = mutableListOf<Float>()
                    recentDates.forEach { d ->
                        wDays.add(d.takeLast(5))
                        wIntake.add(intakeMap[d] ?: 0f)
                        wBurned.add(burnedMap[d] ?: 0f)
                    }

                    // Aylık (Son 30 günü 4 haftaya bölmek için basitleştirilmiş bir algoritma)
                    // Gerçek projelerde hafta hafta gruplanır, burada son 4 haftayı (son 28 gün) 4 sütunda göstereceğiz
                    val mDays = listOf("1.Hft", "2.Hft", "3.Hft", "4.Hft")
                    val mIntake = mutableListOf(0f, 0f, 0f, 0f)
                    val mBurned = mutableListOf(0f, 0f, 0f, 0f)
                    
                    val monthDates = allDates.takeLast(28)
                    monthDates.forEachIndexed { index, d ->
                        val weekIdx = index / 7
                        if (weekIdx < 4) {
                            mIntake[weekIdx] += (intakeMap[d] ?: 0f)
                            mBurned[weekIdx] += (burnedMap[d] ?: 0f)
                        }
                    }

                    _uiState.value = _uiState.value.copy(
                        weeklyDays = if(wDays.isEmpty()) listOf("Yok") else wDays,
                        weeklyIntake = if(wIntake.isEmpty()) listOf(0f) else wIntake,
                        weeklyBurned = if(wBurned.isEmpty()) listOf(0f) else wBurned,
                        monthlyDays = mDays,
                        monthlyIntake = mIntake,
                        monthlyBurned = mBurned
                    )
                } catch (e: Exception) {
                    // Hata durumunda boş veri
                }
            }
        }
    }

    fun updateProfile(newName: String, newHeight: String, newWeight: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val updates = mutableMapOf<String, Any>(
                        "adSoyad" to newName
                    )
                    
                    newHeight.toDoubleOrNull()?.let { updates["boy"] = it }
                    newWeight.toDoubleOrNull()?.let { updates["kilo"] = it }

                    db.collection("users").document(user.uid).update(updates)
                    
                    // Update local state
                    _uiState.value = _uiState.value.copy(
                        name = newName,
                        height = newHeight,
                        weight = newWeight,
                        message = "Profil güncellendi! ✅"
                    )
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(message = "Hata: ${e.message}")
                }
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                auth.signOut()
                _uiState.value = _uiState.value.copy(isSignedOut = true)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun sendPasswordResetEmail() {
        viewModelScope.launch {
            val email = _uiState.value.email
            if (email.isNotBlank()) {
                try {
                    auth.sendPasswordResetEmail(email)
                    _uiState.value = _uiState.value.copy(message = "Şifre sıfırlama e-postası gönderildi.")
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(message = "Hata: ${e.message}")
                }
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class ProfileUiState(
    val name: String = "Yükleniyor...",
    val email: String = "",
    val role: String = "",
    val height: String = "-",
    val weight: String = "-",
    val isSignedOut: Boolean = false,
    val message: String? = null,
    val weeklyDays: List<String> = listOf("Pzt", "Sal", "Çar", "Per", "Cum", "Cmt", "Paz"),
    val weeklyIntake: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val weeklyBurned: List<Float> = listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f),
    val monthlyDays: List<String> = listOf("1.Hft", "2.Hft", "3.Hft", "4.Hft"),
    val monthlyIntake: List<Float> = listOf(0f, 0f, 0f, 0f),
    val monthlyBurned: List<Float> = listOf(0f, 0f, 0f, 0f)
)
