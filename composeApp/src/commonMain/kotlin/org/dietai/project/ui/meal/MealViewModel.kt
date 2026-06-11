package org.dietai.project.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class MealEntry(
    val id: String = "",
    val description: String = "",
    val timestamp: String = "",
    val type: String = "Kahvaltı",
    val calories: Int = 0,
    val protein: Int = 0,
    val carbs: Int = 0,
    val fat: Int = 0
)

class MealViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow(MealUiState())
    val uiState: StateFlow<MealUiState> = _uiState

    init {
        loadMeals()
    }

    fun loadMeals() {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                _uiState.value = _uiState.value.copy(isLoading = true)
                try {
                    val snapshot = db.collection("users").document(user.uid)
                        .collection("meals").get()

                    val meals = snapshot.documents.map { doc ->
                        MealEntry(
                            id = doc.id,
                            description = try { doc.get("description") } catch (e: Exception) { "" },
                            type = try { doc.get("type") } catch (e: Exception) { "Kahvaltı" },
                            timestamp = try { doc.get("timestamp") } catch (e: Exception) { "" },
                            calories = try { doc.get<Long>("calories").toInt() } catch (e: Exception) { 0 },
                            protein = try { doc.get<Long>("protein").toInt() } catch (e: Exception) { 0 },
                            carbs = try { doc.get<Long>("carbs").toInt() } catch (e: Exception) { 0 },
                            fat = try { doc.get<Long>("fat").toInt() } catch (e: Exception) { 0 }
                        )
                    }
                    _uiState.value = _uiState.value.copy(meals = meals, isLoading = false)
                } catch (e: Exception) {
                    _uiState.value = _uiState.value.copy(isLoading = false, error = e.message)
                }
            }
        }
    }

    fun addMeal(description: String, type: String, calories: Int, protein: Int, carbs: Int, fat: Int) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                val cal = java.util.Calendar.getInstance()
                val saat = cal.get(java.util.Calendar.HOUR_OF_DAY)
                val dakika = cal.get(java.util.Calendar.MINUTE)
                val timeStr = "%02d:%02d".format(saat, dakika)

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
                loadMeals()
            }
        }
    }

    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            val user = auth.currentUser
            if (user != null) {
                try {
                    db.collection("users").document(user.uid)
                        .collection("meals").document(mealId).delete()
                    _uiState.value = _uiState.value.copy(
                        meals = _uiState.value.meals.filter { it.id != mealId }
                    )
                } catch (e: Exception) {
                    loadMeals()
                }
            }
        }
    }
}

data class MealUiState(
    val meals: List<MealEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)