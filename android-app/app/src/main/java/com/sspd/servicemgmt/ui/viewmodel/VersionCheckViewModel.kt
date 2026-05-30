package com.sspd.servicemgmt.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.BuildConfig
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.AppVersionDTO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class VersionCheckViewModel : ViewModel() {

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun check() {
        if (_state.value.checked) return          // once per session
        viewModelScope.launch {
            try {
                val res = ApiClient.service.getAppVersion()
                val dto = res.body()?.data ?: return@launch
                if (dto.versionCode > BuildConfig.VERSION_CODE) {
                    _state.update { it.copy(update = dto, checked = true) }
                } else {
                    _state.update { it.copy(checked = true) }
                }
            } catch (_: Exception) {
                _state.update { it.copy(checked = true) }
            }
        }
    }

    fun dismiss() {
        // only allowed when forceUpdate = false
        if (_state.value.update?.forceUpdate == true) return
        _state.update { it.copy(update = null) }
    }

    data class State(
        val update:  AppVersionDTO? = null,
        val checked: Boolean        = false
    )
}
