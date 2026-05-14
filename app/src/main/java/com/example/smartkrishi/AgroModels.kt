package com.example.smartkrishi

// ── Request body for creating a polygon ──────────────────────
data class PolygonRequest(
    val name: String,
    val geo_json: GeoJson
)

data class GeoJson(
    val type: String = "Feature",
    val properties: Map<String, String> = emptyMap(),
    val geometry: Geometry
)

data class Geometry(
    val type: String = "Polygon",
    val coordinates: List<List<List<Double>>>
)

// ── Response after creating / listing polygons ────────────────
data class PolygonResponse(
    val id: String,
    val name: String,
    val area: Double = 0.0      // default so Gson doesn't fail if field is absent
)

// ── Soil data response ────────────────────────────────────────
data class SoilResponse(
    val dt: Long = 0L,          // Unix timestamp
    val t0: Double,             // Surface temp in Kelvin
    val t10: Double,            // 10cm depth temp in Kelvin
    val moisture: Double        // Volumetric soil moisture m³/m³
)

// ── Clean UI-ready soil data ──────────────────────────────────
data class AgroSoilData(
    val surfaceTemp: String,        // e.g. "24°C"
    val depthTemp: String,          // e.g. "21°C"
    val moisture: String,           // e.g. "0.32"
    val moisturePercent: String,    // e.g. "32%"
    val rawMoisture: Double         // raw value for crop logic
)
