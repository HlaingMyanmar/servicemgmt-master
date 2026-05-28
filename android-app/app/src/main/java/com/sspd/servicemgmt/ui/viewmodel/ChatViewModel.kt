package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.ChatMessageDTO
import com.sspd.servicemgmt.api.SendMessageRequest
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(ChatUiState(myUsername = prefs.username))
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    init { startPolling() }

    private fun startPolling() {
        viewModelScope.launch {
            while (true) {
                fetchMessages()
                delay(5000)
            }
        }
    }

    private suspend fun fetchMessages() {
        try {
            val res = ApiClient.service.getChatMessages(ApiClient.bearer(prefs.authToken))
            if (res.isSuccessful) {
                val msgs = res.body()?.data ?: emptyList()
                _uiState.update { it.copy(messages = msgs) }
            }
        } catch (_: Exception) {}
    }

    fun setInput(text: String) = _uiState.update { it.copy(input = text) }

    fun sendMessage() {
        val text = _uiState.value.input.trim()
        if (text.isEmpty() || _uiState.value.sending) return
        viewModelScope.launch {
            _uiState.update { it.copy(sending = true, input = "") }
            try {
                ApiClient.service.sendMessage(ApiClient.bearer(prefs.authToken), SendMessageRequest(text))
                fetchMessages()
            } catch (_: Exception) {}
            _uiState.update { it.copy(sending = false) }
        }
    }

    data class ChatUiState(
        val messages:   List<ChatMessageDTO> = emptyList(),
        val input:      String               = "",
        val sending:    Boolean              = false,
        val myUsername: String               = ""
    )
}
