package com.example.smarttravelplanner.data.remote.cloud

import com.example.smarttravelplanner.data.local.entity.AgentPackageEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.GroupMemberEntity
import com.example.smarttravelplanner.data.local.entity.GroupTripEntity
import com.example.smarttravelplanner.data.local.entity.PackageDayEntity

/** A package as read back from Firestore (ids left at 0; the caller fills them in). */
data class RemotePackage(val pkg: AgentPackageEntity, val days: List<PackageDayEntity>, val deleted: Boolean)

/** A group as read back from Firestore. [packageCloudId] links to the package it follows. */
data class RemoteGroup(val group: GroupTripEntity, val packageCloudId: String?, val deleted: Boolean)

/**
 * The agents' catalogue in Firestore:
 *  - packages/{id}                  ready-made trips (days inside the document)
 *  - groups/{id}                    group trips
 *  - groups/{id}/members/{email}    who joined (one document per traveller)
 *  - agents/{email}                 public agency details shown on the cards
 * Pure functions on plain maps, so they are unit tested without Firebase.
 */
object CatalogCloudMapper {

    // -- Packages --------------------------------------------------------------

    fun packageToMap(pkg: AgentPackageEntity, days: List<PackageDayEntity>, ownerUid: String): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "agentEmail" to pkg.agentEmail,
        "title" to pkg.title,
        "destination" to pkg.destination,
        "pricePerPerson" to pkg.pricePerPerson,
        "description" to pkg.description,
        "highlights" to pkg.highlights,
        "includes" to pkg.includes,
        "excludes" to pkg.excludes,
        "bestSeason" to pkg.bestSeason,
        "published" to pkg.isPublished,
        "deleted" to false,
        "createdAt" to pkg.createdAt,
        "updatedAt" to pkg.updatedAt,
        "days" to days.sortedBy { it.dayNumber }.map {
            mapOf("dayNumber" to it.dayNumber, "title" to it.title, "activitiesJson" to it.activitiesJson)
        }
    )

    fun packageFromMap(cloudId: String, data: Map<String, Any?>): RemotePackage? {
        val agentEmail = (data["agentEmail"] as? String)?.lowercase() ?: return null
        val title = data["title"] as? String ?: return null
        val updatedAt = data.long("updatedAt")
        val pkg = AgentPackageEntity(
            agentEmail = agentEmail,
            title = title,
            destination = data["destination"] as? String ?: "",
            pricePerPerson = (data["pricePerPerson"] as? Number)?.toDouble() ?: 0.0,
            description = data["description"] as? String ?: "",
            highlights = data.strings("highlights"),
            includes = data["includes"] as? String ?: "",
            excludes = data["excludes"] as? String ?: "",
            bestSeason = data["bestSeason"] as? String ?: "",
            isPublished = data["published"] == true,
            createdAt = data.long("createdAt").takeIf { it > 0 } ?: updatedAt,
            updatedAt = updatedAt,
            cloudId = cloudId
        )
        val days = (data["days"] as? List<*>).orEmpty().mapNotNull { raw ->
            val day = raw as? Map<*, *> ?: return@mapNotNull null
            PackageDayEntity(
                packageId = 0,
                dayNumber = (day["dayNumber"] as? Number)?.toInt() ?: return@mapNotNull null,
                title = day["title"] as? String ?: "",
                activitiesJson = day["activitiesJson"] as? String ?: "[]"
            )
        }
        return RemotePackage(pkg, days, deleted = data["deleted"] == true)
    }

    // -- Groups ----------------------------------------------------------------

    fun groupToMap(group: GroupTripEntity, packageCloudId: String?, ownerUid: String): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "agentEmail" to group.agentEmail,
        "title" to group.title,
        "destination" to group.destination,
        "startDate" to group.startDate,
        "endDate" to group.endDate,
        "pricePerPerson" to group.pricePerPerson,
        "totalSeats" to group.totalSeats,
        "meetingPoint" to group.meetingPoint,
        "description" to group.description,
        "packageCloudId" to packageCloudId,
        "published" to group.isPublished,
        "cancelled" to group.isCancelled,
        "deleted" to false,
        "createdAt" to group.createdAt,
        "updatedAt" to group.updatedAt
    )

    fun groupFromMap(cloudId: String, data: Map<String, Any?>): RemoteGroup? {
        val agentEmail = (data["agentEmail"] as? String)?.lowercase() ?: return null
        val title = data["title"] as? String ?: return null
        val startDate = data["startDate"] as? String ?: return null
        val endDate = data["endDate"] as? String ?: return null
        val updatedAt = data.long("updatedAt")
        val group = GroupTripEntity(
            agentEmail = agentEmail,
            title = title,
            destination = data["destination"] as? String ?: "",
            startDate = startDate,
            endDate = endDate,
            pricePerPerson = (data["pricePerPerson"] as? Number)?.toDouble() ?: 0.0,
            totalSeats = (data["totalSeats"] as? Number)?.toInt() ?: 0,
            meetingPoint = data["meetingPoint"] as? String ?: "",
            description = data["description"] as? String ?: "",
            isPublished = data["published"] == true,
            isCancelled = data["cancelled"] == true,
            createdAt = data.long("createdAt").takeIf { it > 0 } ?: updatedAt,
            updatedAt = updatedAt,
            cloudId = cloudId
        )
        return RemoteGroup(group, data["packageCloudId"] as? String, deleted = data["deleted"] == true)
    }

    // -- Members and agents ----------------------------------------------------

    fun memberToMap(member: GroupMemberEntity): Map<String, Any?> = mapOf(
        "travellerEmail" to member.travellerEmail,
        "travellerName" to member.travellerName,
        "joinedAt" to member.joinedAt
    )

    /** [groupId] is the LOCAL group id the member belongs to. */
    fun memberFromMap(groupId: Long, data: Map<String, Any?>): GroupMemberEntity? {
        val email = (data["travellerEmail"] as? String)?.lowercase() ?: return null
        return GroupMemberEntity(
            groupId = groupId,
            travellerEmail = email,
            travellerName = data["travellerName"] as? String ?: email,
            joinedAt = data.long("joinedAt")
        )
    }

    fun agentToMap(profile: AgentProfileEntity, ownerUid: String): Map<String, Any?> = mapOf(
        "ownerUid" to ownerUid,
        "email" to profile.email,
        "agencyName" to profile.agencyName,
        "city" to profile.city,
        "specialities" to profile.specialities,
        "updatedAt" to profile.updatedAt
    )

    fun agentFromMap(data: Map<String, Any?>): AgentProfileEntity? {
        val email = (data["email"] as? String)?.lowercase() ?: return null
        val agency = data["agencyName"] as? String ?: return null
        return AgentProfileEntity(
            email = email,
            agencyName = agency,
            city = data["city"] as? String ?: "",
            specialities = data.strings("specialities"),
            updatedAt = data.long("updatedAt")
        )
    }

    private fun Map<String, Any?>.long(key: String): Long = (this[key] as? Number)?.toLong() ?: 0L
    private fun Map<String, Any?>.strings(key: String): List<String> =
        (this[key] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
}
