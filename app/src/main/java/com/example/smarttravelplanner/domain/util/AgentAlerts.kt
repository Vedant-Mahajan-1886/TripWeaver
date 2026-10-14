package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.agent.Booking
import com.example.smarttravelplanner.domain.model.agent.IncomingShare
import com.example.smarttravelplanner.domain.model.agent.ShareStatus
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

/** URGENT = the agent should act now; REMINDER = good to know. */
enum class AlertLevel { URGENT, REMINDER }

/** One item on the agent's Alerts card. Tapping it opens [shareId]. */
data class AgentAlert(
    val shareId: Long,
    val level: AlertLevel,
    val title: String,
    val detail: String
)

private val SHORT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.ENGLISH)
private const val STALE_REQUEST_MILLIS = 48L * 60 * 60 * 1000
private const val QUOTE_WARNING_DAYS = 2L
private const val TRIP_WARNING_DAYS = 3L

/**
 * Builds the agent's alerts from their trips and bookings. Pure function, unit tested.
 * Urgent alerts come first; within a level, the order is the order of the rules below.
 */
fun computeAgentAlerts(
    incoming: List<IncomingShare>,
    bookings: Map<Long, Booking>,
    today: LocalDate,
    nowMillis: Long
): List<AgentAlert> {
    val alerts = mutableListOf<AgentAlert>()

    for (item in incoming) {
        val id = item.share.id
        val place = item.tripDestination
        val who = item.travellerName
        when (item.share.status) {
            // Chosen but no quote yet
            ShareStatus.CONFIRMED -> alerts.add(AgentAlert(
                id, AlertLevel.URGENT, "Send a quote \u00B7 $place",
                "$who chose you and is waiting for your price."
            ))

            // Quote accepted, not booked yet
            ShareStatus.QUOTE_ACCEPTED -> alerts.add(AgentAlert(
                id, AlertLevel.URGENT, "Ready to book \u00B7 $place",
                "$who accepted your quote. Book it and add the reference."
            ))

            // Quote expiring or expired
            ShareStatus.QUOTE_SENT -> {
                val validUntil = bookings[id]?.quoteValidUntil ?: continue
                val days = ChronoUnit.DAYS.between(today, validUntil)
                when {
                    days < 0 -> alerts.add(AgentAlert(
                        id, AlertLevel.URGENT, "Quote expired \u00B7 $place",
                        "It expired on ${validUntil.format(SHORT_DATE)}. Send a new quote or follow up with $who."
                    ))
                    days == 0L -> alerts.add(AgentAlert(
                        id, AlertLevel.URGENT, "Quote expires today \u00B7 $place",
                        "$who hasn't answered yet."
                    ))
                    days <= QUOTE_WARNING_DAYS -> alerts.add(AgentAlert(
                        id, AlertLevel.REMINDER,
                        "Quote expires in $days day${if (days == 1L) "" else "s"} \u00B7 $place",
                        "$who hasn't answered yet."
                    ))
                    else -> Unit
                }
            }

            // Booked trip starting soon
            ShareStatus.BOOKED -> when (val timing = tripTiming(item.tripStartDate, item.tripEndDate, today)) {
                TripTiming.StartsToday -> alerts.add(AgentAlert(
                    id, AlertLevel.REMINDER, "$place trip starts today",
                    "Make sure $who has everything they need."
                ))
                is TripTiming.Upcoming -> if (timing.daysToGo <= TRIP_WARNING_DAYS) {
                    alerts.add(AgentAlert(
                        id, AlertLevel.REMINDER,
                        "$place trip starts ${if (timing.daysToGo == 1L) "tomorrow" else "in ${timing.daysToGo} days"}",
                        "Check that everything is confirmed for $who."
                    ))
                }
                else -> Unit
            }

            else -> Unit
        }
    }

    // New requests nobody has opened for over 2 days: one combined reminder.
    val stale = incoming
        .filter { it.share.status == ShareStatus.SENT && nowMillis - it.share.sentAt > STALE_REQUEST_MILLIS }
        .sortedBy { it.share.sentAt }
    if (stale.isNotEmpty()) {
        val oldest = stale.first()
        alerts.add(AgentAlert(
            oldest.share.id, AlertLevel.REMINDER,
            if (stale.size == 1) "1 request waiting over 2 days" else "${stale.size} requests waiting over 2 days",
            "Oldest: ${oldest.tripDestination} from ${oldest.travellerName}."
        ))
    }

    return alerts.sortedBy { it.level.ordinal }
}
