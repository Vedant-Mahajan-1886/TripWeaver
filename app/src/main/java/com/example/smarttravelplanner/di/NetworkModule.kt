package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.BuildConfig
import com.example.smarttravelplanner.data.remote.api.OllamaApi
import com.example.smarttravelplanner.data.remote.api.WeatherApi
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    // ── Shared Gson instance ──────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder().create()

    // ── Shared OkHttpClient ───────────────────────────────────────────────────
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG)
                HttpLoggingInterceptor.Level.BODY
            else
                HttpLoggingInterceptor.Level.NONE
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(60, TimeUnit.SECONDS)   // LLM responses can be slow
            .readTimeout(240, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // ── Ollama Retrofit (dynamic base URL from UserPreferences) ───────────────
    @Provides
    @Singleton
    @Named("ollama")
    fun provideOllamaRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.OLLAMA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideOllamaApi(@Named("ollama") retrofit: Retrofit): OllamaApi =
        retrofit.create(OllamaApi::class.java)

    // ── Open-Meteo Retrofit (weather, no API key) ─────────────────────────────
    @Provides
    @Singleton
    @Named("weather")
    fun provideWeatherRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://api.open-meteo.com/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideWeatherApi(@Named("weather") retrofit: Retrofit): WeatherApi =
        retrofit.create(WeatherApi::class.java)

    // ── Wikipedia Retrofit (Free destination image search, no API key) ────────
    @Provides
    @Singleton
    @Named("wikipedia")
    fun provideWikipediaRetrofit(okHttpClient: OkHttpClient, gson: Gson): Retrofit =
        Retrofit.Builder()
            .baseUrl("https://en.wikipedia.org/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

    @Provides
    @Singleton
    fun provideWikipediaApi(@Named("wikipedia") retrofit: Retrofit): com.example.smarttravelplanner.data.remote.api.WikipediaApi =
        retrofit.create(com.example.smarttravelplanner.data.remote.api.WikipediaApi::class.java)
}
