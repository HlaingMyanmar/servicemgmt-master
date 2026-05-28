package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AccountSettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(AccountSettingsUiState(
        username    = prefs.username,
        displayName = prefs.displayName.ifEmpty { prefs.username },
        serverInput = prefs.serverUrl
    ))
    val uiState: StateFlow<AccountSettingsUiState> = _uiState.asStateFlow()

    fun setServerInput(url: String) {
        _uiState.update { it.copy(serverInput = url, saved = false) }
    }

    fun saveServerUrl() {
        val url = _uiState.value.serverInput.trim().trimEnd('/')
        if (url.isNotEmpty()) {
            prefs.serverUrl = url
            ApiClient.setBaseUrl(url)
            _uiState.update { it.copy(saved = true) }
        }
    }

    data class AccountSettingsUiState(
        val username:    String  = "",
        val displayName: String  = "",
        val serverInput: String  = "",
        val saved:       Boolean = false
    )
}
