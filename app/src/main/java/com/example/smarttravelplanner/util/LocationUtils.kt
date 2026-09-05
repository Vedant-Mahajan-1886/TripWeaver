package com.example.smarttravelplanner.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

/**
 * Accurately fetches the user's current city name using FusedLocationProviderClient and Geocoder.
 * Supports both fine and coarse (approximate) location permissions without crashing.
 */
suspend fun fetchCurrentCity(context: Context): String? =
    withContext(Dispatchers.IO) {
        val location = getDeviceLocation(context) ?: return@withContext null
        suspendCancellableCoroutine { cont ->
            try {
                val geocoder = Geocoder(context, Locale.getDefault())
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    geocoder.getFromLocation(location.latitude, location.longitude, 5) { addresses ->
                        val city = extractCityName(addresses)
                        if (cont.isActive) cont.resume(city)
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 5)
                    val city = extractCityName(addresses ?: emptyList())
                    if (cont.isActive) cont.resume(city)
                }
            } catch (e: Exception) {
                if (cont.isActive) cont.resume(null)
            }
        }
    }

suspend fun getDeviceLocation(context: Context): Location? =
    suspendCancellableCoroutine { cont ->
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        val cancelSource = CancellationTokenSource()
        cont.invokeOnCancellation { cancelSource.cancel() }

        try {
            // 1. Try fusedClient.lastLocation first (instant, especially for approximate location)
            fusedClient.lastLocation.addOnSuccessListener { lastLoc ->
                if (lastLoc != null) {
                    if (cont.isActive) cont.resume(lastLoc)
                } else {
                    // 2. Request current location with appropriate priority for coarse / fine
                    val hasFine = ContextCompat.checkSelfPermission(
                        context, Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
                    val priority = if (hasFine) {
                        Priority.PRIORITY_HIGH_ACCURACY
                    } else {
                        Priority.PRIORITY_BALANCED_POWER_ACCURACY
                    }
                    fusedClient.getCurrentLocation(priority, cancelSource.token)
                        .addOnSuccessListener { currentLoc ->
                            if (currentLoc != null) {
                                if (cont.isActive) cont.resume(currentLoc)
                            } else {
                                val fallback = getFallbackLocation(context)
                                if (cont.isActive) cont.resume(fallback)
                            }
                        }
                        .addOnFailureListener {
                            val fallback = getFallbackLocation(context)
                            if (cont.isActive) cont.resume(fallback)
                        }
                }
            }.addOnFailureListener {
                val fallback = getFallbackLocation(context)
                if (cont.isActive) cont.resume(fallback)
            }
        } catch (e: SecurityException) {
            val fallback = getFallbackLocation(context)
            if (cont.isActive) cont.resume(fallback)
        }
    }

fun getFallbackLocation(context: Context): Location? {
    return try {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(
            LocationManager.NETWORK_PROVIDER,
            LocationManager.PASSIVE_PROVIDER,
            LocationManager.GPS_PROVIDER
        )
        for (provider in providers) {
            try {
                if (locationManager.isProviderEnabled(provider)) {
                    val loc = locationManager.getLastKnownLocation(provider)
                    if (loc != null) return loc
                }
            } catch (_: SecurityException) {
            }
        }
        null
    } catch (e: Exception) {
        null
    }
}

fun extractCityName(addresses: List<Address>): String? {
    if (addresses.isEmpty()) return null

    // 1. Locality is the actual City across candidate addresses
    for (addr in addresses) {
        val locality = addr.locality
        if (!locality.isNullOrBlank() && !locality.equals("Unknown", ignoreCase = true)) {
            return locality.trim()
        }
    }

    // 2. Sub-Admin area (often metropolitan city / district)
    for (addr in addresses) {
        val subAdmin = addr.subAdminArea
        if (!subAdmin.isNullOrBlank()) {
            val cleaned = subAdmin
                .replace(" District", "", ignoreCase = true)
                .replace(" Urban", "", ignoreCase = true)
                .replace(" Rural", "", ignoreCase = true)
                .replace(" Division", "", ignoreCase = true)
                .trim()
            if (cleaned.isNotBlank()) return cleaned
        }
    }

    // 3. Sub-Locality (neighborhood / town)
    for (addr in addresses) {
        val subLocality = addr.subLocality
        if (!subLocality.isNullOrBlank()) {
            return subLocality.trim()
        }
    }

    return null
}
