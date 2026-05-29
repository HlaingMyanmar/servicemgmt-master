package com.sspd.servicemgmt.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.sspd.servicemgmt.api.ApiClient
import com.sspd.servicemgmt.api.ProductDTO
import com.sspd.servicemgmt.api.ProductSerialDTO
import com.sspd.servicemgmt.utils.PreferenceManager
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val prefs = PreferenceManager(application)
    private val productId: Int = checkNotNull(savedStateHandle["productId"])

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState: StateFlow<ProductDetailUiState> = _uiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = true) }
            try {
                val token = ApiClient.bearer(prefs.authToken)
                val productDeferred = async { ApiClient.service.getProduct(token, productId) }
                val serialsDeferred = async { ApiClient.service.getProductSerials(token, productId) }

                val productRes = productDeferred.await()
                val serialsRes = serialsDeferred.await()

                _uiState.update {
                    it.copy(
                        product = if (productRes.isSuccessful) productRes.body()?.data else null,
                        serials = if (serialsRes.isSuccessful) serialsRes.body()?.data ?: emptyList() else emptyList(),
                        loading = false
                    )
                }
            } catch (_: Exception) {
                _uiState.update { it.copy(loading = false) }
            }
        }
    }

    data class ProductDetailUiState(
        val product: ProductDTO? = null,
        val serials: List<ProductSerialDTO> = emptyList(),
        val loading: Boolean = true
    )
}
