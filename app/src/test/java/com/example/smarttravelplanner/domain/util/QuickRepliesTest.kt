package com.example.smarttravelplanner.domain.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickRepliesTest {

    @Test
    fun clean_trimsCollapsesSpacesAndDropsBlank() {
        assertEquals("Closed on Mondays.", QuickReplies.clean("  Closed   on\nMondays.  "))
        assertNull(QuickReplies.clean("   "))
    }

    @Test
    fun clean_limitsLength() {
        assertEquals(QuickReplies.MAX_LENGTH, QuickReplies.clean("a".repeat(500))!!.length)
    }

    @Test
    fun encodeDecode_roundTrip() {
        val list = listOf("One", "Two words", "Three")
        assertEquals(list, QuickReplies.decode(QuickReplies.encode(list)))
    }

    @Test
    fun decode_emptyStringMeansNoTemplates() {
        assertEquals(emptyList<String>(), QuickReplies.decode(""))
    }

    @Test
    fun add_putsNewOnTop_withoutDuplicates() {
        val result = QuickReplies.add(listOf("Book early.", "Closed on Mondays."), "closed on mondays.")
        assertEquals(listOf("closed on mondays.", "Book early."), result)
    }

    @Test
    fun add_keepsAtMostMaxCount() {
        val full = (1..QuickReplies.MAX_COUNT).map { "Reply $it" }
        val result = QuickReplies.add(full, "Newest")
        assertEquals(QuickReplies.MAX_COUNT, result.size)
        assertEquals("Newest", result.first())
    }

    @Test
    fun add_ignoresBlank() {
        assertEquals(listOf("A"), QuickReplies.add(listOf("A"), "  "))
    }

    @Test
    fun insertInto_replacesEmpty_orAppendsWithSpace() {
        assertEquals("Book early.", QuickReplies.insertInto("", "Book early."))
        assertEquals("Lovely plan. Book early.", QuickReplies.insertInto("Lovely plan.  ", "Book early."))
    }
}
