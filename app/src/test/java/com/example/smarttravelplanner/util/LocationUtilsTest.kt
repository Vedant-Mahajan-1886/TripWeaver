package com.example.smarttravelplanner.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocationUtilsTest {

    @Test
    fun testBengaluruKoramangalaExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "Shop 4, 100 Feet Rd, Koramangala 4th Block, Bengaluru, Karnataka 560034, India",
            country = "India",
            adminArea = "Karnataka",
            postalCode = "560034"
        )
        assertEquals("Bengaluru", city)
    }

    @Test
    fun testNoidaSector62ExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "Tower B, Sector 62, Noida, Uttar Pradesh 201309, India",
            country = "India",
            adminArea = "Uttar Pradesh",
            postalCode = "201309"
        )
        assertEquals("Noida", city)
    }

    @Test
    fun testMumbaiBandraWestExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "Hill Road, Bandra West, Mumbai, Maharashtra 400050, India",
            country = "India",
            adminArea = "Maharashtra",
            postalCode = "400050"
        )
        assertEquals("Mumbai", city)
    }

    @Test
    fun testNewDelhiConnaughtPlaceExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "Block A, Connaught Place, New Delhi 110001, India",
            country = "India",
            adminArea = "Delhi",
            postalCode = "110001"
        )
        assertEquals("New Delhi", city)
    }

    @Test
    fun testPuneKothrudExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "Paud Road, Kothrud, Pune, Maharashtra 411038, India",
            country = "India",
            adminArea = "Maharashtra",
            postalCode = "411038"
        )
        assertEquals("Pune", city)
    }

    @Test
    fun testJaipurCSchemeExtractsCity() {
        val city = extractCityFromAddressComponents(
            line = "C-Scheme, Ashok Nagar, Jaipur, Rajasthan 302001, India",
            country = "India",
            adminArea = "Rajasthan",
            postalCode = "302001"
        )
        assertEquals("Jaipur", city)
    }

    @Test
    fun testCleanSubAdminArea() {
        assertEquals("Bengaluru", cleanSubAdminArea("Bengaluru Urban"))
        assertEquals("Mumbai", cleanSubAdminArea("Mumbai Suburban"))
        assertEquals("Mumbai", cleanSubAdminArea("Mumbai City"))
        assertEquals("Delhi", cleanSubAdminArea("South Delhi"))
        assertEquals("New Delhi", cleanSubAdminArea("New Delhi"))
        assertEquals("Noida", cleanSubAdminArea("Gautam Buddha Nagar"))
        assertEquals("Gurugram", cleanSubAdminArea("Gurgaon"))
        assertEquals("Pune", cleanSubAdminArea("Pune District"))
        assertEquals("Lucknow", cleanSubAdminArea("Lucknow Division"))
    }

    @Test
    fun testIsLikelyNeighborhood() {
        val subLocalities = setOf("koramangala", "bandra")
        assertTrue(isLikelyNeighborhood("Koramangala", subLocalities))
        assertTrue(isLikelyNeighborhood("Sector 62", emptySet()))
        assertTrue(isLikelyNeighborhood("Indira Nagar", emptySet()))
        assertTrue(isLikelyNeighborhood("HSR Layout", emptySet()))
        assertTrue(isLikelyNeighborhood("Bandra West", emptySet()))
        assertFalse(isLikelyNeighborhood("Bengaluru", emptySet()))
        assertFalse(isLikelyNeighborhood("Mumbai", emptySet()))
        assertFalse(isLikelyNeighborhood("New Delhi", emptySet()))
        assertFalse(isLikelyNeighborhood("Gandhinagar", emptySet()))
        assertFalse(isLikelyNeighborhood("Srinagar", emptySet()))
    }
}
