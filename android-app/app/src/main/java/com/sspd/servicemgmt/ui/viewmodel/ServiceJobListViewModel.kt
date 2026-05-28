package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.ServiceJobDTO
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ServiceJobListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(ServiceJobListUiState())
    val uiState: StateFlow<ServiceJobListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val res = ApiClient.service.getServiceJobs(ApiClient.bearer(prefs.authToken))
                if (res.isSuccessful) _uiState.update { it.copy(items = res.body()?.data ?: emptyList()) }
            } catch (_: Exception) {}
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun setFilter(f: String) = _uiState.update { it.copy(filter = f) }

    data class ServiceJobListUiState(
        val items:   List<ServiceJobDTO> = emptyList(),
        val loading: Boolean             = true,
        val filter:  String              = "ALL"
    )
}
