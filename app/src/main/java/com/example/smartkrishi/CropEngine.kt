package com.example.smartkrishi

import java.util.Calendar

// ── Crop profile with ideal ranges ───────────────────────────
data class CropProfile(
    val name: String,
    val emoji: String,
    val season: String,
    val months: List<Int>,          // months when crop can be sown
    val phMin: Float, val phMax: Float,
    val moistureMin: Float, val moistureMax: Float,
    val tempMin: Float, val tempMax: Float,
    val nMin: Float, val nMax: Float,
    val pMin: Float, val pMax: Float,
    val kMin: Float, val kMax: Float,
    val tip: String
)

data class CropResult(
    val name: String,
    val emoji: String,
    val score: Float,
    val season: String,
    val tip: String
)

enum class AlertLevel { LOW, MEDIUM, HIGH }

data class RiskAlert(
    val icon: String,
    val title: String,
    val message: String,
    val action: String,
    val level: AlertLevel
)

// ── All supported crops with real agronomic ranges ───────────
val CROP_DATABASE = listOf(
    CropProfile(
        name = "Wheat", emoji = "🌾",
        season = "Rabi (Oct–Mar)", months = listOf(10, 11, 12),
        phMin = 6.0f, phMax = 7.5f,
        moistureMin = 40f, moistureMax = 70f,
        tempMin = 12f, tempMax = 25f,
        nMin = 80f, nMax = 120f,
        pMin = 40f, pMax = 60f,
        kMin = 40f, kMax = 60f,
        tip = "Ensure proper irrigation at crown root initiation and grain filling stage."
    ),
    CropProfile(
        name = "Rice", emoji = "🌿",
        season = "Kharif (Jun–Nov)", months = listOf(6, 7, 8),
        phMin = 5.0f, phMax = 6.5f,
        moistureMin = 70f, moistureMax = 100f,
        tempMin = 20f, tempMax = 35f,
        nMin = 80f, nMax = 120f,
        pMin = 30f, pMax = 50f,
        kMin = 40f, kMax = 60f,
        tip = "Maintain 5cm standing water during vegetative stage. Transplant at 25 days."
    ),
    CropProfile(
        name = "Cotton", emoji = "🌸",
        season = "Kharif (Apr–Nov)", months = listOf(4, 5, 6),
        phMin = 6.5f, phMax = 8.0f,
        moistureMin = 30f, moistureMax = 60f,
        tempMin = 21f, tempMax = 37f,
        nMin = 60f, nMax = 100f,
        pMin = 30f, pMax = 50f,
        kMin = 30f, kMax = 50f,
        tip = "Cotton needs full sunlight and well-drained soil. Space rows 60–90cm apart."
    ),
    CropProfile(
        name = "Maize", emoji = "🌽",
        season = "Kharif (Jun–Sep)", months = listOf(6, 7),
        phMin = 5.8f, phMax = 7.0f,
        moistureMin = 40f, moistureMax = 75f,
        tempMin = 18f, tempMax = 32f,
        nMin = 100f, nMax = 150f,
        pMin = 50f, pMax = 75f,
        kMin = 40f, kMax = 60f,
        tip = "Apply nitrogen in 3 splits — sowing, knee high, and tasseling stage."
    ),
    CropProfile(
        name = "Soybean", emoji = "🫘",
        season = "Kharif (Jun–Oct)", months = listOf(6, 7),
        phMin = 6.0f, phMax = 7.0f,
        moistureMin = 40f, moistureMax = 70f,
        tempMin = 20f, tempMax = 30f,
        nMin = 20f, nMax = 40f,   // low N — fixes own nitrogen
        pMin = 40f, pMax = 60f,
        kMin = 40f, kMax = 60f,
        tip = "Inoculate seeds with Rhizobium. Soybean fixes nitrogen — reduces fertilizer cost."
    ),
    CropProfile(
        name = "Groundnut", emoji = "🥜",
        season = "Kharif (Jun–Oct)", months = listOf(6, 7),
        phMin = 6.0f, phMax = 7.5f,
        moistureMin = 30f, moistureMax = 60f,
        tempMin = 25f, tempMax = 35f,
        nMin = 20f, nMax = 40f,
        pMin = 40f, pMax = 60f,
        kMin = 30f, kMax = 50f,
        tip = "Apply gypsum at pegging stage for better pod development and calcium supply."
    ),
    CropProfile(
        name = "Mustard", emoji = "🌼",
        season = "Rabi (Oct–Feb)", months = listOf(10, 11),
        phMin = 6.0f, phMax = 7.5f,
        moistureMin = 30f, moistureMax = 60f,
        tempMin = 10f, tempMax = 25f,
        nMin = 60f, nMax = 80f,
        pMin = 30f, pMax = 40f,
        kMin = 30f, kMax = 40f,
        tip = "Thin crop to 10–15cm spacing. One irrigation at flowering is critical."
    ),
    CropProfile(
        name = "Sugarcane", emoji = "🎋",
        season = "Annual (Feb–Mar)", months = listOf(2, 3),
        phMin = 6.0f, phMax = 7.5f,
        moistureMin = 60f, moistureMax = 90f,
        tempMin = 20f, tempMax = 35f,
        nMin = 150f, nMax = 250f,
        pMin = 60f, pMax = 80f,
        kMin = 80f, kMax = 120f,
        tip = "Requires 10–12 irrigations. Earth up at 90 and 120 days to prevent lodging."
    ),
    CropProfile(
        name = "Chickpea", emoji = "🫛",
        season = "Rabi (Oct–Feb)", months = listOf(10, 11),
        phMin = 6.0f, phMax = 8.0f,
        moistureMin = 25f, moistureMax = 55f,
        tempMin = 10f, tempMax = 25f,
        nMin = 20f, nMax = 40f,
        pMin = 40f, pMax = 60f,
        kMin = 20f, kMax = 40f,
        tip = "Drought tolerant — avoid excess irrigation. One pre-sowing irrigation is enough."
    ),
    CropProfile(
        name = "Barley", emoji = "🌾",
        season = "Rabi (Oct–Mar)", months = listOf(10, 11, 12),
        phMin = 6.0f, phMax = 8.0f,
        moistureMin = 30f, moistureMax = 65f,
        tempMin = 8f, tempMax = 22f,
        nMin = 60f, nMax = 90f,
        pMin = 30f, pMax = 50f,
        kMin = 30f, kMax = 50f,
        tip = "Hardy crop for marginal soils. Salt and drought tolerant. Good for dry areas."
    ),
    CropProfile(
        name = "Tomato", emoji = "🍅",
        season = "Rabi (Oct–Jan)", months = listOf(10, 11),
        phMin = 6.0f, phMax = 7.0f,
        moistureMin = 50f, moistureMax = 80f,
        tempMin = 18f, tempMax = 30f,
        nMin = 100f, nMax = 150f,
        pMin = 60f, pMax = 80f,
        kMin = 80f, kMax = 100f,
        tip = "Stake plants at 30cm height. Apply potassium at fruiting for better quality."
    ),
    CropProfile(
        name = "Potato", emoji = "🥔",
        season = "Rabi (Oct–Jan)", months = listOf(10, 11),
        phMin = 5.5f, phMax = 6.5f,
        moistureMin = 60f, moistureMax = 85f,
        tempMin = 10f, tempMax = 22f,
        nMin = 120f, nMax = 160f,
        pMin = 60f, pMax = 80f,
        kMin = 100f, kMax = 150f,
        tip = "Earth up at 30 days to prevent greening. High potassium improves tuber quality."
    )
)

