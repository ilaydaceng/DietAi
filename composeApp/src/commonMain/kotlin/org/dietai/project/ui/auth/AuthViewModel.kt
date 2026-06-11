package org.dietai.project.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {
    private val auth = Firebase.auth
    private val db = Firebase.firestore

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    fun onEvent(event: AuthEvent) {
        when (event) {
            is AuthEvent.EmailChanged -> _uiState.value = _uiState.value.copy(email = event.email)
            is AuthEvent.PasswordChanged -> _uiState.value = _uiState.value.copy(password = event.password)
            is AuthEvent.NameChanged -> _uiState.value = _uiState.value.copy(name = event.name)
            is AuthEvent.ToggleMode -> _uiState.value = _uiState.value.copy(isRegisterMode = !uiState.value.isRegisterMode)
            is AuthEvent.SelectRole -> _uiState.value = _uiState.value.copy(isDietitian = event.isDietitian)
            is AuthEvent.Submit -> submit()
            is AuthEvent.ErrorDismissed -> _uiState.value = _uiState.value.copy(error = null)
        }
    }

    private fun submit() {
        val state = uiState.value
        if (state.email.isBlank() || state.password.length < 6) {
            _uiState.value = state.copy(error = "Bilgileri kontrol ediniz (Şifre min 6).")
            return
        }

        _uiState.value = state.copy(isLoading = true, error = null)
        viewModelScope.launch {
            try {
                if (state.isRegisterMode) {
                    auth.createUserWithEmailAndPassword(state.email.trim(), state.password)
                    val userId = auth.currentUser?.uid
                    if (userId != null) {
                        val userData = mapOf(
                            "uid" to userId,
                            "adSoyad" to state.name,
                            "email" to state.email.trim(),
                            "rol" to if (state.isDietitian) "Diyetisyen" else "Danışan"
                        )
                        db.collection("users").document(userId).set(userData)
                    }
                } else {
                    auth.signInWithEmailAndPassword(state.email.trim(), state.password)
                }
                _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoading = false, error = e.message ?: "Bir hata oluştu")
            }
        }
    }
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isRegisterMode: Boolean = false,
    val isDietitian: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

sealed class AuthEvent {
    data class EmailChanged(val email: String) : AuthEvent()
    data class PasswordChanged(val password: String) : AuthEvent()
    data class NameChanged(val name: String) : AuthEvent()
    data object ToggleMode : AuthEvent()
    data class SelectRole(val isDietitian: Boolean) : AuthEvent()
    data object Submit : AuthEvent()
    data object ErrorDismissed : AuthEvent()
}
