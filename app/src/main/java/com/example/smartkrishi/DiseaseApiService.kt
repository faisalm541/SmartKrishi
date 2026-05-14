package com.example.smartkrishi

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

// ── Response model ────────────────────────────────────────────
// Handles BOTH response shapes from your Flask API:
//
// Shape 1 — Success:
// { "crop": "Tomato", "disease": "Septoria", "solution": "...",
//   "confidence": 0.96, "label": "Tomato_Septoria" }
//
// Shape 2 — Not recognized / error:
// { "status": "not_recognized", "confidence": 0.3,
//   "message": "Could not identify disease..." }
data class DiseaseResponse(
    val crop: String?,           // null on not_recognized
    val disease: String?,        // null on not_recognized
    val solution: String?,       // null on not_recognized
    val confidence: Float?,      // present on both
    val label: String?,          // full label e.g. "Tomato_Septoria"
    val status: String?,         // "not_recognized" | "error" | null on success
    val message: String?         // error/not_recognized message from Flask
)

// ── Retrofit interface ────────────────────────────────────────
interface DiseaseApiService {
    @Multipart
    @POST("predict-disease")
    suspend fun detectDisease(
        @Part image: MultipartBody.Part
    ): DiseaseResponse
}

// ── Retrofit client ───────────────────────────────────────────
object DiseaseApi {
    private const val BASE_URL = "https://crop-disease-api-99qb.onrender.com/"

    val service: DiseaseApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            // 90s to handle Render cold start (30–50s) + model inference (10–20s)
            .connectTimeout(90, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DiseaseApiService::class.java)
    }
}