// ── Score a crop against user inputs ─────────────────────────
fun scoreCrop(
    crop: CropProfile,
    ph: Float,
    moisture: Float,
    temp: Float,
    nitrogen: Float,
    phosphorus: Float,
    potassium: Float,
    currentMonth: Int
): Float {

    var score = 0f
    var maxScore = 0f

    // pH score — 25 points
    maxScore += 25f
    score += when {
        ph in crop.phMin..crop.phMax -> 25f
        ph < crop.phMin -> maxOf(0f, 25f - (crop.phMin - ph) * 15f)
        else            -> maxOf(0f, 25f - (ph - crop.phMax) * 15f)
    }

    // Temperature score — 15 points
    maxScore += 15f
    score += when {
        temp in crop.tempMin..crop.tempMax -> 15f
        temp < crop.tempMin -> maxOf(0f, 15f - (crop.tempMin - temp) * 2f)
        else                -> maxOf(0f, 15f - (temp - crop.tempMax) * 2f)
    }

    // Moisture score — 15 points
    maxScore += 15f
    score += when {
        moisture in crop.moistureMin..crop.moistureMax -> 15f
        moisture < crop.moistureMin -> maxOf(0f, 15f - (crop.moistureMin - moisture) * 0.5f)
        else                        -> maxOf(0f, 15f - (moisture - crop.moistureMax) * 0.5f)
    }

    // Nitrogen score — 10 points
    if (nitrogen > 0f) {
        maxScore += 10f
        score += when {
            nitrogen in crop.nMin..crop.nMax -> 10f
            nitrogen < crop.nMin -> maxOf(0f, 10f - (crop.nMin - nitrogen) * 0.15f)
            else                 -> maxOf(0f, 10f - (nitrogen - crop.nMax) * 0.15f)
        }
    }

    // Phosphorus score — 8 points
    if (phosphorus > 0f) {
        maxScore += 8f
        score += when {
            phosphorus in crop.pMin..crop.pMax -> 8f
            phosphorus < crop.pMin -> maxOf(0f, 8f - (crop.pMin - phosphorus) * 0.2f)
            else                   -> maxOf(0f, 8f - (phosphorus - crop.pMax) * 0.2f)
        }
    }

    // Potassium score — 7 points
    if (potassium > 0f) {
        maxScore += 7f
        score += when {
            potassium in crop.kMin..crop.kMax -> 7f
            potassium < crop.kMin -> maxOf(0f, 7f - (crop.kMin - potassium) * 0.2f)
            else                  -> maxOf(0f, 7f - (potassium - crop.kMax) * 0.2f)
        }
    }

    // Season bonus — 5 points
    maxScore += 5f
    if (currentMonth in crop.months) score += 5f
    else if (crop.months.any { kotlin.math.abs(it - currentMonth) <= 1 }) score += 2f

    return if (maxScore > 0f) (score / maxScore).coerceIn(0f, 1f) else 0f
}

