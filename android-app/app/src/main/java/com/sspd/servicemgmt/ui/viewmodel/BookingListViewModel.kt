package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.BookingDTO
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BookingListViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)

    private val _uiState = MutableStateFlow(BookingListUiState())
    val uiState: StateFlow<BookingListUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val res = ApiClient.service.getBookings(ApiClient.bearer(prefs.authToken))
                if (res.isSuccessful) _uiState.update { it.copy(items = res.body()?.data ?: emptyList()) }
            } catch (_: Exception) {}
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun setSearch(q: String) = _uiState.update { it.copy(search = q) }

    data class BookingListUiState(
        val items:   List<BookingDTO> = emptyList(),
        val loading: Boolean          = true,
        val search:  String           = ""
    )
}
