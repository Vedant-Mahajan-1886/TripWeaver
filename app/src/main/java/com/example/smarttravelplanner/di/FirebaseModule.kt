package com.example.smarttravelplanner.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Firebase instances for cloud sync.
 *
 * Inject them as dagger.Lazy<FirebaseAuth> / dagger.Lazy<FirebaseFirestore> and only call
 * get() after checking CloudStatus.isEnabled - getInstance() crashes when the app was built
 * without google-services.json.
 */
@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()
}
