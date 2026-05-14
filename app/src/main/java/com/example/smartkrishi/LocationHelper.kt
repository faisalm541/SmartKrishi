package com.example.smartkrishi

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class LatLon(val lat: Double, val lon: Double)

// Fix #11: Internet connectivity check before API calls
fun isInternetAvailable(context: Context): Boolean {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val network = cm.activeNetwork ?: return false
    val caps = cm.getNetworkCapabilities(network) ?: return false
    return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
}

fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
}

suspend fun getCurrentLocation(context: Context): LatLon {
    if (!hasLocationPermission(context)) {
        throw SecurityException("Location permission not granted")
    }

    val client = LocationServices.getFusedLocationProviderClient(context)

    return suspendCancellableCoroutine { cont ->
        val tokenSource = CancellationTokenSource()

        try {
            client.getCurrentLocation(
                Priority.PRIORITY_BALANCED_POWER_ACCURACY,
                tokenSource.token
            ).addOnSuccessListener { location ->
                if (location != null) {
                    cont.resume(LatLon(location.latitude, location.longitude))
                } else {
                    // Try last known location as fallback
                    client.lastLocation.addOnSuccessListener { last ->
                        if (last != null) {
                            cont.resume(LatLon(last.latitude, last.longitude))
                        } else {
                            // India center as final fallback
                            cont.resume(LatLon(20.5937, 78.9629))
                        }
                    }.addOnFailureListener {
                        cont.resume(LatLon(20.5937, 78.9629))
                    }
                }
            }.addOnFailureListener { e ->
                cont.resumeWithException(e)
            }
        } catch (e: SecurityException) {
            cont.resumeWithException(e)
        }

        cont.invokeOnCancellation { tokenSource.cancel() }
    }
}