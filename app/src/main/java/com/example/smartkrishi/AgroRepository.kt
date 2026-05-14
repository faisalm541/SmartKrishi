package com.example.smartkrishi

import android.content.Context
import android.util.Log

const val AGRO_API_KEY = "eb27947a5896b4f73971832d7c5d0e95"

private const val PREF_NAME   = "agro_prefs"
private const val KEY_POLY_ID = "polygon_id"
private const val TAG         = "AgroMonitoring"

fun savePolyId(context: Context, polyId: String) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit().putString(KEY_POLY_ID, polyId).apply()
    Log.d(TAG, "Saved polyId: $polyId")
}

fun getSavedPolyId(context: Context): String? {
    val id = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .getString(KEY_POLY_ID, null)
    Log.d(TAG, "Loaded polyId from prefs: $id")
    return id
}

fun clearSavedPolyId(context: Context) {
    context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        .edit().remove(KEY_POLY_ID).apply()
    Log.d(TAG, "Cleared saved polyId")
}

fun buildPolygonRequest(lat: Double, lon: Double): PolygonRequest {
    val delta = 0.005
    val coordinates = listOf(listOf(
        listOf(lon - delta, lat - delta),
        listOf(lon + delta, lat - delta),
        listOf(lon + delta, lat + delta),
        listOf(lon - delta, lat + delta),
        listOf(lon - delta, lat - delta)
    ))
    return PolygonRequest(
        name = "smartkrishi_field_${System.currentTimeMillis()}",
        geo_json = GeoJson(geometry = Geometry(coordinates = coordinates))
    )
}

fun kelvinToCelsius(kelvin: Double): Int = (kelvin - 273.15).toInt()

suspend fun createAndSavePolygon(context: Context, lat: Double, lon: Double): Result<String> {
    return try {
        Log.d(TAG, "Creating polygon at lat=$lat lon=$lon")
        val request  = buildPolygonRequest(lat, lon)
        val response = AgroApi.service.createPolygon(AGRO_API_KEY, request)

        when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Polygon created! id=${body.id}")
                    savePolyId(context, body.id)
                    Result.success(body.id)
                } else {
                    Result.failure(Exception("Empty response body"))
                }
            }
            response.code() == 409 -> {
                // Polygon limit reached — fetch existing polygons and reuse the first one
                Log.w(TAG, "409 Conflict — polygon limit hit, fetching existing polygons")
                val existing = AgroApi.service.getPolygons(AGRO_API_KEY)
                if (existing.isSuccessful) {
                    val list = existing.body()
                    if (!list.isNullOrEmpty()) {
                        val existingId = list.first().id
                        Log.d(TAG, "Reusing existing polygon id=$existingId")
                        savePolyId(context, existingId)
                        Result.success(existingId)
                    } else {
                        Result.failure(Exception("No existing polygons found"))
                    }
                } else {
                    Result.failure(Exception("Failed to fetch existing polygons: ${existing.code()}"))
                }
            }
            else -> {
                val errBody = response.errorBody()?.string() ?: "unknown"
                Log.e(TAG, "Create polygon failed ${response.code()}: $errBody")
                Result.failure(Exception("HTTP ${response.code()}: $errBody"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create polygon: ${e.message}")
        Result.failure(e)
    }
}

suspend fun fetchSoilData(polyId: String): Result<AgroSoilData> {
    return try {
        Log.d(TAG, "Fetching soil data for polyId=$polyId")
        val response = AgroApi.service.getSoilData(polyId, AGRO_API_KEY)

        when {
            response.isSuccessful -> {
                val body = response.body()
                if (body != null) {
                    Log.d(TAG, "Soil data received: t0=${body.t0} t10=${body.t10} moisture=${body.moisture}")
                    val surfaceTemp = kelvinToCelsius(body.t0)
                    val depthTemp   = kelvinToCelsius(body.t10)
                    val moisture    = body.moisture
                    Result.success(
                        AgroSoilData(
                            surfaceTemp     = "${surfaceTemp}°C",
                            depthTemp       = "${depthTemp}°C",
                            moisture        = "%.2f".format(moisture),
                            moisturePercent = "${(moisture * 100).toInt()}%",
                            rawMoisture     = moisture
                        )
                    )
                } else {
                    Result.failure(Exception("Empty soil response"))
                }
            }
            response.code() == 404 -> {
                // polyId no longer exists on the server — clear it so next call recreates
                Log.w(TAG, "404 for polyId=$polyId — polygon deleted on server side")
                Result.failure(Exception("Polygon not found (404)"))
            }
            else -> {
                val errBody = response.errorBody()?.string() ?: "unknown"
                Log.e(TAG, "Soil fetch failed ${response.code()}: $errBody")
                Result.failure(Exception("HTTP ${response.code()}: $errBody"))
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Failed to fetch soil data: ${e.message}")
        Result.failure(e)
    }
}

suspend fun getOrFetchSoilData(context: Context, lat: Double, lon: Double): Result<AgroSoilData> {

    var polyId = getSavedPolyId(context)

    if (polyId == null) {
        Log.d(TAG, "No saved polyId — creating new polygon...")
        val createResult = createAndSavePolygon(context, lat, lon)
        if (createResult.isFailure) {
            Log.e(TAG, "Polygon creation failed: ${createResult.exceptionOrNull()?.message}")
            return Result.failure(createResult.exceptionOrNull() ?: Exception("Failed to create polygon"))
        }
        polyId = createResult.getOrNull()!!
    }

    // Try fetching soil with saved polyId
    val soilResult = fetchSoilData(polyId)
    if (soilResult.isFailure) {
        val msg = soilResult.exceptionOrNull()?.message ?: ""
        Log.w(TAG, "Soil fetch failed: $msg — clearing polyId and retrying once")
        clearSavedPolyId(context)

        // One retry: create a fresh polygon and try again
        val retryCreate = createAndSavePolygon(context, lat, lon)
        if (retryCreate.isFailure) {
            return Result.failure(retryCreate.exceptionOrNull() ?: Exception("Retry polygon creation failed"))
        }
        val newPolyId = retryCreate.getOrNull()!!
        return fetchSoilData(newPolyId)
    }

    return soilResult
}
