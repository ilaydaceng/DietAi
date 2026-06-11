package org.dietai.project.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.Direction
import dev.gitlive.firebase.firestore.firestore
import dev.gitlive.firebase.firestore.orderBy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class Message(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)

data class DirectMessageUiState(
    val messages: List<Message> = emptyList(),
    val isLoading: Boolean = false,
    val currentUserId: String = ""
)

class DirectMessageViewModel : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    private val _uiState = MutableStateFlow(DirectMessageUiState())
    val uiState: StateFlow<DirectMessageUiState> = _uiState.asStateFlow()

    private var currentChatId: String? = null

    init {
        _uiState.value = _uiState.value.copy(currentUserId = auth.currentUser?.uid ?: "")
    }

    fun loadChat(chatId: String) {
        if (currentChatId == chatId) return
        currentChatId = chatId
        _uiState.value = _uiState.value.copy(isLoading = true)

        viewModelScope.launch {
            try {
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .orderBy("timestamp", Direction.DESCENDING)
                    .limit(50)
                    .snapshots
                    .collect { snapshot ->
                        val msgs = snapshot.documents.mapNotNull { doc ->
                            try {
                                Message(
                                    id = doc.id,
                                    senderId = doc.get<String>("senderId"),
                                    text = doc.get<String>("text"),
                                    timestamp = doc.get<Long>("timestamp")
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }.reversed() // En yeni mesaj en altta olsun diye ters çevir
                        
                        _uiState.value = _uiState.value.copy(
                            messages = msgs,
                            isLoading = false
                        )
                    }
            } catch (e: Exception) {
                println("Mesajları dinleme hatası: ${e.message}")
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun sendMessage(text: String) {
        val chatId = currentChatId ?: return
        val userId = auth.currentUser?.uid ?: return
        if (text.isBlank()) return

        viewModelScope.launch {
            try {
                val now = Clock.System.now().toEpochMilliseconds()
                val msg = mapOf(
                    "senderId" to userId,
                    "text" to text,
                    "timestamp" to now
                )

                // Mesajı alt koleksiyona ekle
                db.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(msg)

                // Ana sohbet dökümanını güncelle (son mesaj)
                db.collection("chats")
                    .document(chatId)
                    .set(
                        mapOf(
                            "lastMessage" to text,
                            "lastMessageTimestamp" to now
                        ),
                        merge = true
                    )
            } catch (e: Exception) {
                println("Mesaj gönderme hatası: ${e.message}")
            }
        }
    }
}
