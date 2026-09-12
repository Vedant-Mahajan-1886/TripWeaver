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

    // Collect all sub-localities across all returned address candidates to filter them out
    val allSubLocalities = addresses.mapNotNull { it.subLocality?.trim()?.lowercase() }.toSet()

    // 1. Try to extract clean city from the formatted address line of candidates
    for (addr in addresses) {
        val cityFromLine = extractCityFromAddressLine(addr)
        if (!cityFromLine.isNullOrBlank() && !isLikelyNeighborhood(cityFromLine, allSubLocalities)) {
            return cleanCityName(cityFromLine)
        }
    }

    // 2. Locality (City) – strictly ensure it's NOT a sub-locality or neighborhood
    for (addr in addresses) {
        val locality = addr.locality?.trim()
        if (!locality.isNullOrBlank() && !locality.equals("Unknown", ignoreCase = true)) {
            if (!isLikelyNeighborhood(locality, allSubLocalities)) {
                return cleanCityName(locality)
            }
        }
    }

    // 3. Sub-Admin area (often metropolitan city / district like "Mumbai Suburban", "Bengaluru Urban")
    for (addr in addresses) {
        val subAdmin = addr.subAdminArea
        val cleaned = cleanSubAdminArea(subAdmin)
        if (!cleaned.isNullOrBlank() && !isLikelyNeighborhood(cleaned, allSubLocalities)) {
            return cleanCityName(cleaned)
        }
    }

    // 4. Admin area for City-States / Territories (e.g. Delhi, Chandigarh)
    for (addr in addresses) {
        val admin = addr.adminArea?.trim()
        if (!admin.isNullOrBlank()) {
            if (admin.equals("Delhi", ignoreCase = true) ||
                admin.contains("National Capital Territory of Delhi", ignoreCase = true)
            ) {
                return "Delhi"
            }
            if (admin.equals("Chandigarh", ignoreCase = true)) {
                return "Chandigarh"
            }
        }
    }

    // 5. Fallback: If locality exists even if flagged as neighborhood, prefer it over sub-locality
    for (addr in addresses) {
        val locality = addr.locality?.trim()
        if (!locality.isNullOrBlank() && !locality.equals("Unknown", ignoreCase = true)) {
            return cleanCityName(locality)
        }
    }

    // 6. Fallback: Cleaned sub-admin area
    for (addr in addresses) {
        val cleaned = cleanSubAdminArea(addr.subAdminArea)
        if (!cleaned.isNullOrBlank()) {
            return cleanCityName(cleaned)
        }
    }

    return null
}

fun extractCityFromAddressLine(address: Address): String? {
    val line = try { address.getAddressLine(0) } catch (_: Exception) { null } ?: return null
    return extractCityFromAddressComponents(
        line = line,
        country = address.countryName,
        adminArea = address.adminArea,
        postalCode = address.postalCode
    )
}

fun extractCityFromAddressComponents(
    line: String?,
    country: String?,
    adminArea: String?,
    postalCode: String?
): String? {
    if (line.isNullOrBlank()) return null
    val parts = line.split(",").map { it.trim() }.filter { it.isNotBlank() }
    if (parts.isEmpty()) return null

    val countryTrimmed = country?.trim()
    val adminAreaTrimmed = adminArea?.trim()
    val candidateParts = parts.toMutableList()

    // Remove country if present at the end
    if (candidateParts.isNotEmpty()) {
        val last = candidateParts.last()
        if (!countryTrimmed.isNullOrBlank() && last.equals(countryTrimmed, ignoreCase = true)) {
            candidateParts.removeAt(candidateParts.size - 1)
        } else if (last.equals("India", ignoreCase = true) || last.equals("United States", ignoreCase = true) || last.equals("USA", ignoreCase = true)) {
            candidateParts.removeAt(candidateParts.size - 1)
        }
    }

    // Inspect the last part: state / postal code
    if (candidateParts.isNotEmpty()) {
        val last = candidateParts.last()
        val withoutPostal = last.replace(Regex("\\b\\d{4,7}\\b"), "").trim()
        val isOnlyPostal = withoutPostal.isBlank()
        val isOnlyAdminArea = !adminAreaTrimmed.isNullOrBlank() && withoutPostal.equals(adminAreaTrimmed, ignoreCase = true)

        if (isOnlyPostal || isOnlyAdminArea) {
            candidateParts.removeAt(candidateParts.size - 1)
        } else if (withoutPostal.isNotBlank() && !adminAreaTrimmed.isNullOrBlank() && candidateParts.size > 1) {
            if (withoutPostal.contains(adminAreaTrimmed, ignoreCase = true)) {
                val stripped = withoutPostal.replace(adminAreaTrimmed, "", ignoreCase = true).trim()
                if (stripped.isBlank()) {
                    candidateParts.removeAt(candidateParts.size - 1)
                }
            }
        }
    }

    // The remaining trailing part is our city candidate
    if (candidateParts.isNotEmpty()) {
        var candidate = candidateParts.last()
            .replace(Regex("\\b\\d{4,7}\\b"), "")
            .trim()

        // If adminArea was attached to it (e.g. "Noida Uttar Pradesh"), strip adminArea as long as it's not a city like "New Delhi"
        if (!adminAreaTrimmed.isNullOrBlank() && 
            !candidate.equals("New Delhi", ignoreCase = true) && 
            candidate.contains(adminAreaTrimmed, ignoreCase = true)
        ) {
            val stripped = candidate.replace(Regex("(?i)\\b${Regex.escape(adminAreaTrimmed)}\\b"), "").trim()
            val directionalWords = setOf("north", "south", "east", "west", "central", "new", "old")
            if (stripped.isNotBlank() && stripped.length > 2 && !directionalWords.contains(stripped.lowercase())) {
                candidate = stripped
            }
        }

        if (candidate.isNotBlank() && !candidate.any { it.isDigit() }) {
            return candidate
        }
    }

    return null
}

