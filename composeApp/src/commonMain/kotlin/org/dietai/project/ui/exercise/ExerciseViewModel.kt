package org.dietai.project.ui.exercise

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.*
import org.dietai.project.network.*

data class ExerciseEntry(
    val id: String = "",
    val type: String = "",
    val duration: String = "",
    val timestamp: String = "", // e.g. "2023-10-25"
    val isCompleted: Boolean = false,
    val burnedCalories: Int = 0
)

class ExerciseViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow(ExerciseUiState())
    val uiState: StateFlow<ExerciseUiState> = _uiState

    init {
        // Initialize selected date to today
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
        _uiState.value = _uiState.value.copy(selectedDate = today)
        loadExercises()
    }

    fun setSelectedDate(dateStr: String) {
        _uiState.value = _uiState.value.copy(selectedDate = dateStr)
    }

    fun loadExercises() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val snapshot = db.collection("users").document(user.uid).collection("exercises").get()
                    
                    val exercises = snapshot.documents.map { doc ->
                        ExerciseEntry(
                            id = doc.id,
                            type = try { doc.get("type") } catch(e:Exception) {""},
                            duration = try { doc.get("duration") } catch(e:Exception) {""},
                            timestamp = try { doc.get("timestamp") } catch(e:Exception) { "Bugün" },
                            isCompleted = try { doc.get("isCompleted") } catch(e:Exception) { false },
                            burnedCalories = try { doc.get("burnedCalories") } catch(e:Exception) { 0 }
                        )
                    }

                    // Diyetisyenin belirlediği hedefi de çek
                    val userDoc = db.collection("users").document(user.uid).get()
                    val target = try { userDoc.get<String>("dailyExerciseTarget") } catch(e:Exception) { "" }

                    _uiState.value = _uiState.value.copy(allExercises = exercises, dailyTarget = target, isLoading = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun addExercise(type: String, duration: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            val selectedDate = _uiState.value.selectedDate
            if (user != null) {
                val durInt = duration.toIntOrNull() ?: 0
                
                // Dinamik Kalori Hesabı (Ortalama 70kg baz alınarak)
                val caloriesPerMin = when (type.lowercase()) {
                    "kardiyo" -> 10
                    "ağırlık" -> 6
                    "yürüyüş" -> 5
                    "yoga" -> 4
                    "yüzme" -> 8
                    else -> 5
                }
                val burned = durInt * caloriesPerMin

                val newExercise = mapOf(
                    "type" to type,
                    "duration" to duration,
                    "timestamp" to selectedDate,
                    "isCompleted" to false,
                    "burnedCalories" to burned
                )
                db.collection("users").document(user.uid).collection("exercises").add(newExercise)
                loadExercises() // Refresh
            }
        }
    }

    fun toggleExerciseStatus(exerciseId: String, isCompleted: Boolean) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                // Optimistic update
                val currentList = _uiState.value.allExercises.toMutableList()
                val index = currentList.indexOfFirst { it.id == exerciseId }
                if (index != -1) {
                    currentList[index] = currentList[index].copy(isCompleted = isCompleted)
                    _uiState.value = _uiState.value.copy(allExercises = currentList)
                    
                    try {
                        db.collection("users").document(user.uid)
                            .collection("exercises").document(exerciseId)
                            .update(mapOf("isCompleted" to isCompleted))
                    } catch (e: Exception) {
                        // Revert on error if needed
                    }
                }
            }
        }
    }

    fun getAiExerciseSuggestion() {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            _uiState.value = _uiState.value.copy(aiSuggestionLoading = true)

            try {
                // Bugünün kalorilerini topla
                val mealsParams = db.collection("users").document(user.uid).collection("meals").get()
                var totalCalories = 0
                val selectedDate = _uiState.value.selectedDate
                mealsParams.documents.forEach { d ->
                    val date = try { d.get<String>("timestamp") } catch (e: Exception) { "" }
                    if (date == selectedDate) {
                        totalCalories += try { d.get<Long>("calories").toInt() } catch (e: Exception) { try { d.get<Int>("calories") } catch (e: Exception) { 0 } }
                    }
                }

                // AI Prompt'u hazırla
                val prompt = "Ben bugün toplam $totalCalories kalori aldım. Sağlıklı bir yaşam için bunu dengelemek istiyorum. Bana motive edici, çok kısa ve öz (en fazla 2 cümle) bir egzersiz önerisi yapar mısın? Sadece egzersiz adını ve tahmini süresini ver."

                val client = HttpClient {
                    install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
                    install(HttpTimeout) { requestTimeoutMillis = 30000 }
                }

                val requestBody = buildJsonObject {
                    put("contents", buildJsonArray {
                        addJsonObject {
                            put("parts", buildJsonArray {
                                addJsonObject { put("text", prompt) }
                            })
                        }
                    })
                }

                val response: HttpResponse = client.post("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=${org.dietai.project.Config.GEMINI_API_KEY}") {
                    contentType(ContentType.Application.Json)
                    setBody(requestBody.toString())
                }

                val responseBody = response.bodyAsText()
                val jsonResponse = Json { ignoreUnknownKeys = true }.parseToJsonElement(responseBody).jsonObject
                
                var textResponse = jsonResponse["candidates"]?.jsonArray?.get(0)?.jsonObject
                    ?.get("content")?.jsonObject?.get("parts")?.jsonArray?.get(0)?.jsonObject
                    ?.get("text")?.jsonPrimitive?.content

                if (textResponse == null) {
                    val errorCode = jsonResponse["error"]?.jsonObject?.get("code")?.jsonPrimitive?.intOrNull
                    val errorMsg = jsonResponse["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content
                    textResponse = if (errorCode == 503) {
                        "Sistem şu an çok yoğun (Google Gemini Sunucuları). Lütfen birkaç saniye bekleyip tekrar deneyin."
                    } else if (errorMsg != null) {
                        "API Hatası ($errorCode): $errorMsg"
                    } else {
                        "Yapay zekadan yanıt alınamadı. (Güvenlik filtresine takılmış olabilir veya API geçersiz yanıt verdi)"
                    }
                }

                _uiState.value = _uiState.value.copy(aiSuggestion = textResponse, aiSuggestionLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(aiSuggestion = "AI Bağlantı Hatası: ${e.message}", aiSuggestionLoading = false)
            }
        }
    }
}

data class ExerciseUiState(
    val allExercises: List<ExerciseEntry> = emptyList(),
    val selectedDate: String = "",
    val dailyTarget: String = "",
    val aiSuggestion: String? = null,
    val aiSuggestionLoading: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
) {
    val displayedExercises: List<ExerciseEntry>
        get() = allExercises.filter { it.timestamp == selectedDate || (it.timestamp == "Bugün") }
}
