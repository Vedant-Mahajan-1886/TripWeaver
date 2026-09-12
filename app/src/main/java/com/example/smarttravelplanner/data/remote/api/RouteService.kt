package com.example.smarttravelplanner.data.remote.api

import android.util.Log
import android.util.LruCache
import com.example.smarttravelplanner.BuildConfig
import com.example.smarttravelplanner.ui.screens.tripdetail.MapUtils
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object RouteService {

    private const val TAG = "RouteService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    // In-memory cache to prevent redundant network calls
    private val routeCache = LruCache<String, List<LatLng>>(50)

    /**
     * Resolves a turn-by-turn street route connecting sequential waypoints.
     * Tries Google Directions API first (if API key is present), then falls back
     * seamlessly to OSRM (free, open source routing).
     */
    suspend fun getStreetRoute(waypoints: List<LatLng>): List<LatLng> = withContext(Dispatchers.IO) {
        if (waypoints.size < 2) return@withContext waypoints

        val cacheKey = waypoints.joinToString(";") { "%.5f,%.5f".format(it.latitude, it.longitude) }
        val cached = routeCache.get(cacheKey)
        if (cached != null && cached.isNotEmpty()) {
            Log.d(TAG, "Returning cached street route (${cached.size} points)")
            return@withContext cached
        }

        // 1. Try Google Directions API if MAPS_API_KEY is available
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            val googleRoute = fetchGoogleDirections(waypoints)
            if (googleRoute.isNotEmpty()) {
                Log.d(TAG, "Google Directions API resolved ${googleRoute.size} road points")
                routeCache.put(cacheKey, googleRoute)
                return@withContext googleRoute
            }
        }

        // 2. Fallback to OSRM (Open Source Routing Machine) — Free, Keyless, Worldwide Roads
        val osrmRoute = fetchOsrmRoute(waypoints)
        if (osrmRoute.isNotEmpty()) {
            Log.d(TAG, "OSRM resolved ${osrmRoute.size} road points")
            routeCache.put(cacheKey, osrmRoute)
            return@withContext osrmRoute
        }

        // 3. Final fallback: return original waypoints if both routing engines fail
        Log.w(TAG, "All routing providers failed, falling back to direct waypoints")
        waypoints
    }

    /**
     * Fetches driving route from Google Directions API.
     */
    private fun fetchGoogleDirections(waypoints: List<LatLng>): List<LatLng> {
        return try {
            val origin = "${waypoints.first().latitude},${waypoints.first().longitude}"
            val destination = "${waypoints.last().latitude},${waypoints.last().longitude}"

            val waypointsParam = if (waypoints.size > 2) {
                val intermediates = waypoints.subList(1, waypoints.size - 1)
                    .joinToString("|") { "${it.latitude},${it.longitude}" }
                "&waypoints=optimize:false|$intermediates"
            } else {
                ""
            }

            val url = "https://maps.googleapis.com/maps/api/directions/json" +
                    "?origin=$origin" +
                    "&destination=$destination" +
                    waypointsParam +
                    "&mode=driving" +
                    "&key=${BuildConfig.MAPS_API_KEY}"

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Google Directions HTTP ${response.code}")
                    return emptyList()
                }

                val bodyStr = response.body?.string() ?: return emptyList()
                val json = JSONObject(bodyStr)
                val status = json.optString("status")

                if (status != "OK") {
                    Log.w(TAG, "Google Directions status: $status (error_message: ${json.optString("error_message")})")
                    return emptyList()
                }

                val routes = json.optJSONArray("routes") ?: return emptyList()
                if (routes.length() == 0) return emptyList()

                val overviewPolyline = routes.getJSONObject(0)
                    .optJSONObject("overview_polyline")
                    ?.optString("points") ?: return emptyList()

                MapUtils.decodePolyline(overviewPolyline)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Directions fetch failed: ${e.message}")
            emptyList()
        }
    }

    /**
     * Fetches driving route from OSRM (Open Source Routing Machine).
     * Format: /route/v1/driving/{lon1},{lat1};{lon2},{lat2};...
     */
    private fun fetchOsrmRoute(waypoints: List<LatLng>): List<LatLng> {
        return try {
            // OSRM coordinates are in longitude,latitude order
            val coordsParam = waypoints.joinToString(";") { "${it.longitude},${it.latitude}" }
            val url = "https://router.project-osrm.org/route/v1/driving/$coordsParam?overview=full&geometries=polyline"

            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner)")
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "OSRM HTTP error: ${response.code}")
                    return emptyList()
                }

                val bodyStr = response.body?.string() ?: return emptyList()
                val json = JSONObject(bodyStr)
                val code = json.optString("code")

                if (code != "Ok") {
                    Log.w(TAG, "OSRM route code: $code")
                    return emptyList()
                }

                val routes = json.optJSONArray("routes") ?: return emptyList()
                if (routes.length() == 0) return emptyList()

                val geometry = routes.getJSONObject(0).optString("geometry")
                if (geometry.isBlank()) return emptyList()

                MapUtils.decodePolyline(geometry)
            }
        } catch (e: Exception) {
            Log.e(TAG, "OSRM route fetch failed: ${e.message}")
            emptyList()
        }
    }
}
