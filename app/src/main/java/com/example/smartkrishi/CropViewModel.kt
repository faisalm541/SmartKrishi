package com.example.smartkrishi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException

// ── UI state sealed class ─────────────────────────────────────
sealed class CropPredictState {
    object Idle    : CropPredictState()
    object Loading : CropPredictState()
    data class Success(val recommendations: List<CropRecommendation>) : CropPredictState()
    data class Error(val message: String, val type: ErrorType) : CropPredictState()
}

enum class ErrorType { NO_INTERNET, TIMEOUT, SERVER, UNKNOWN }

// ── ViewModel ─────────────────────────────────────────────────
class CropViewModel : ViewModel() {

    private val _state = MutableStateFlow<CropPredictState>(CropPredictState.Idle)
    val state: StateFlow<CropPredictState> = _state

    fun predictCrops(
        n: Float, p: Float, k: Float,
        temperature: Float, humidity: Float,
        ph: Float, rainfall: Float
    ) {
        viewModelScope.launch {
            _state.value = CropPredictState.Loading

            try {
                val request = CropApiRequest(
                    N = n, P = p, K = k,
                    temperature = temperature,
                    humidity    = humidity,
                    ph          = ph,
                    rainfall    = rainfall
                )
                val response = CropApi.service.predictCrops(request)
                _state.value = CropPredictState.Success(
                    response.recommendations.take(3)
                )
            } catch (e: UnknownHostException) {
                _state.value = CropPredictState.Error(
                    "No internet connection. Please check your network.",
                    ErrorType.NO_INTERNET
                )
            } catch (e: SocketTimeoutException) {
                _state.value = CropPredictState.Error(
                    "Request timed out. The server may be starting up — try again.",
                    ErrorType.TIMEOUT
                )
            } catch (e: retrofit2.HttpException) {
                _state.value = CropPredictState.Error(
                    "Server error (${e.code()}). Please try again.",
                    ErrorType.SERVER
                )
            } catch (e: Exception) {
                _state.value = CropPredictState.Error(
                    "Something went wrong: ${e.message}",
                    ErrorType.UNKNOWN
                )
            }
        }
    }

    fun reset() {
        _state.value = CropPredictState.Idle
    }
}
