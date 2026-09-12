package com.example.smarttravelplanner.ui.screens.tripdetail

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.smarttravelplanner.domain.model.Activity
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds

object MapUtils {

    /**
     * Checks if coordinates are valid non-zero latitude/longitude.
     */
    fun isValidCoord(lat: Double?, lng: Double?): Boolean {
        if (lat == null || lng == null) return false
        if (lat == 0.0 && lng == 0.0) return false
        return lat in -90.0..90.0 && lng in -180.0..180.0
    }

    /**
     * Extracts valid LatLng points from a list of activities.
     */
    fun getValidActivityPoints(activities: List<Activity>): List<Pair<Activity, LatLng>> {
        return activities.mapNotNull { act ->
            val lat = act.lat
            val lng = act.lng
            if (isValidCoord(lat, lng)) {
                act to LatLng(lat!!, lng!!)
            } else {
                null
            }
        }
    }

    /**
     * Computes a LatLngBounds enclosing all provided coordinates, with padding.
     */
    fun computeBounds(points: List<LatLng>): LatLngBounds? {
        if (points.isEmpty()) return null
        val builder = LatLngBounds.builder()
        points.forEach { builder.include(it) }
        return builder.build()
    }

    /**
     * Launches the Google Maps app for turn-by-turn navigation or place details.
     */
    fun launchGoogleMapsDirections(context: Context, lat: Double, lng: Double, label: String = "") {
        try {
            val uriStr = if (label.isNotBlank()) {
                "geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})"
            } else {
                "google.navigation:q=$lat,$lng"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriStr)).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                // Fallback to browser or generic geo intent
                val webIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$lat,$lng")
                )
                context.startActivity(webIntent)
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to open Google Maps", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Launches a multi-stop or destination route in Google Maps for the entire day.
     */
    fun launchDayRoute(context: Context, activities: List<Activity>) {
        val valid = getValidActivityPoints(activities)
        if (valid.isEmpty()) {
            Toast.makeText(context, "No coordinates available for this day", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val destination = valid.last().second
            val waypoints = valid.dropLast(1).joinToString("|") { "${it.second.latitude},${it.second.longitude}" }

            val url = buildString {
                append("https://www.google.com/maps/dir/?api=1")
                append("&destination=${destination.latitude},${destination.longitude}")
                if (waypoints.isNotBlank()) {
                    append("&waypoints=${Uri.encode(waypoints)}")
                }
                append("&travelmode=driving")
            }

            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                setPackage("com.google.android.apps.maps")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            } else {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Unable to launch day route", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Decodes a Google Maps / OSRM encoded polyline string into a list of LatLng coordinates.
     */
    fun decodePolyline(encoded: String): List<LatLng> {
        val poly = ArrayList<LatLng>()
        var index = 0
        val len = encoded.length
        var lat = 0
        var lng = 0

        while (index < len) {
            var b: Int
            var shift = 0
            var result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlat = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lat += dlat

            shift = 0
            result = 0
            do {
                if (index >= len) break
                b = encoded[index++].code - 63
                result = result or (b and 0x1f shl shift)
                shift += 5
            } while (b >= 0x20)
            val dlng = if (result and 1 != 0) (result shr 1).inv() else result shr 1
            lng += dlng

            poly.add(LatLng(lat.toDouble() / 1E5, lng.toDouble() / 1E5))
        }
        return poly
    }
}

