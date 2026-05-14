package com.example.smartkrishi

// ── Open-Meteo API response ───────────────────────────────────
data class OpenMeteoResponse(
    val latitude: Double,
    val longitude: Double,
    val timezone: String,
    val current: CurrentWeather,
    val daily: DailyWeather?
)

data class CurrentWeather(
    val temperature_2m: Double,
    val relative_humidity_2m: Int,
    val apparent_temperature: Double,
    val precipitation: Double,
    val rain: Double,
    val wind_speed_10m: Double,
    val weather_code: Int,
    val uv_index: Double,
    val soil_temperature_0cm: Double,
    val soil_moisture_0_to_1cm: Double
)

data class DailyWeather(
    val time: List<String>,
    val weather_code: List<Int>,
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val precipitation_sum: List<Double>,
    val uv_index_max: List<Double>
)

data class DayForecast(
    val dayName: String,
    val emoji: String,
    val maxTemp: Int,
    val minTemp: Int,
    val rainMm: Double,
    val condition: String
)

// ── Full UI state for HomeScreen ──────────────────────────────
data class WeatherUiState(
    val temperature: String          = "--",
    val feelsLike: String            = "--",
    val humidity: String             = "--",
    val wind: String                 = "--",
    val rain: String                 = "--",
    val uvIndex: String              = "--",
    val soilTemp: String             = "--",
    val soilMoisture: String         = "--",
    val condition: String            = "Loading...",
    val emoji: String                = "⛅",
    val locationName: String         = "Detecting location...",
    val advisory: String             = "",
    val forecast: List<DayForecast>  = emptyList(),
    val isLoading: Boolean           = true,
    val error: String?               = null,
    // Raw values for passing to CropInput and logic functions
    val rawTemp: Double              = 25.0,
    val rawHumidity: Int             = 50,
    val rawSoilMoisture: Double      = 0.3,
    val rawSoilTemp: Double          = 20.0,
    val rawRain: Double              = 0.0,
    val rawWeatherCode: Int          = 1,
    val rawUvIndex: Double           = 3.0
)

// ── Shared live data object ───────────────────────────────────
// This solves the duplicate getCurrentLocation() problem.
// HomeScreen fetches once and stores here.
// CropInputActivity reads from here to auto-fill fields.
object LiveFarmData {
    var temperature: Double    = 25.0
    var moisture: Double       = 0.3      // 0.0–1.0 scale
    var soilTemp: Double       = 20.0
    var rawRain: Double        = 0.0
    var humidity: Int          = 50
    var uvIndex: Double        = 3.0
    var weatherCode: Int       = 1
    var lat: Double            = 20.5937
    var lon: Double            = 78.9629

    // Fix #14: Timestamp-based staleness instead of a plain Boolean flag
    private var lastUpdated: Long = 0L

    // True only if data was fetched within the last 30 minutes
    val isPopulated: Boolean
        get() = lastUpdated > 0L &&
                (System.currentTimeMillis() - lastUpdated) < 30 * 60 * 1000L

    // Moisture as percentage string for display
    val moisturePercent: String
        get() = "${(moisture * 100).toInt()}%"

    // Moisture as 0–100 float for CropInput form
    val moistureAsInput: Float
        get() = (moisture * 100).toFloat().coerceIn(0f, 100f)

    fun updateFromWeather(c: CurrentWeather, lat: Double, lon: Double) {
        temperature  = c.temperature_2m
        moisture     = c.soil_moisture_0_to_1cm
        soilTemp     = c.soil_temperature_0cm
        rawRain      = c.rain
        humidity     = c.relative_humidity_2m
        uvIndex      = c.uv_index
        weatherCode  = c.weather_code
        this.lat     = lat
        this.lon     = lon
        lastUpdated  = System.currentTimeMillis()
    }

    fun updateFromAgro(agroMoisture: Double, agroSurfaceTemp: Double) {
        moisture  = agroMoisture
        soilTemp  = agroSurfaceTemp
    }
}
