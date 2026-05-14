package com.example.smartkrishi

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────
sealed class DiseaseUiState {
    object Idle                                         : DiseaseUiState()
    data class Loading(val message: String = "Analyzing image...")
                                                        : DiseaseUiState()
    data class Success(val result: DiseaseResponse)     : DiseaseUiState()
    data class Error(
        val message: String,
        val type: DiseaseErrorType
    )                                                   : DiseaseUiState()
}

enum class DiseaseErrorType {
    NO_IMAGE, NOT_RECOGNIZED, NO_INTERNET, TIMEOUT, SERVER, UNKNOWN
}

// ── ViewModel ─────────────────────────────────────────────────
class DiseaseViewModel : ViewModel() {

    private val repository = DiseaseRepository()

    private val _state = MutableStateFlow<DiseaseUiState>(DiseaseUiState.Idle)
    val state: StateFlow<DiseaseUiState> = _state

    private val _selectedImage = MutableStateFlow<Uri?>(null)
    val selectedImage: StateFlow<Uri?> = _selectedImage

    fun onImageSelected(uri: Uri?) {
        _selectedImage.value = uri
        // Clear previous result when new image is selected
        if (_state.value is DiseaseUiState.Success ||
            _state.value is DiseaseUiState.Error) {
            _state.value = DiseaseUiState.Idle
        }
    }

    fun detect(context: Context) {
        val uri = _selectedImage.value
        if (uri == null) {
            _state.value = DiseaseUiState.Error(
                "Please select or capture an image first.",
                DiseaseErrorType.NO_IMAGE
            )
            return
        }

        viewModelScope.launch {

            // Step 1 — preparing
            _state.value = DiseaseUiState.Loading("Preparing image...")
            delay(300)

            // Step 2 — uploading
            _state.value = DiseaseUiState.Loading("Uploading to AI server...")
            delay(200)

            // Step 3 — show "server waking up" if still loading after 8s
            val coldStartJob = launch {
                delay(8000)
                if (_state.value is DiseaseUiState.Loading) {
                    _state.value = DiseaseUiState.Loading(
                        "Server is waking up...\nThis may take 30–60 seconds on first use."
                    )
                }
            }

            val result = repository.detectDisease(context, uri)
            coldStartJob.cancel()

            _state.value = if (result.isSuccess) {
                DiseaseUiState.Success(result.getOrNull()!!)
            } else {
                when (val e = result.exceptionOrNull()) {

                    // ── Random / non-crop photo ───────────────
                    // Your Flask API returns this when confidence < 0.5
                    // This is the main case you were seeing errors for
                    is NotRecognizedException -> DiseaseUiState.Error(
                        "This doesn't look like a crop leaf image.\n\n" +
                        "Please upload a photo of:\n" +
                        "• Corn, Potato, or Tomato plant\n" +
                        "• A single leaf showing the affected area\n" +
                        "• Clear, well-lit image\n\n" +
                        "Random or unclear photos cannot be analyzed.",
                        DiseaseErrorType.NOT_RECOGNIZED
                    )

                    is NoInternetException -> DiseaseUiState.Error(
                        "No internet connection.\nPlease check your WiFi or mobile data.",
                        DiseaseErrorType.NO_INTERNET
                    )

                    is TimeoutException -> DiseaseUiState.Error(
                        "Server took too long to respond.\n" +
                        "The server may be starting up — please wait 30 seconds and try again.",
                        DiseaseErrorType.TIMEOUT
                    )

                    is ServerException -> DiseaseUiState.Error(
                        when (e.code) {
                            500  -> "Server error. The AI model had an issue processing your image. Try again."
                            502  -> "Server gateway error. Please try again in a moment."
                            503  -> "Server is starting up. Please try again in 30 seconds."
                            else -> "Server error (${e.code}). Please try again."
                        },
                        DiseaseErrorType.SERVER
                    )

                    else -> DiseaseUiState.Error(
                        "Something went wrong: ${e?.message ?: "Unknown error"}",
                        DiseaseErrorType.UNKNOWN
                    )
                }
            }
        }
    }

    fun reset() {
        _state.value = DiseaseUiState.Idle
        _selectedImage.value = null
    }
}
