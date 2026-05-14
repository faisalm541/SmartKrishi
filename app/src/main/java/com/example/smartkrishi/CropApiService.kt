package com.example.smartkrishi

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

// ── Request model ─────────────────────────────────────────────
data class CropApiRequest(
    val N: Float,
    val P: Float,
    val K: Float,
    val temperature: Float,
    val humidity: Float,
    val ph: Float,
    val rainfall: Float
)

// ── Response models ───────────────────────────────────────────
data class CropApiResponse(
    val recommendations: List<CropRecommendation>
)

data class CropRecommendation(
    val crop: String,
    val confidence: Float
)

// ── Retrofit interface ────────────────────────────────────────
interface CropApiService {
    @POST("predict")
    suspend fun predictCrops(
        @Body request: CropApiRequest
    ): CropApiResponse
}

// ── Retrofit client ───────────────────────────────────────────
object CropApi {
    private const val BASE_URL = "https://crop-api-a1cf.onrender.com/"

    val service: CropApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CropApiService::class.java)
    }
}

// ── Suitability label from confidence ────────────────────────
fun getSuitabilityLabel(confidence: Float): Pair<String, Long> {
    return when {
        confidence >= 50f -> "High"   to 0xFF2E7D32
        confidence >= 20f -> "Medium" to 0xFFF57F17
        else              -> "Low"    to 0xFFC62828
    }
}

// ── Crop emoji map ────────────────────────────────────────────
fun getCropEmoji(crop: String): String {
    return when (crop.lowercase().trim()) {
        "rice"        -> "🌿"
        "maize"       -> "🌽"
        "chickpea"    -> "🫛"
        "kidneybeans",
        "kidney beans"-> "🫘"
        "pigeonpeas",
        "pigeon peas" -> "🫘"
        "mothbeans",
        "moth beans"  -> "🫘"
        "mungbean",
        "mung bean"   -> "🌱"
        "blackgram",
        "black gram"  -> "⚫"
        "lentil"      -> "🫘"
        "pomegranate" -> "🍎"
        "banana"      -> "🍌"
        "mango"       -> "🥭"
        "grapes"      -> "🍇"
        "watermelon"  -> "🍉"
        "muskmelon"   -> "🍈"
        "apple"       -> "🍎"
        "orange"      -> "🍊"
        "papaya"      -> "🍈"
        "coconut"     -> "🥥"
        "cotton"      -> "🌸"
        "jute"        -> "🎋"
        "coffee"      -> "☕"
        "wheat"       -> "🌾"
        "sugarcane"   -> "🎋"
        "tobacco"     -> "🌿"
        else          -> "🌱"
    }
}

// ── Crop season info ──────────────────────────────────────────
fun getCropSeason(crop: String): String {
    return when (crop.lowercase().trim()) {
        "rice", "maize", "cotton", "jute",
        "mungbean", "mung bean", "blackgram",
        "black gram", "pigeonpeas", "pigeon peas" -> "Kharif (Jun–Nov)"
        "wheat", "chickpea", "lentil",
        "mothbeans", "moth beans"                  -> "Rabi (Oct–Mar)"
        "banana", "papaya", "mango",
        "coconut", "coffee", "sugarcane"           -> "Annual / Perennial"
        "grapes", "pomegranate", "apple",
        "orange", "watermelon", "muskmelon"        -> "Zaid / Seasonal"
        else                                        -> "Seasonal"
    }
}
