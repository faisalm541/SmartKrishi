package com.example.smartkrishi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ── Response data classes ─────────────────────────────────────
data class MarketApiResponse(
    val records: List<MarketRecord>
)

data class MarketRecord(
    val commodity: String?,
    val market: String?,
    val state: String?,
    val modal_price: String?,
    val min_price: String?,
    val max_price: String?
)

// ── Retrofit interface ────────────────────────────────────────
interface MarketApiService {
    @GET("resource/9ef84268-d588-465a-a308-a864a43d0070")
    suspend fun getMarketPrices(
        @Query("api-key") apiKey: String  = MarketApi.MARKET_API_KEY,
        @Query("format")  format: String  = "json",
        @Query("limit")   limit: Int      = 50,
        @Query("filters[commodity]") commodity: String? = null
    ): MarketApiResponse
}

// ── Singleton client ──────────────────────────────────────────
object MarketApi {
    const val MARKET_API_KEY = "579b464db66ec23bdd000001f280b279f59e4b4278e50f6e2ce8fa44"

    val service: MarketApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.data.gov.in/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MarketApiService::class.java)
    }
}

// ── Crops we care about ───────────────────────────────────────
val TRACKED_CROPS = listOf(
    "Wheat", "Rice", "Cotton", "Maize",
    "Barley", "Soybean", "Groundnut", "Mustard",
    "Onion", "Tomato", "Sugarcane", "Jowar"
)

// ── Emoji map ─────────────────────────────────────────────────
fun getMarketEmoji(crop: String): String = when (crop.lowercase()) {
    "wheat"     -> "🌾"
    "rice"      -> "🌿"
    "cotton"    -> "🌸"
    "maize"     -> "🌽"
    "barley"    -> "🌾"
    "soybean"   -> "🫘"
    "groundnut" -> "🥜"
    "mustard"   -> "🌼"
    "onion"     -> "🧅"
    "tomato"    -> "🍅"
    "sugarcane" -> "🎋"
    "jowar"     -> "🌾"
    else        -> "🌱"
}

// ── Convert raw API records into MarketItem list ──────────────
fun parseMarketRecords(records: List<MarketRecord>): List<MarketItem> {
    // Group by commodity, average the modal price across markets
    val grouped = records
        .filter { r ->
            val name = r.commodity?.trim() ?: return@filter false
            TRACKED_CROPS.any { it.equals(name, ignoreCase = true) }
        }
        .groupBy { it.commodity?.trim()?.lowercase() ?: "" }

    return grouped.mapNotNull { (_, recs) ->
        val first     = recs.first()
        val name      = first.commodity?.trim()
            ?.replaceFirstChar { it.uppercase() } ?: return@mapNotNull null
        val prices    = recs.mapNotNull { it.modal_price?.toIntOrNull() }
        if (prices.isEmpty()) return@mapNotNull null
        val avgPrice  = prices.average().toInt()
        val maxPrice  = prices.max()
        val minPrice  = prices.min()

        MarketItem(
            emoji    = getMarketEmoji(name),
            name     = name,
            price    = "₹${"%,d".format(avgPrice)}/quintal",
            market   = first.market?.trim() ?: "Mandi",
            minPrice = minPrice,
            maxPrice = maxPrice,
            rawPrice = avgPrice,
            trend    = "→"   // trend computed after sorting
        )
    }.sortedByDescending { it.rawPrice }
}
