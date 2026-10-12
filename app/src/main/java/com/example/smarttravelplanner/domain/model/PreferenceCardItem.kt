package com.example.smarttravelplanner.domain.model

import androidx.annotation.DrawableRes
import com.example.smarttravelplanner.R

/**
 * Action triggered by user swipe gestures or bottom action buttons.
 */
enum class CardAction(val weight: Float) {
    /** Swipe left or X button: not interested (weight: -1.0). */
    DISLIKE(-1.0f),

    /** Swipe right or Check button: interested (weight: +1.0). */
    LIKE(1.0f),

    /** Double-tap or Heart button: strongly preferred (weight: +2.0). */
    STRONG_PREFERENCE(2.0f)
}

/**
 * Immutable historical record of a card decision, used to support Undo.
 */
data class PreferenceDecision(
    val cardId: String,
    val action: CardAction,
    val cardIndex: Int,
    val previousWeight: Float? = null,
    val previouslyLiked: Boolean = false,
    val previouslyStrong: Boolean = false,
    val previouslyDisliked: Boolean = false
)

/**
 * Representation of an individual travel-preference card in the onboarding deck.
 */
data class PreferenceCardItem(
    val id: String,
    val title: String,
    val category: String,
    val description: String,
    @param:DrawableRes val imageRes: Int
)

/**
 * Standard 12 broad-interest cards defining travel styles, experiences, and exploration ways.
 */
object DefaultPreferenceDeck {
    val cards: List<PreferenceCardItem> = listOf(
        // ── Travel style ──────────────────────────────────────────────────────
        PreferenceCardItem(
            id = "adventure",
            title = "Adventure",
            category = "Travel Style",
            description = "High-octane outdoor pursuits, river rafting, rugged trails, and exhilarating challenges.",
            imageRes = R.drawable.pref_adventure
        ),
        PreferenceCardItem(
            id = "relaxation",
            title = "Relaxation",
            category = "Travel Style",
            description = "Tranquil coastal havens, soothing wellness retreats, quiet waters, and leisurely paces.",
            imageRes = R.drawable.pref_relaxation
        ),
        PreferenceCardItem(
            id = "nature_wildlife",
            title = "Nature & Wildlife",
            category = "Travel Style",
            description = "Protected national parks, wildlife safaris, dense jungle canopies, and lush greenery.",
            imageRes = R.drawable.pref_nature_wildlife
        ),
        PreferenceCardItem(
            id = "culture_heritage",
            title = "Culture & Heritage",
            category = "Travel Style",
            description = "Magnificent royal forts, age-old artisan traditions, vibrant festivals, and folk music.",
            imageRes = R.drawable.dest_jaipur
        ),

        // ── Interests and experiences ─────────────────────────────────────────
        PreferenceCardItem(
            id = "food_cafes",
            title = "Food & Cafés",
            category = "Interests & Experiences",
            description = "Authentic street food stalls, aromatic spice markets, artisanal cafés, and culinary tours.",
            imageRes = R.drawable.pref_food_cafes
        ),
        PreferenceCardItem(
            id = "history_architecture",
            title = "History & Architecture",
            category = "Interests & Experiences",
            description = "World wonder landmarks, monumental palaces, ancient ruins, and classical stone architecture.",
            imageRes = R.drawable.dest_agra
        ),
        PreferenceCardItem(
            id = "shopping_city_life",
            title = "Shopping & City Life",
            category = "Interests & Experiences",
            description = "Bustling flea markets, scenic promenades, craft bazaars, and vibrant evening cityscapes.",
            imageRes = R.drawable.pref_city_shopping
        ),
        PreferenceCardItem(
            id = "photography_scenic",
            title = "Photography & Scenic Views",
            category = "Interests & Experiences",
            description = "Golden-hour valley panoramas, dramatic cliff edges, alpine lakes, and postcard vistas.",
            imageRes = R.drawable.dest_kashmir
        ),

        // ── Ways to explore ───────────────────────────────────────────────────
        PreferenceCardItem(
            id = "road_trips",
            title = "Road Trips",
            category = "Ways to Explore",
            description = "Expansive open highways, thrilling high mountain passes, and spontaneous scenic stops.",
            imageRes = R.drawable.pref_road_trips
        ),
        PreferenceCardItem(
            id = "beaches_islands",
            title = "Beaches & Islands",
            category = "Ways to Explore",
            description = "Sun-drenched sands, turquoise waves, coastal sunset walks, and tropical island hopping.",
            imageRes = R.drawable.pref_beaches_islands
        ),
        PreferenceCardItem(
            id = "mountains_trekking",
            title = "Mountains & Trekking",
            category = "Ways to Explore",
            description = "Summit ridge hikes, crisp Himalayan air, cascading waterfalls, and pine-clad slopes.",
            imageRes = R.drawable.pref_mountains_trekking
        ),
        PreferenceCardItem(
            id = "local_offbeat",
            title = "Local & Offbeat Experiences",
            category = "Ways to Explore",
            description = "Living root bridges, secluded mountain hamlets, native folklore, and unexplored paths.",
            imageRes = R.drawable.pref_local_offbeat
        )
    )
}
