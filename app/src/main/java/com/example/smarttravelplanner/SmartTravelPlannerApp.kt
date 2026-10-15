package com.example.smarttravelplanner

import android.app.Application
import com.example.smarttravelplanner.data.remote.cloud.CloudStatus
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class SmartTravelPlannerApp : Application(), ImageLoaderFactory {

    @Inject lateinit var cloudStatus: CloudStatus

    override fun onCreate() {
        super.onCreate()
        // One line in Logcat (tag "Cloud") to confirm Firebase is connected
        if (cloudStatus.isEnabled) {
            android.util.Log.i("Cloud", "Firebase connected: project ${cloudStatus.projectId}")
        } else {
            android.util.Log.i("Cloud", "Cloud sync off (no app/google-services.json) - using local data only")
        }
        if (BuildConfig.MAPS_API_KEY.isNotBlank()) {
            try {
                if (!com.google.android.libraries.places.api.Places.isInitialized()) {
                    com.google.android.libraries.places.api.Places.initializeWithNewPlacesApiEnabled(applicationContext, BuildConfig.MAPS_API_KEY)
                }

            } catch (e: Exception) {
                android.util.Log.w("SmartTravelPlannerApp", "Places SDK init failed", e)
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Wikimedia Commons requires a non-generic User-Agent header or returns HTTP 403 Forbidden
                val request = chain.request().newBuilder()
                    .header("User-Agent", "SmartTravelPlanner/1.0 (Android travel planner app)")
                    .build()
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(this)
            .okHttpClient(okHttpClient)
            // 1. In-memory cache capped at 25% of available application memory
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .strongReferencesEnabled(true)
                    .build()
            }
            // 2. Persistent disk cache (50 MB) for downloaded images (e.g. Wikimedia)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(50L * 1024 * 1024)
                    .build()
            }
            // 3. Offload all network and image decoding strictly to Dispatchers.IO
            .dispatcher(Dispatchers.IO)
            // 4. Decode into Hardware Bitmaps to keep pixel buffers in GPU memory and reduce JVM heap pressure
            .allowHardware(true)
            // 5. Crossfade smoothly
            .crossfade(true)
            // 6. Avoid aggressive refetching of remote assets
            .respectCacheHeaders(false)
            .build()
    }
}