// ── Main recommendation function ─────────────────────────────
fun recommendCropsAdvanced(
    ph: Float,
    moisture: Float,
    temp: Float,
    nitrogen: Float = 0f,
    phosphorus: Float = 0f,
    potassium: Float = 0f,
    currentMonth: Int = Calendar.getInstance().get(Calendar.MONTH) + 1
): List<CropResult> {

    return CROP_DATABASE
        .map { crop ->
            val score = scoreCrop(crop, ph, moisture, temp,
                nitrogen, phosphorus, potassium, currentMonth)
            CropResult(
                name   = crop.name,
                emoji  = crop.emoji,
                score  = score,
                season = crop.season,
                tip    = crop.tip
            )
        }
        .filter { it.score > 0.3f }           // only show meaningful matches
        .sortedByDescending { it.score }
        .take(4)
        .ifEmpty {
            // absolute fallback — return best scoring even if below 0.3
            CROP_DATABASE.map { crop ->
                val score = scoreCrop(crop, ph, moisture, temp,
                    nitrogen, phosphorus, potassium, currentMonth)
                CropResult(crop.name, crop.emoji, score, crop.season, crop.tip)
            }.sortedByDescending { it.score }.take(2)
        }
}

// ── Best Crop Today — uses weather + soil data directly ───────
fun getBestCropToday(
    temp: Double,
    moisture: Double,       // AgroMonitoring or Open-Meteo moisture
    soilTemp: Double,
    rain: Double,
    weatherCode: Int
): Triple<String, String, String> {  // emoji, name, reason

    val month = Calendar.getInstance().get(Calendar.MONTH) + 1

    // Pick best scoring crop using current live conditions
    val results = recommendCropsAdvanced(
        ph         = 6.5f,              // neutral default
        moisture   = (moisture * 100).toFloat().coerceIn(0f, 100f),
        temp       = temp.toFloat(),
        currentMonth = month
    )

    val best = results.firstOrNull()
        ?: return Triple("🌾", "Wheat", "Safe choice for current conditions")

    val reason = when {
        weatherCode in 61..82 ->
            "Rain forecast — ideal for ${best.name} sowing"
        temp > 30 ->
            "High temp suits ${best.name} growth"
        moisture > 0.35 ->
            "Good soil moisture for ${best.name}"
        month in listOf(10, 11, 12) ->
            "Rabi season — ${best.name} timing is perfect"
        month in listOf(6, 7, 8) ->
            "Kharif season — ideal for ${best.name}"
        else ->
            "${best.name} matches your current soil & climate"
    }

    return Triple(best.emoji, best.name, reason)
}

