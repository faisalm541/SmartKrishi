package com.example.smartkrishi

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Calendar

interface OpenMeteoService {

    @GET("v1/forecast")
    suspend fun getWeather(
        @Query("latitude")      lat: Double,
        @Query("longitude")     lon: Double,
        @Query("current")       current: String =
            "temperature_2m,relative_humidity_2m,apparent_temperature," +
            "precipitation,rain,wind_speed_10m,weather_code,uv_index," +
            "soil_temperature_0cm,soil_moisture_0_to_1cm",
        @Query("daily")         daily: String =
            "weather_code,temperature_2m_max,temperature_2m_min,precipitation_sum,uv_index_max",
        @Query("timezone")      timezone: String = "auto",
        @Query("forecast_days") days: Int = 7
    ): OpenMeteoResponse
}

interface GeocodingService {
    @GET("reverse")
    suspend fun getCityName(
        @Query("lat")    lat: Double,
        @Query("lon")    lon: Double,
        @Query("format") format: String = "json",
        @Query("zoom")   zoom: Int = 10
    ): GeocodingResponse
}

data class GeocodingResponse(val address: Address?)

data class Address(
    val city: String?,
    val town: String?,
    val village: String?,
    val county: String?,
    val state: String?,
    val country: String?
)

object WeatherApi {
    val service: OpenMeteoService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenMeteoService::class.java)
    }

    val geocoding: GeocodingService by lazy {
        Retrofit.Builder()
            .baseUrl("https://nominatim.openstreetmap.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(GeocodingService::class.java)
    }
}

fun getWeatherEmoji(code: Int): String = when (code) {
    0            -> "☀️"
    1, 2         -> "🌤️"
    3            -> "⛅"
    45, 48       -> "🌫️"
    51, 53, 55   -> "🌦️"
    61, 63, 65   -> "🌧️"
    71, 73, 75   -> "❄️"
    80, 81, 82   -> "🌦️"
    95           -> "⛈️"
    96, 99       -> "⛈️"
    else         -> "🌤️"
}

fun getWeatherCondition(code: Int): String = when (code) {
    0            -> "Clear Sky"
    1            -> "Mainly Clear"
    2            -> "Partly Cloudy"
    3            -> "Overcast"
    45, 48       -> "Foggy"
    51, 53, 55   -> "Drizzle"
    61, 63, 65   -> "Rainy"
    71, 73, 75   -> "Snowy"
    80, 81, 82   -> "Rain Showers"
    95           -> "Thunderstorm"
    96, 99       -> "Heavy Thunderstorm"
    else         -> "Partly Cloudy"
}

fun getFarmingAdvisory(
    code: Int, temp: Double,
    soilMoisture: Double, rain: Double
): String = when {
    code in 95..99 ->
        "⚠️ Storm alert! Stay indoors and secure all equipment."
    code in 61..82 || rain > 5 ->
        "🌧️ Rainy conditions. Good time to sow rice. Avoid pesticide spraying."
    code in 51..55 ->
        "🌦️ Light drizzle. Hold off on fertilizer application today."
    soilMoisture > 0.4 ->
        "💧 Soil moisture is high. No irrigation needed today."
    soilMoisture < 0.15 ->
        "🚿 Soil is very dry. Irrigate your fields today."
    temp > 38 ->
        "🌡️ Extreme heat! Irrigate only in early morning or after sunset."
    temp < 8 ->
        "🥶 Cold wave alert. Protect sensitive crops from frost damage."
    code in 0..2 && temp in 18.0..32.0 ->
        "✅ Perfect farming conditions. Great day for field work."
    else ->
        "🌾 Moderate conditions. Good for most farm activities."
}

// ── Parse 7-day forecast from Open-Meteo daily response ──────
fun parseForecast(daily: DailyWeather?): List<DayForecast> {
    if (daily == null) return emptyList()

    val dayNames = listOf("Today", "Tomorrow", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cal = Calendar.getInstance()
    val today = cal.get(Calendar.DAY_OF_WEEK)
    val fullDayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")

    return daily.weather_code.mapIndexed { i, code ->
        val label = when (i) {
            0 -> "Today"
            1 -> "Tomorrow"
            else -> {
                cal.add(Calendar.DAY_OF_WEEK, 1)
                fullDayNames[cal.get(Calendar.DAY_OF_WEEK) - 1]
            }
        }
        DayForecast(
            dayName   = label,
            emoji     = getWeatherEmoji(code),
            maxTemp   = daily.temperature_2m_max.getOrElse(i) { 0.0 }.toInt(),
            minTemp   = daily.temperature_2m_min.getOrElse(i) { 0.0 }.toInt(),
            rainMm    = daily.precipitation_sum.getOrElse(i) { 0.0 },
            condition = getWeatherCondition(code)
        )
    }
}
