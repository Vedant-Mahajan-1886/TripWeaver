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
            description = "Home of the Taj Mahal, Agra Fort and nearby Fatehpur Sikri — the heart of Mughal architecture.",
            stateCountry = "UTTAR PRADESH · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Heritage",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_agra
        ),
        ExploreDestination(
            name = "Varanasi",
            description = "One of the world's oldest living cities, famous for its ghats on the Ganges and the evening Ganga Aarti.",
            stateCountry = "UTTAR PRADESH · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Religious",
            tagCategory = "religious",
            imageRes = R.drawable.dest_varanasi
        ),
        ExploreDestination(
            name = "Jaipur",
            description = "The Pink City — Amber Fort, Hawa Mahal, City Palace and bustling bazaars full of textiles and gems.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royalty",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jaipur
        ),
        ExploreDestination(
            name = "Udaipur",
            description = "The City of Lakes, with palaces rising from Lake Pichola and some of Rajasthan's most romantic sunsets.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Romantic",
            tagCategory = "nature",
            imageRes = R.drawable.dest_udaipur
        ),
        ExploreDestination(
            name = "Rishikesh",
            description = "Yoga capital of the world on the Ganges, with riverside ashrams, suspension bridges and white-water rafting.",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Apr",
            tag = "Spiritual & Rafting",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_rishikesh
        ),
        ExploreDestination(
            name = "Alleppey",
            description = "Kerala's backwater hub — drift through palm-lined canals on a traditional houseboat.",
            stateCountry = "KERALA · INDIA",
            idealDuration = "4–6 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Backwaters & Nature",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kerala
        ),
        ExploreDestination(
            name = "Mysore",
            description = "A royal city known for the illuminated Mysore Palace, Chamundi Hills and its famous silk and sandalwood.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royal Heritage",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_mysore
        ),
        ExploreDestination(
            name = "Manali",
            description = "A Himalayan getaway for snow, apple orchards, Solang Valley adventures and the road to Rohtang Pass.",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Oct – Jun",
            tag = "Snow & Trek",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_manali
        ),
        ExploreDestination(
            name = "Srinagar",
            description = "Shikara rides and houseboats on Dal Lake, Mughal gardens and snow-capped mountain views.",
            stateCountry = "JAMMU & KASHMIR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Apr – Oct",
            tag = "Paradise",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kashmir
        ),
        ExploreDestination(
            name = "Goa",
            description = "Beaches, Portuguese-era churches, spice plantations and a laid-back coastal food scene.",
            stateCountry = "GOA · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Coastal & Beach",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_goa
        ),
        ExploreDestination(
            name = "Leh",
            description = "High-altitude Ladakh — monasteries, Pangong Lake, Khardung La and dramatic desert mountains.",
            stateCountry = "LADAKH · INDIA",
            idealDuration = "6–8 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "High Altitude",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_leh
        ),
        ExploreDestination(
            name = "Jodhpur",
            description = "The Blue City, watched over by the massive Mehrangarh Fort on the edge of the Thar Desert.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Blue City",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jodhpur
        ),
        ExploreDestination(
            name = "Amritsar",
            description = "Home of the Golden Temple and its langar, plus the Wagah Border ceremony and legendary Punjabi food.",
            stateCountry = "PUNJAB · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Spiritual",
            tagCategory = "religious",
            imageRes = R.drawable.dest_amritsar
        ),
        ExploreDestination(
            name = "Munnar",
            description = "Rolling tea estates, misty hills and the Eravikulam National Park in the Western Ghats.",
            stateCountry = "KERALA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Tea Gardens",
            tagCategory = "nature",
            imageRes = R.drawable.dest_munnar
        ),
        ExploreDestination(
            name = "Coorg",
            description = "Coffee plantations, waterfalls and cool, green hills — often called the Scotland of India.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Coffee Country",
            tagCategory = "nature",
            imageRes = R.drawable.dest_coorg
        ),
        ExploreDestination(
            name = "Darjeeling",
            description = "Toy-train rides, tea gardens and sunrise views of Kanchenjunga from Tiger Hill.",
            stateCountry = "WEST BENGAL · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Hill Station",
            tagCategory = "nature",
            imageRes = R.drawable.dest_darjeeling
        ),
        ExploreDestination(
            name = "Gangtok",
            description = "Sikkim's hillside capital, with monasteries, mountain views and trips to Tsomgo Lake and Nathula.",
            stateCountry = "SIKKIM · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Mountain Town",
            tagCategory = "nature",
            imageRes = R.drawable.dest_gangtok
        ),
        ExploreDestination(
            name = "Andaman",
            description = "Turquoise water, coral reefs and white-sand beaches like Radhanagar — great for snorkelling and diving.",
            stateCountry = "ANDAMAN & NICOBAR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Island Paradise",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_andaman
        ),
        ExploreDestination(
            name = "Pondicherry",
            description = "French-quarter streets, seaside promenades, cafés and the nearby community of Auroville.",
            stateCountry = "PUDUCHERRY · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "French Colony",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_pondicherry
        ),
        ExploreDestination(
            name = "Mussoorie",
            description = "A classic hill station with Mall Road strolls, Kempty Falls and views over the Doon Valley.",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – Jun",
            tag = "Queen of Hills",
            tagCategory = "nature",
            imageRes = R.drawable.dest_mussoorie
        ),
        ExploreDestination(
            name = "Spiti Valley",
            description = "A remote cold desert of ancient monasteries, tiny villages and stark high-altitude landscapes.",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "7–10 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "Cold Desert",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_spiti
        ),
        ExploreDestination(
            name = "Gokarna",
            description = "A temple town with quieter beaches than Goa, including Om Beach and scenic coastal treks.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Beach & Temple",
            tagCategory = "religious",
            imageRes = R.drawable.dest_gokarna
        ),
        ExploreDestination(
            name = "Shillong",
            description = "Meghalaya's capital, gateway to living root bridges, waterfalls and some of the wettest places on Earth.",
            stateCountry = "MEGHALAYA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – May",
            tag = "Scotland of East",
            tagCategory = "nature",
            imageRes = R.drawable.dest_shillong
        )
    )
}