// ── Risk Alerts ──────────────────────────────
fun getRiskAlerts(
    weatherCode: Int,
    temp: Double,
    moisture: Double,
    rain: Double,
    uv: Double,
    soilMoisture: Double
): List<RiskAlert> {
    val alerts = mutableListOf<RiskAlert>()

    // Heat/Cold
    if (temp > 38) alerts.add(RiskAlert("🔥", "Heat Wave", "Temp is above 38°C.", "Increase irrigation frequency", AlertLevel.HIGH))
    else if (temp < 5) alerts.add(RiskAlert("❄️", "Frost Risk", "Very low temp detected.", "Protect young seedlings", AlertLevel.HIGH))

    // Rain/Storm
    if (weatherCode in listOf(65, 67, 75, 77, 82)) alerts.add(RiskAlert("⛈️", "Heavy Rain", "Intense precipitation likely.", "Check field drainage", AlertLevel.HIGH))
    else if (rain > 10) alerts.add(RiskAlert("🌧️", "Rainy Day", "Significant rain today.", "Postpone fertilizer application", AlertLevel.MEDIUM))

    // Drought/Moisture
    if (soilMoisture < 0.15) alerts.add(RiskAlert("🌵", "Dry Soil", "Soil moisture is very low.", "Immediate watering needed", AlertLevel.HIGH))
    else if (soilMoisture < 0.25) alerts.add(RiskAlert("💧", "Moisture Low", "Soil is getting dry.", "Plan for irrigation", AlertLevel.MEDIUM))

    // UV
    if (uv > 8) alerts.add(RiskAlert("☀️", "High UV", "Intense solar radiation.", "Avoid midday field work", AlertLevel.MEDIUM))

    // Pest (Estimated)
    if (temp in 25.0..32.0 && soilMoisture > 0.4) {
        alerts.add(RiskAlert("🐛", "Pest Risk", "Warm & humid conditions.", "Inspect crops for insects", AlertLevel.MEDIUM))
    }

    return alerts
}

fun getAITips(
    weatherCode: Int,
    temp: Double,
    humidity: Int,
    moisture: Double,
    rain: Double,
    uv: Double,
    soilTemp: Double
): List<String> {
    val tips = mutableListOf<String>()
    if (rain > 0) tips.add("Natural rainwater is rich in nitrogen — great for growth!")
    if (uv > 6) tips.add("High UV detected. Mulching can help retain soil moisture.")
    if (humidity > 80) tips.add("High humidity increases fungal risks. Ensure good airflow.")
    if (soilTemp > 25) tips.add("Warm soil speeds up germination but also weed growth.")
    
    if (tips.isEmpty()) {
        tips.add("Keep your field tools clean to prevent disease spread.")
        tips.add("Regularly check soil pH for optimal nutrient uptake.")
    }
    return tips
}
