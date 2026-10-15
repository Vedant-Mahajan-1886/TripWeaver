package com.example.smarttravelplanner.data.remote.cloud

import android.util.Log
import com.example.smarttravelplanner.data.local.dao.ItineraryDao
import com.example.smarttravelplanner.data.local.dao.TripDao
import com.example.smarttravelplanner.data.remote.cloud.TripCloudMapper.effectiveUpdatedAt
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the logged-in traveller's trips (with itineraries) in Firestore. */
interface TripSync {
    /** Upload one trip (call after any change to the trip or its days). */
    suspend fun pushTrip(tripId: Long)
    /** Tell other phones a trip was deleted (call before deleting it locally). */
    suspend fun markDeleted(tripId: Long)
    /** Two-way sync of all of [email]'s trips (on login / app start). */
    suspend fun syncAll(email: String)
}

/** Cloud off: nothing leaves the phone. Also used by unit tests. */
object NoTripSync : TripSync {
    override suspend fun pushTrip(tripId: Long) = Unit
    override suspend fun markDeleted(tripId: Long) = Unit
    override suspend fun syncAll(email: String) = Unit
}

/**
 * Firestore implementation. Uploads are not awaited: Firestore keeps them in its offline
 * queue and sends them when the phone is online, so the app never waits for the network.
 * Room stays the copy the screens read; this only copies data to and from the cloud.
 */
@Singleton
class FirestoreTripSync @Inject constructor(
    private val cloudAuth: CloudAuth,
    private val firestore: dagger.Lazy<FirebaseFirestore>,
    private val tripDao: TripDao,
    private val itineraryDao: ItineraryDao
) : TripSync {

    private val trips get() = firestore.get().collection(COLLECTION)

    override suspend fun pushTrip(tripId: Long) {
        val uid = cloudAuth.signedInUid ?: return
        val trip = tripDao.getTripById(tripId) ?: return
        // Only the owner's own trips, and only while their Firebase account is signed in
        if (trip.ownerEmail.isBlank() || trip.ownerEmail != cloudAuth.signedInEmail) return

        val cloudId = trip.cloudId ?: UUID.randomUUID().toString().also { tripDao.setCloudId(tripId, it) }
        // Every upload is a change: stamp the time so other phones know this copy is newer
        // (itinerary edits don't touch the trip row otherwise)
        val now = System.currentTimeMillis()
        tripDao.setUpdatedAt(tripId, now)
        val days = itineraryDao.getItinerariesForTrip(tripId)
        trips.document(cloudId)
            .set(TripCloudMapper.toMap(trip.copy(cloudId = cloudId, updatedAt = now), days, uid))
            .addOnFailureListener { Log.w(TAG, "Trip upload failed: ${it.message}") }
    }

    override suspend fun markDeleted(tripId: Long) {
        val uid = cloudAuth.signedInUid ?: return
        val cloudId = tripDao.getTripById(tripId)?.cloudId ?: return
        trips.document(cloudId)
            .set(
                mapOf(
                    "ownerUid" to uid,
                    "deleted" to true,
                    "updatedAt" to System.currentTimeMillis(),
                    "days" to emptyList<Any>()
                ),
                SetOptions.merge()
            )
            .addOnFailureListener { Log.w(TAG, "Trip delete failed: ${it.message}") }
    }

    override suspend fun syncAll(email: String) {
        val uid = cloudAuth.signedInUid ?: return
        if (cloudAuth.signedInEmail != email) return

        val snapshot = try {
            trips.whereEqualTo("ownerUid", uid).get().await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Trip download failed: ${e.message}")
            return
        }

        val seenInCloud = mutableSetOf<String>()
        var downloaded = 0
        for (doc in snapshot.documents) {
            val remote = TripCloudMapper.fromMap(doc.id, doc.data ?: continue) ?: continue
            seenInCloud += doc.id
            val local = tripDao.getTripByCloudId(doc.id)
            when (TripCloudMapper.decide(local?.effectiveUpdatedAt, remote.trip.updatedAt, remote.deleted)) {
                SyncAction.DOWNLOAD -> {
                    saveLocally(local?.id, remote, email)
                    downloaded++
                }
                SyncAction.DELETE_LOCAL -> local?.let { tripDao.deleteTripById(it.id) }
                SyncAction.UPLOAD -> local?.let { pushTrip(it.id) }
                SyncAction.NOTHING -> Unit
            }
        }

        // Trips that never reached the cloud (made offline, or before sync existed)
        val toUpload = tripDao.getTripsForOwnerOnce(email).filter { it.cloudId == null || it.cloudId !in seenInCloud }
        toUpload.forEach { pushTrip(it.id) }
        Log.i(TAG, "Trips synced: $downloaded downloaded, ${toUpload.size} uploaded")
    }

    private suspend fun saveLocally(localId: Long?, remote: RemoteTrip, email: String) {
        val entity = remote.trip.copy(id = localId ?: 0, ownerEmail = email)
        val id = if (localId != null) {
            tripDao.updateTrip(entity)
            localId
        } else {
            tripDao.insertTrip(entity)
        }
        itineraryDao.deleteItinerariesForTrip(id)
        itineraryDao.insertItineraries(remote.days.map { it.copy(id = 0, tripId = id) })
    }

    private companion object {
        const val COLLECTION = "trips"
        const val TAG = "Cloud"
    }
}
