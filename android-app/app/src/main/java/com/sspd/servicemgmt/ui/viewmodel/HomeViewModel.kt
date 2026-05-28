package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.DashboardStats
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(HomeUiState(
        username    = prefs.username,
        displayName = prefs.displayName.ifEmpty { prefs.username }
    ))
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { loadStats() }

    fun loadStats() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val res = ApiClient.service.getStats(ApiClient.bearer(prefs.authToken))
                if (res.isSuccessful) {
                    _uiState.update { it.copy(stats = res.body()?.data ?: DashboardStats(), loading = false) }
                } else {
                    _uiState.update { it.copy(loading = false) }
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    fun logout() {
        prefs.clear()
        _uiState.update { it.copy(isLoggedOut = true) }
    }

    data class HomeUiState(
        val stats:       DashboardStats = DashboardStats(),
        val loading:     Boolean        = true,
        val username:    String         = "",
        val displayName: String         = "",
        val isLoggedOut: Boolean        = false
    )
}
