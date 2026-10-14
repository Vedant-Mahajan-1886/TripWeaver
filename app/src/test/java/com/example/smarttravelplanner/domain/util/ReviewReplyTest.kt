package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.AgentReview
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewReplyTest {

    @Test
    fun cleanReviewReply_trims() {
        assertEquals("Thank you!", BookingRules.cleanReviewReply("   Thank you!  "))
    }

    @Test
    fun cleanReviewReply_blankMeansRemove() {
        assertEquals("", BookingRules.cleanReviewReply("   "))
    }

    @Test
    fun cleanReviewReply_limitsLength() {
        assertEquals(BookingRules.MAX_REVIEW_REPLY, BookingRules.cleanReviewReply("x".repeat(900)).length)
    }

    private fun review(reply: String) = AgentReview(
        shareId = 1, agentEmail = "a@x.com", tripDestination = "Goa", stars = 5,
        comment = "Great", createdAt = 0, reply = reply
    )

    @Test
    fun hasReply_onlyWhenNotBlank() {
        assertTrue(review("Thanks!").hasReply)
        assertFalse(review("").hasReply)
        assertFalse(review("   ").hasReply)
    }
}
