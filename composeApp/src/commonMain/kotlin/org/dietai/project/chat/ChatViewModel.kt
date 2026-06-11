package org.dietai.project.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class UserContextData(
    val adSoyad: String = "",
    val boy: Double = 0.0,
    val kilo: Double = 0.0,
    val vki: Double = 0.0,
    val isLoading: Boolean = true
)

class ChatViewModel : ViewModel() {
    private val _userContext = MutableStateFlow(UserContextData())
    val userContext: StateFlow<UserContextData> = _userContext.asStateFlow()

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        loadUserContext()
    }

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

    private fun loadUserContext() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val doc = db.collection("users").document(user.uid).get()
                    _userContext.value = UserContextData(
                        adSoyad = try { doc.get<String>("adSoyad") } catch (e: Exception) { "" },
                        boy = safeGetDouble(doc, "boy"),
                        kilo = safeGetDouble(doc, "kilo"),
                        vki = safeGetDouble(doc, "vki"),
                        isLoading = false
                    )
                } catch (e: Exception) {
                    _userContext.value = _userContext.value.copy(isLoading = false)
                }
            } else {
                _userContext.value = _userContext.value.copy(isLoading = false)
            }
        }
    }

    fun logMeal(description: String, type: String, calories: Int, protein: Int = 0, carbs: Int = 0, fat: Int = 0) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    val cal = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val saat = cal.hour
                    val dakika = cal.minute
                    val timeStr = "${saat.toString().padStart(2, '0')}:${dakika.toString().padStart(2, '0')}"

                    val newMeal = mapOf(
                        "description" to description,
                        "type" to type,
                        "timestamp" to timeStr,
                        "calories" to calories,
                        "protein" to protein,
                        "carbs" to carbs,
                        "fat" to fat
                    )
                    db.collection("users").document(user.uid).collection("meals").add(newMeal)
                } catch (e: Exception) {
                    println("Yemek kaydetme hatası: ${e.message}")
                }
            }
        }
    }
}
