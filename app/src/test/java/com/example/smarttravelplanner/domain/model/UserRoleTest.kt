package com.example.smarttravelplanner.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class UserRoleTest {

    @Test
    fun readsStoredNames() {
        assertEquals(UserRole.TRAVELLER, UserRole.fromStorage("TRAVELLER"))
        assertEquals(UserRole.AGENT, UserRole.fromStorage("AGENT"))
    }

    @Test
    fun isCaseInsensitiveAndTrimsSpaces() {
        assertEquals(UserRole.AGENT, UserRole.fromStorage("  agent "))
    }

    @Test
    fun unknownOrMissingValuesAreTravellers() {
        assertEquals(UserRole.TRAVELLER, UserRole.fromStorage(null))
        assertEquals(UserRole.TRAVELLER, UserRole.fromStorage(""))
        assertEquals(UserRole.TRAVELLER, UserRole.fromStorage("ADMIN"))
    }
}
