package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.SaleDTO
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SaleListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(SaleListUiState())
    val uiState: StateFlow<SaleListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val res = ApiClient.service.getSales(ApiClient.bearer(prefs.authToken))
                if (res.isSuccessful) _uiState.update { it.copy(items = res.body()?.data ?: emptyList()) }
            } catch (_: Exception) {}
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun setSearch(q: String) = _uiState.update { it.copy(search = q) }

    data class SaleListUiState(
        val items:   List<SaleDTO> = emptyList(),
        val loading: Boolean       = true,
        val search:  String        = ""
    )
}
