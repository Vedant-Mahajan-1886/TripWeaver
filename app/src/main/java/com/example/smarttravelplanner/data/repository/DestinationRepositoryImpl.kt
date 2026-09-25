package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.domain.model.ExploreDestination
import javax.inject.Inject

/**
 * Curated, offline catalog of Indian destinations for the Explore screen.
 * (Moved here from ExploreScreen.kt so the UI no longer owns its data.)
 */
class DestinationRepositoryImpl @Inject constructor() : DestinationRepository {

    override suspend fun getDestinations(): Result<List<ExploreDestination>> =
        Result.success(curatedDestinations)

    override fun getDepartureCities(): List<String> = departureCities

    private val departureCities = listOf(
        "New Delhi", "Mumbai", "Bangalore", "Chennai", "Kolkata", "Hyderabad"
    )

    private val curatedDestinations = listOf(
        ExploreDestination(
            name = "Agra",
            stateCountry = "UTTAR PRADESH · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Heritage",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_agra
        ),
        ExploreDestination(
            name = "Varanasi",
            stateCountry = "UTTAR PRADESH · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Religious",
            tagCategory = "religious",
            imageRes = R.drawable.dest_varanasi
        ),
        ExploreDestination(
            name = "Jaipur",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royalty",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jaipur
        ),
        ExploreDestination(
            name = "Udaipur",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Romantic",
            tagCategory = "nature",
            imageRes = R.drawable.dest_udaipur
        ),
        ExploreDestination(
            name = "Rishikesh",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Apr",
            tag = "Spiritual & Rafting",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_rishikesh
        ),
        ExploreDestination(
            name = "Alleppey",
            stateCountry = "KERALA · INDIA",
            idealDuration = "4–6 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Backwaters & Nature",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kerala
        ),
        ExploreDestination(
            name = "Mysore",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royal Heritage",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_mysore
        ),
        ExploreDestination(
            name = "Manali",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Oct – Jun",
            tag = "Snow & Trek",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_manali
        ),
        ExploreDestination(
            name = "Srinagar",
            stateCountry = "JAMMU & KASHMIR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Apr – Oct",
            tag = "Paradise",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kashmir
        ),
        ExploreDestination(
            name = "Goa",
            stateCountry = "GOA · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Coastal & Beach",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_goa
        ),
        ExploreDestination(
            name = "Leh",
            stateCountry = "LADAKH · INDIA",
            idealDuration = "6–8 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "High Altitude",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_leh
        ),
        ExploreDestination(
            name = "Jodhpur",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Blue City",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jodhpur
        ),
        ExploreDestination(
            name = "Amritsar",
            stateCountry = "PUNJAB · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Spiritual",
            tagCategory = "religious",
            imageRes = R.drawable.dest_amritsar
        ),
        ExploreDestination(
            name = "Munnar",
            stateCountry = "KERALA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Tea Gardens",
            tagCategory = "nature",
            imageRes = R.drawable.dest_munnar
        ),
        ExploreDestination(
            name = "Coorg",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Coffee Country",
            tagCategory = "nature",
            imageRes = R.drawable.dest_coorg
        ),
        ExploreDestination(
            name = "Darjeeling",
            stateCountry = "WEST BENGAL · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Hill Station",
            tagCategory = "nature",
            imageRes = R.drawable.dest_darjeeling
        ),
        ExploreDestination(
            name = "Gangtok",
            stateCountry = "SIKKIM · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Mountain Town",
            tagCategory = "nature",
            imageRes = R.drawable.dest_gangtok
        ),
        ExploreDestination(
            name = "Andaman",
            stateCountry = "ANDAMAN & NICOBAR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Island Paradise",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_andaman
        ),
        ExploreDestination(
            name = "Pondicherry",
            stateCountry = "PUDUCHERRY · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "French Colony",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_pondicherry
        ),
        ExploreDestination(
            name = "Mussoorie",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – Jun",
            tag = "Queen of Hills",
            tagCategory = "nature",
            imageRes = R.drawable.dest_mussoorie
        ),
        ExploreDestination(
            name = "Spiti Valley",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "7–10 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "Cold Desert",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_spiti
        ),
        ExploreDestination(
            name = "Gokarna",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Beach & Temple",
            tagCategory = "religious",
            imageRes = R.drawable.dest_gokarna
        ),
        ExploreDestination(
            name = "Shillong",
            stateCountry = "MEGHALAYA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – May",
            tag = "Scotland of East",
            tagCategory = "nature",
            imageRes = R.drawable.dest_shillong
        )
    )
}
