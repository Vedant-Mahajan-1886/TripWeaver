package com.example.smarttravelplanner.di

import com.example.smarttravelplanner.data.remote.cloud.CloudAuth
import com.example.smarttravelplanner.data.remote.cloud.FirebaseCloudAuth
import com.example.smarttravelplanner.data.remote.cloud.CatalogSync
import com.example.smarttravelplanner.data.remote.cloud.FirestoreCatalogSync
import com.example.smarttravelplanner.data.remote.cloud.FirestoreTripSync
import com.example.smarttravelplanner.data.remote.cloud.TripSync
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

    /** Login code talks to [CloudAuth]; the real one is Firebase (it checks CloudStatus itself). */
    @Provides
    @Singleton
    fun provideCloudAuth(impl: FirebaseCloudAuth): CloudAuth = impl

    /** Trip sync; FirestoreTripSync does nothing unless a Firebase user is signed in. */
    @Provides
    @Singleton
    fun provideTripSync(impl: FirestoreTripSync): TripSync = impl

    /** Packages, group trips, joins and agency details. */
    @Provides
    @Singleton
    fun provideCatalogSync(impl: FirestoreCatalogSync): CatalogSync = impl
}
