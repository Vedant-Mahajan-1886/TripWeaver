package com.example.smarttravelplanner.domain.util

import com.example.smarttravelplanner.domain.model.Trip

/** What the app shows about Solo mode on Home. */
data class SoloModeState(
    /** Solo mode is on: suggestions are tuned for travelling alone. */
    val enabled: Boolean,
    /** The trips look solo but the traveller never chose: suggest turning Solo mode on. */
    val showNudge: Boolean,
    /** Guess from the traveller's trips (shown as "We noticed you usually travel solo"). */
    val detectedSolo: Boolean
)

/** Decides whether someone travels solo. Pure functions, unit tested. */
object SoloDetector {

    /** How many of the most recent trips are looked at. */
    const val RECENT_TRIPS = 3

    /** One adult, no kids. */
    fun isSoloTrip(trip: Trip): Boolean = trip.adults == 1 && trip.kids == 0

    /**
     * True when MORE than half of the latest [RECENT_TRIPS] trips are solo
     * (1 of 1, 2 of 2, 2 of 3, 3 of 3). No trips = no guess.
     */
    fun looksSolo(trips: List<Trip>): Boolean {
        val recent = trips.sortedByDescending { it.createdAt }.take(RECENT_TRIPS)
        if (recent.isEmpty()) return false
        return recent.count(::isSoloTrip) * 2 > recent.size
    }

    /**
     * The traveller's own choice ([savedChoice]) always wins.
     * Until they choose, Solo mode stays off and we only nudge if their trips look solo.
     */
    fun state(savedChoice: Boolean?, trips: List<Trip>): SoloModeState {
        val detected = looksSolo(trips)
        return SoloModeState(
            enabled = savedChoice ?: false,
            showNudge = savedChoice == null && detected,
            detectedSolo = detected
        )
    }
}
