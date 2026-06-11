package org.dietai.project.ui.dietitian

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.dietai.project.network.*

data class Danisan(
    val uid: String = "",
    val adSoyad: String = "",
    val email: String = "",
    val vki: Double = 0.0,
    val durum: String = ""
)

class DietitianViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(DietitianUiState())
    val uiState: StateFlow<DietitianUiState> = _uiState

    init {
        loadClients()
    }

    fun loadClients() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            try {
                val allUsers = db.collection("users").get()
                val clients = mutableListOf<Danisan>()

                val currentUserId = auth.currentUser?.uid

                for (doc in allUsers.documents) {
                    val role = try { doc.get<String>("rol") } catch(e: Exception) { "" }
                    val assignedDietitianId = try { doc.get<String>("diyetisyenId") } catch(e: Exception) { null }

                    if (role == "Danışan" && assignedDietitianId == currentUserId) {
                        val uid = doc.id
                        val name = try { doc.get<String>("adSoyad") } catch(e: Exception) { "İsimsiz" }
                        val email = try { doc.get<String>("email") } catch(e: Exception) { "" }
                        val bmi = try { doc.get<Double>("vki") } catch (e: Exception) { 0.0 }

                        val status = when {
                            bmi == 0.0 -> "Analiz Yok"
                            bmi < 18.5 -> "Zayıf"
                            bmi < 25 -> "Normal"
                            bmi < 30 -> "Fazla Kilolu"
                            else -> "Obezite"
                        }
                        clients.add(Danisan(uid, name, email, bmi, status))
                    }
                }
                _uiState.value = _uiState.value.copy(clientList = clients, isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
            }
        }
    }



    fun loadClientDetails(userId: String) {
        _uiState.value = _uiState.value.copy(isLoadingDetails = true)
        viewModelScope.launch {
            try {
                val doc = db.collection("users").document(userId).get()
                val client = _uiState.value.clientList.find { it.uid == userId }
                val currentDiet = try { doc.get<String>("diyetListesi") } catch (e: Exception) { "" }
                
                val height = try { doc.get<Double>("boy").toString() } catch (e: Exception) { "-" }
                val weight = try { doc.get<Double>("kilo").toString() } catch (e: Exception) { "-" }

                // Fetch Weight History
                val weightHistoryParams = db.collection("users").document(userId).collection("weightHistory").get()
                val weightHistory = weightHistoryParams.documents.map { d ->
                    ClientWeightEntry(
                        date = try { d.get("date") } catch (e: Exception) { "" },
                        weight = try { d.get<Double>("weight") } catch (e: Exception) { 0.0 }
                    )
                }.sortedByDescending { it.date }

                val mealsParams = db.collection("users").document(userId).collection("meals").get()
                val meals = mealsParams.documents.map { d ->
                    ClientMealEntry(
                        description = try { d.get("description") } catch (e: Exception) { "" },
                        calories = try { d.get<Int>("calories") } catch (e: Exception) { 0 },
                        timestamp = try { d.get("timestamp") } catch (e: Exception) { "" }
                    )
                }.sortedByDescending { it.timestamp }

                // Fetch Exercises
                val exercisesParams = db.collection("users").document(userId).collection("exercises").get()
                val exercises = exercisesParams.documents.map { d ->
                    ClientExerciseEntry(
                        type = try { d.get("type") } catch(e:Exception) {""},
                        duration = try { d.get("duration") } catch(e:Exception) {""},
                        burnedCalories = try { d.get<Int>("burnedCalories") } catch(e:Exception) { 0 },
                        isCompleted = try { d.get<Boolean>("isCompleted") } catch(e:Exception) { false },
                        timestamp = try { d.get("timestamp") } catch(e:Exception) { "Bugün" }
                    )
                }.sortedByDescending { it.timestamp }

                val target = try { doc.get<String>("dailyExerciseTarget") } catch (e: Exception) { "" }

                _uiState.value = _uiState.value.copy(
                    selectedClient = client,
                    currentDietPlan = currentDiet,
                    clientHeight = height,
                    clientWeight = weight,
                    clientWeightHistory = weightHistory,
                    clientMeals = meals,
                    clientExercises = exercises,
                    clientExerciseTarget = target,
                    isLoadingDetails = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingDetails = false, error = "Detay yüklenemedi")
            }
        }
    }

    fun saveDietPlan(userId: String, dietText: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update(mapOf("diyetListesi" to dietText))
                _uiState.value = _uiState.value.copy(saveMessage = "✅ Diyet listesi başarıyla kaydedildi ve danışana gönderildi.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "❌ Kayıt Hatası: ${e.message}")
            }
        }
    }

    fun setExerciseTarget(userId: String, targetStr: String) {
        viewModelScope.launch {
            try {
                db.collection("users").document(userId).update(mapOf("dailyExerciseTarget" to targetStr))
                _uiState.value = _uiState.value.copy(clientExerciseTarget = targetStr, saveMessage = "✅ Hedef atandı.")
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(saveMessage = "❌ Hata: ${e.message}")
            }
        }
    }
    
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(saveMessage = null)
    }

    fun generateDietPlanWithAI(userId: String) {
        val client = _uiState.value.selectedClient ?: return
        
        _uiState.value = _uiState.value.copy(isLoadingDetails = true, saveMessage = "Yapay Zeka plan oluşturuyor, lütfen bekleyin... ⏳")
        viewModelScope.launch {
            try {
                val apiKey = org.dietai.project.Config.GEMINI_API_KEY
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$apiKey"

                val httpClient = HttpClient {
                    install(ContentNegotiation) {
                        json(Json { ignoreUnknownKeys = true; isLenient = true; encodeDefaults = true })
                    }
                    install(HttpTimeout) {
                        requestTimeoutMillis = 60000
                        connectTimeoutMillis = 60000
                        socketTimeoutMillis = 60000
                    }
                }

                val systemPrompt = "Sen uzman bir diyetisyensin. Aşağıda bilgileri verilen danışan için 7 günlük sağlıklı, dengeli ve profesyonel bir diyet listesi oluştur. Lütfen listeyi net ve okunabilir bir şekilde 'Pazartesi: Sabah: ... Öğle: ... Akşam: ...' formatında ver. Ekstra sohbet veya giriş cümlesi kurma, sadece diyeti liste halinde ver."
                val userMsg = "Danışan Bilgileri:\n- İsim: ${client.adSoyad}\n- Vücut Kitle İndeksi (VKİ): ${client.vki}\n- Kilo Durumu: ${client.durum}\nLütfen bu danışana uygun 7 günlük bir diyet taslağı hazırla."

                val response = httpClient.post(url) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        GeminiRequest(
                            systemInstruction = SystemInstruction(parts = listOf(Part(text = systemPrompt))),
                            contents = listOf(Content(parts = listOf(Part(text = userMsg))))
                        )
                    )
                }

                if (response.status == HttpStatusCode.OK) {
                    val responseBody = response.bodyAsText()
                    val json = Json.parseToJsonElement(responseBody)
                    val generatedText = json.jsonObject["candidates"]?.jsonArray?.getOrNull(0)
                        ?.jsonObject?.get("content")?.jsonObject?.get("parts")
                        ?.jsonArray?.getOrNull(0)?.jsonObject?.get("text")?.jsonPrimitive?.content ?: "Liste oluşturulamadı."
                    
                    _uiState.value = _uiState.value.copy(
                        currentDietPlan = generatedText, 
                        isLoadingDetails = false, 
                        saveMessage = "✨ Yapay Zeka başarıyla taslağı oluşturdu! Lütfen inceleyip kaydedin."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoadingDetails = false,
                        saveMessage = "Hata: ${response.status.value}"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoadingDetails = false,
                    saveMessage = "Bağlantı Hatası: ${e.message}"
                )
            }
        }
    }
}

data class ClientWeightEntry(val date: String, val weight: Double)
data class ClientMealEntry(val description: String, val calories: Int, val timestamp: String)
data class ClientExerciseEntry(val type: String, val duration: String, val burnedCalories: Int, val isCompleted: Boolean, val timestamp: String)

data class DietitianUiState(
    val clientList: List<Danisan> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    
    val selectedClient: Danisan? = null,
    val currentDietPlan: String = "",
    val clientHeight: String = "",
    val clientWeight: String = "",
    val clientWeightHistory: List<ClientWeightEntry> = emptyList(),
    val clientMeals: List<ClientMealEntry> = emptyList(),
    val clientExercises: List<ClientExerciseEntry> = emptyList(),
    val clientExerciseTarget: String = "",
    val isLoadingDetails: Boolean = false,
    val saveMessage: String? = null
)
