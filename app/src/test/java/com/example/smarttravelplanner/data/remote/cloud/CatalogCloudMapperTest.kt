package com.example.smarttravelplanner.data.remote.cloud

import com.example.smarttravelplanner.data.local.entity.AgentPackageEntity
import com.example.smarttravelplanner.data.local.entity.AgentProfileEntity
import com.example.smarttravelplanner.data.local.entity.GroupMemberEntity
import com.example.smarttravelplanner.data.local.entity.GroupTripEntity
import com.example.smarttravelplanner.data.local.entity.PackageDayEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogCloudMapperTest {

    private val pkg = AgentPackageEntity(
        id = 4, agentEmail = "priya@wanderlust.demo", title = "Royal Rajasthan", destination = "Jaipur",
        pricePerPerson = 18_500.0, description = "Forts and food", highlights = listOf("Amer Fort", "Hawa Mahal"),
        includes = "Hotels", excludes = "Flights", bestSeason = "Oct-Mar", isPublished = true,
        createdAt = 100, updatedAt = 200, cloudId = "p1"
    )
    private val days = listOf(
        PackageDayEntity(id = 9, packageId = 4, dayNumber = 2, title = "Bazaars", activitiesJson = "[2]"),
        PackageDayEntity(id = 8, packageId = 4, dayNumber = 1, title = "Forts", activitiesJson = "[1]")
    )
    private val group = GroupTripEntity(
        id = 3, agentEmail = "priya@wanderlust.demo", title = "Ladakh solo riders", destination = "Leh",
        startDate = "2026-11-10", endDate = "2026-11-15", pricePerPerson = 32_000.0, totalSeats = 8,
        meetingPoint = "Leh airport", description = "Bikes included", packageId = 4,
        isPublished = true, isCancelled = false, createdAt = 100, updatedAt = 300, cloudId = "g1"
    )

    @Test
    fun package_roundTrip() {
        val back = CatalogCloudMapper.packageFromMap("p1", CatalogCloudMapper.packageToMap(pkg, days, "uid"))!!
        assertEquals(pkg.copy(id = 0), back.pkg)
        assertEquals(listOf(1, 2), back.days.map { it.dayNumber })
        assertEquals("Forts", back.days[0].title)
        assertFalse(back.deleted)
    }

    @Test
    fun package_wholeNumberPrice_fromFirestore() {
        val map = CatalogCloudMapper.packageToMap(pkg, days, "uid").toMutableMap().apply { put("pricePerPerson", 18_500L) }
        assertEquals(18_500.0, CatalogCloudMapper.packageFromMap("p1", map)!!.pkg.pricePerPerson, 0.0)
    }

    @Test
    fun group_roundTrip_keepsPackageLinkAsCloudId() {
        val map = CatalogCloudMapper.groupToMap(group, packageCloudId = "p1", ownerUid = "uid")
        val back = CatalogCloudMapper.groupFromMap("g1", map)!!
        assertEquals(group.copy(id = 0, packageId = null), back.group) // local id is mapped later
        assertEquals("p1", back.packageCloudId)
    }

    @Test
    fun group_cancelledFlagSurvives() {
        val map = CatalogCloudMapper.groupToMap(group.copy(isCancelled = true), null, "uid")
        assertTrue(CatalogCloudMapper.groupFromMap("g1", map)!!.group.isCancelled)
    }

    @Test
    fun member_roundTrip_toLocalGroup() {
        val member = GroupMemberEntity(groupId = 3, travellerEmail = "riya@x.com", travellerName = "Riya", joinedAt = 55)
        val back = CatalogCloudMapper.memberFromMap(groupId = 12, data = CatalogCloudMapper.memberToMap(member))!!
        assertEquals(member.copy(groupId = 12), back)
    }

    @Test
    fun agent_roundTrip() {
        val profile = AgentProfileEntity("priya@wanderlust.demo", "Wanderlust Journeys", "Jaipur", listOf("Heritage"), 10)
        assertEquals(profile, CatalogCloudMapper.agentFromMap(CatalogCloudMapper.agentToMap(profile, "uid")))
    }

    @Test
    fun brokenDocuments_areSkipped() {
        assertNull(CatalogCloudMapper.packageFromMap("x", mapOf("title" to "No agent")))
        assertNull(CatalogCloudMapper.groupFromMap("x", mapOf("agentEmail" to "a@x.com", "title" to "No dates")))
        assertNull(CatalogCloudMapper.memberFromMap(1, mapOf("travellerName" to "No email")))
        assertNull(CatalogCloudMapper.agentFromMap(mapOf("email" to "a@x.com")))
    }
}