fun cleanSubAdminArea(raw: String?): String? {
    if (raw.isNullOrBlank()) return null
    var name = raw.trim()

    // Well-known Indian Metropolitan mappings
    if (name.contains("Bengaluru", ignoreCase = true) || name.contains("Bangalore", ignoreCase = true)) return "Bengaluru"
    if (name.contains("Mumbai", ignoreCase = true)) return "Mumbai"
    if (name.contains("Delhi", ignoreCase = true)) {
        return if (name.contains("New Delhi", ignoreCase = true)) "New Delhi" else "Delhi"
    }
    if (name.contains("Kolkata", ignoreCase = true) || name.contains("Calcutta", ignoreCase = true)) return "Kolkata"
    if (name.contains("Chennai", ignoreCase = true) || name.contains("Madras", ignoreCase = true)) return "Chennai"
    if (name.contains("Hyderabad", ignoreCase = true)) return "Hyderabad"
    if (name.contains("Gautam Buddh", ignoreCase = true) || name.contains("Gautam Buddha", ignoreCase = true)) return "Noida"
    if (name.contains("Gurgaon", ignoreCase = true) || name.contains("Gurugram", ignoreCase = true)) return "Gurugram"

    // Suffix removals
    val suffixesToRemove = listOf(
        " District", " Division", " Urban", " Rural", " Suburban",
        " City", " Cantonment", " Region", " Corporation", " Municipality"
    )
    for (suffix in suffixesToRemove) {
        name = name.replace(Regex("(?i)${Regex.escape(suffix)}$"), "").trim()
    }

    // Directional prefixes (e.g. "South Delhi" -> "Delhi")
    name = name.replace(Regex("^(?i)(North|South|East|West|Central|North West|North East|South West|South East)\\s+"), "").trim()

    return if (name.isNotBlank() && !name.equals("Unknown", ignoreCase = true)) name else null
}

fun cleanCityName(city: String): String {
    return city
        .replace(Regex("(?i)\\s+(district|division|urban|rural|suburban|city|cantonment)$"), "")
        .trim()
}

fun isLikelyNeighborhood(name: String?, subLocalities: Set<String>): Boolean {
    if (name.isNullOrBlank()) return true
    val lower = name.trim().lowercase()
    if (subLocalities.contains(lower)) return true
    if (lower.startsWith("sector ") || lower.startsWith("block ") || lower.startsWith("phase ") || lower.startsWith("ward ") || lower.startsWith("pocket ")) {
        return true
    }
    if (lower.any { it.isDigit() }) {
        return true
    }

    // Identify neighborhood descriptors in compound names (e.g. "Indira Nagar", "Bandra West")
    val neighborhoodKeywords = listOf(
        "colony", "nagar", "enclave", "layout", "vihar", "extn", "extension",
        "mohalla", "basti", "road", "street", "lane", "marg", "cross", "stage",
        "west", "east", "north", "south", "bazaar", "bazar", "market", "puram"
    )
    if (name.trim().contains(" ")) {
        val words = lower.split("\\s+".toRegex())
        for (kw in neighborhoodKeywords) {
            if (words.contains(kw)) {
                return true
            }
        }
    }
    return false
}
