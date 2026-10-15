package com.example.smarttravelplanner.data.remote.cloud

import android.content.Context
import com.example.smarttravelplanner.BuildConfig
import com.google.firebase.FirebaseApp
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tells the rest of the app whether cloud sync (Firebase) is available.
 *
 * Cloud is OFF when the build had no app/google-services.json (e.g. a teammate without the
 * private config file). Repositories must check [isEnabled] before touching Firebase and
 * fall back to the local Room data, so the app keeps working offline.
 */
@Singleton
class CloudStatus @Inject constructor(
    @ApplicationContext private val context: Context
) {
    val isEnabled: Boolean by lazy {
        BuildConfig.CLOUD_ENABLED && FirebaseApp.getApps(context).isNotEmpty()
    }

    /** Firebase project id, e.g. "smart-travel-planner-d1b46" (null when cloud is off). */
    val projectId: String? by lazy {
        if (isEnabled) FirebaseApp.getInstance().options.projectId else null
    }
}
