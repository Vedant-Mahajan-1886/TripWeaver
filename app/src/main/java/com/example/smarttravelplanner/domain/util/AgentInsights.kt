package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/** Money earned in one calendar month (from trips the agent booked). */
data class MonthRevenue(val month: YearMonth, val amount: Double)

/** Business numbers for the agent's Profile tab. All amounts are in INR. */
data class AgentInsights(
    /** Quote totals of trips the agent booked (Booked + Completed). */
    val earned: Double,
    val bookedTrips: Int,
    /** Quote totals still waiting: quote sent, or accepted but not booked yet. */
    val pipeline: Double,
    val pipelineTrips: Int,
    /** Of the trips where the traveller picked an agent, the % that picked this one. Null if none yet. */
    val winRatePercent: Int?,
    val averageBooking: Double?,
    /** Earnings for the last [MONTHS] months, oldest first (zero-filled). */
    val months: List<MonthRevenue>
) {
    companion object {
        const val MONTHS = 6
    }
}

private val EARNED = setOf(ShareStatus.BOOKED, ShareStatus.COMPLETED)
private val PIPELINE = setOf(ShareStatus.QUOTE_SENT, ShareStatus.QUOTE_ACCEPTED)

/**
 * Works out the agent's insights from their trips and bookings. Pure function, unit tested.
 * A booking counts in the month it was booked (falling back to when the quote was answered or sent).
 */
fun computeAgentInsights(
    incoming: List<IncomingShare>,
    bookings: Map<Long, Booking>,
    today: LocalDate,
    zone: ZoneId = ZoneId.systemDefault()
): AgentInsights {
    fun total(item: IncomingShare) = bookings[item.share.id]?.quoteTotal ?: 0.0

    val earnedItems = incoming.filter { it.share.status in EARNED }
    val pipelineItems = incoming.filter { it.share.status in PIPELINE }
    val earned = earnedItems.sumOf(::total)

    val won = incoming.count { it.share.status.isChosen }
    val lost = incoming.count { it.share.status == ShareStatus.CLOSED }
    val winRate = if (won + lost == 0) null else Math.round(won * 100.0 / (won + lost)).toInt()

    val thisMonth = YearMonth.from(today)
    val months = (AgentInsights.MONTHS - 1 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
    val perMonth = earnedItems.groupBy { item ->
        val booking = bookings[item.share.id]
        val millis = booking?.bookedAt ?: booking?.quoteRespondedAt ?: booking?.quoteSentAt ?: item.share.updatedAt
        YearMonth.from(Instant.ofEpochMilli(millis).atZone(zone))
    }.mapValues { (_, items) -> items.sumOf(::total) }

    return AgentInsights(
        earned = earned,
        bookedTrips = earnedItems.size,
        pipeline = pipelineItems.sumOf(::total),
        pipelineTrips = pipelineItems.size,
        winRatePercent = winRate,
        averageBooking = if (earnedItems.isEmpty()) null else earned / earnedItems.size,
        months = months.map { MonthRevenue(it, perMonth[it] ?: 0.0) }
    )
}
