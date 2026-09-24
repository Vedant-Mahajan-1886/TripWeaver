import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.smarttravelplanner"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.smarttravelplanner"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Load secure secrets from local.properties (ignored by git)
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            FileInputStream(localPropertiesFile).use { stream ->
                properties.load(stream)
            }
        }

        // Ollama base URL:
        // - "http://127.0.0.1:11434/" for physical phone over USB with `adb reverse tcp:11434 tcp:11434`
        // - "http://10.0.2.2:11434/" for Android Emulator
        // - "http://<Mac-LAN-IP>:11434/" for physical phone over Wi-Fi
        val ollamaBaseUrl = properties.getProperty("OLLAMA_BASE_URL")
            ?: (project.findProperty("OLLAMA_BASE_URL") as? String)
            ?: "http://127.0.0.1:11434/"
        buildConfigField("String", "OLLAMA_BASE_URL", "\"$ollamaBaseUrl\"")

        // Maps API key from local.properties
        val mapsApiKey = properties.getProperty("MAPS_API_KEY")
            ?: (project.findProperty("MAPS_API_KEY") as? String)
            ?: ""
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")

        // Gmail SMTP credentials from local.properties
        val smtpSenderEmail = properties.getProperty("SMTP_SENDER_EMAIL")
            ?: (project.findProperty("SMTP_SENDER_EMAIL") as? String)
            ?: ""
        val smtpAppPassword = properties.getProperty("SMTP_APP_PASSWORD")
            ?: (project.findProperty("SMTP_APP_PASSWORD") as? String)
            ?: ""
        buildConfigField("String", "SMTP_SENDER_EMAIL", "\"$smtpSenderEmail\"")
        buildConfigField("String", "SMTP_APP_PASSWORD", "\"$smtpAppPassword\"")

        // Google OAuth Web Client ID from local.properties
        val googleWebClientId = properties.getProperty("GOOGLE_WEB_CLIENT_ID")
            ?: (project.findProperty("GOOGLE_WEB_CLIENT_ID") as? String)
            ?: ""
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")

        // Gemini API key from local.properties
        val geminiApiKey = properties.getProperty("GEMINI_API_KEY")
            ?: (project.findProperty("GEMINI_API_KEY") as? String)
            ?: ""
        buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKey\"")
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // Material Icons Extended (needed for Icons.Filled/Outlined)
    implementation("androidx.compose.material:material-icons-extended")
    // Google Fonts API for DM Sans
//    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.compose.ui:ui-text-google-fonts:1.12.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.9.8")


    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // ViewModel
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // DataStore
    implementation(libs.datastore.preferences)

    // Networking (Retrofit + OkHttp for Ollama API)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)

    // Google Maps & Places
    implementation(libs.maps.compose)
    implementation(libs.play.services.maps)
    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation(libs.places)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Image Loading
    implementation(libs.coil.compose)
    implementation("com.github.bumptech.glide:compose:1.0.0-beta10")

    // Google Credential Manager (Sign In with Google)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}