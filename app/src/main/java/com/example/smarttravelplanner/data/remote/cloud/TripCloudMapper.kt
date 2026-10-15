package com.example.smarttravelplanner.data.remote.cloud

import com.example.smarttravelplanner.data.local.entity.ItineraryEntity
import com.example.smarttravelplanner.data.local.entity.TripEntity

/** What to do when a phone compares its copy of a trip with the cloud copy. */
enum class SyncAction { DOWNLOAD, UPLOAD, DELETE_LOCAL, NOTHING }

/** A trip as read back from Firestore. */
data class RemoteTrip(
    val trip: TripEntity,
    val days: List<ItineraryEntity>,
    val deleted: Boolean
)

/**
 * Converts trips to and from Firestore documents (collection "trips") and decides which
 * copy wins. Pure functions on plain maps, so it is unit tested without Firebase.
 *
 * Document fields: ownerUid, ownerEmail, title, destination, startDate, endDate, budget,
 * adults, kids, interests, notes, coverImageUrl, createdAt, updatedAt, deleted,
 * days = [{dayNumber, date, activitiesJson}]. The itinerary travels inside the trip
 * document, so one read gives the whole trip.
 */
object TripCloudMapper {

    /** updatedAt, or createdAt for trips made before sync existed (updatedAt = 0). */
    val TripEntity.effectiveUpdatedAt: Long get() = if (updatedAt > 0) updatedAt else createdAt

    fun toMap(trip: TripEntity, days: List<ItineraryEntity>, ownerUid: String): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "ownerEmail" to trip.ownerEmail,
        "title" to trip.title,
        "destination" to trip.destination,
        "startDate" to trip.startDate,
        "endDate" to trip.endDate,
        "budget" to trip.budget,
        "adults" to trip.adults,
        "kids" to trip.kids,
        "interests" to trip.interests,
        "notes" to trip.notes,
        "coverImageUrl" to trip.coverImageUrl,
        "createdAt" to trip.createdAt,
        "updatedAt" to trip.effectiveUpdatedAt,
        "deleted" to false,
        "days" to days.sortedBy { it.dayNumber }.map {
            mapOf("dayNumber" to it.dayNumber, "date" to it.date, "activitiesJson" to it.activitiesJson)
        }
    )

    /** Null if the document is unreadable (missing title/dates). Local ids are left at 0. */
    fun fromMap(cloudId: String, data: Map<String, Any?>): RemoteTrip? {
        val title = data["title"] as? String ?: return null
        val startDate = data["startDate"] as? String ?: return null
        val endDate = data["endDate"] as? String ?: return null
        val updatedAt = (data["updatedAt"] as? Number)?.toLong() ?: 0L
        val trip = TripEntity(
            title = title,
            destination = data["destination"] as? String ?: "",
            startDate = startDate,
            endDate = endDate,
            budget = (data["budget"] as? Number)?.toDouble() ?: 0.0,
            adults = (data["adults"] as? Number)?.toInt() ?: 1,
            kids = (data["kids"] as? Number)?.toInt() ?: 0,
            interests = (data["interests"] as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
            notes = data["notes"] as? String ?: "",
            coverImageUrl = data["coverImageUrl"] as? String ?: "",
            createdAt = (data["createdAt"] as? Number)?.toLong() ?: updatedAt,
            ownerEmail = (data["ownerEmail"] as? String)?.lowercase() ?: "",
            cloudId = cloudId,
            updatedAt = updatedAt
        )
        val days = (data["days"] as? List<*>).orEmpty().mapNotNull { raw ->
            val day = raw as? Map<*, *> ?: return@mapNotNull null
            ItineraryEntity(
                tripId = 0,
                dayNumber = (day["dayNumber"] as? Number)?.toInt() ?: return@mapNotNull null,
                date = day["date"] as? String ?: "",
                activitiesJson = day["activitiesJson"] as? String ?: "[]"
            )
        }
        return RemoteTrip(trip, days, deleted = data["deleted"] == true)
    }

    /**
     * Newer copy wins. A delete on another phone wins unless this phone changed the trip
     * afterwards (then the trip comes back).
     */
    fun decide(localUpdatedAt: Long?, remoteUpdatedAt: Long, remoteDeleted: Boolean): SyncAction = when {
        localUpdatedAt == null -> if (remoteDeleted) SyncAction.NOTHING else SyncAction.DOWNLOAD
        remoteDeleted -> if (remoteUpdatedAt >= localUpdatedAt) SyncAction.DELETE_LOCAL else SyncAction.UPLOAD
        remoteUpdatedAt > localUpdatedAt -> SyncAction.DOWNLOAD
        localUpdatedAt > remoteUpdatedAt -> SyncAction.UPLOAD
        else -> SyncAction.NOTHING
    }
}
