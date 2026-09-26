package com.example.smarttravelplanner.data.repository

import com.example.smarttravelplanner.R
import com.example.smarttravelplanner.domain.model.DepartureCity
import com.example.smarttravelplanner.domain.model.ExploreDestination
import com.example.smarttravelplanner.domain.model.GeoPoint
import javax.inject.Inject

/**
 * Curated, offline catalog of Indian destinations for the Explore screen.
 * (Moved here from ExploreScreen.kt so the UI no longer owns its data.)
 */
class DestinationRepositoryImpl @Inject constructor() : DestinationRepository {

    override suspend fun getDestinations(): Result<List<ExploreDestination>> =
        Result.success(curatedDestinations)

    override fun getDepartureCities(): List<DepartureCity> = departureCities

    private val departureCities = listOf(
        DepartureCity("New Delhi", GeoPoint(28.6139, 77.2090)),
        DepartureCity("Mumbai",    GeoPoint(19.0760, 72.8777)),
        DepartureCity("Bangalore", GeoPoint(12.9716, 77.5946)),
        DepartureCity("Chennai",   GeoPoint(13.0827, 80.2707)),
        DepartureCity("Kolkata",   GeoPoint(22.5726, 88.3639)),
        DepartureCity("Hyderabad", GeoPoint(17.3850, 78.4867))
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
            imageRes = R.drawable.dest_agra,
            location = GeoPoint(27.1767, 78.0081)
        ),
        ExploreDestination(
            name = "Varanasi",
            description = "One of the world's oldest living cities, famous for its ghats on the Ganges and the evening Ganga Aarti.",
            stateCountry = "UTTAR PRADESH · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Religious",
            tagCategory = "religious",
            imageRes = R.drawable.dest_varanasi,
            location = GeoPoint(25.3176, 82.9739)
        ),
        ExploreDestination(
            name = "Jaipur",
            description = "The Pink City — Amber Fort, Hawa Mahal, City Palace and bustling bazaars full of textiles and gems.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royalty",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jaipur,
            location = GeoPoint(26.9124, 75.7873)
        ),
        ExploreDestination(
            name = "Udaipur",
            description = "The City of Lakes, with palaces rising from Lake Pichola and some of Rajasthan's most romantic sunsets.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Romantic",
            tagCategory = "nature",
            imageRes = R.drawable.dest_udaipur,
            location = GeoPoint(24.5854, 73.7125)
        ),
        ExploreDestination(
            name = "Rishikesh",
            description = "Yoga capital of the world on the Ganges, with riverside ashrams, suspension bridges and white-water rafting.",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Apr",
            tag = "Spiritual & Rafting",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_rishikesh,
            location = GeoPoint(30.0869, 78.2676)
        ),
        ExploreDestination(
            name = "Alleppey",
            description = "Kerala's backwater hub — drift through palm-lined canals on a traditional houseboat.",
            stateCountry = "KERALA · INDIA",
            idealDuration = "4–6 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Backwaters & Nature",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kerala,
            location = GeoPoint(9.4981, 76.3388)
        ),
        ExploreDestination(
            name = "Mysore",
            description = "A royal city known for the illuminated Mysore Palace, Chamundi Hills and its famous silk and sandalwood.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Royal Heritage",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_mysore,
            location = GeoPoint(12.2958, 76.6394)
        ),
        ExploreDestination(
            name = "Manali",
            description = "A Himalayan getaway for snow, apple orchards, Solang Valley adventures and the road to Rohtang Pass.",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Oct – Jun",
            tag = "Snow & Trek",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_manali,
            location = GeoPoint(32.2432, 77.1892)
        ),
        ExploreDestination(
            name = "Srinagar",
            description = "Shikara rides and houseboats on Dal Lake, Mughal gardens and snow-capped mountain views.",
            stateCountry = "JAMMU & KASHMIR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Apr – Oct",
            tag = "Paradise",
            tagCategory = "nature",
            imageRes = R.drawable.dest_kashmir,
            location = GeoPoint(34.0837, 74.7973)
        ),
        ExploreDestination(
            name = "Goa",
            description = "Beaches, Portuguese-era churches, spice plantations and a laid-back coastal food scene.",
            stateCountry = "GOA · INDIA",
            idealDuration = "3–5 days ideal",
            bestSeason = "Best: Nov – Feb",
            tag = "Coastal & Beach",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_goa,
            location = GeoPoint(15.2993, 74.124)
        ),
        ExploreDestination(
            name = "Leh",
            description = "High-altitude Ladakh — monasteries, Pangong Lake, Khardung La and dramatic desert mountains.",
            stateCountry = "LADAKH · INDIA",
            idealDuration = "6–8 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "High Altitude",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_leh,
            location = GeoPoint(34.1526, 77.5771)
        ),
        ExploreDestination(
            name = "Jodhpur",
            description = "The Blue City, watched over by the massive Mehrangarh Fort on the edge of the Thar Desert.",
            stateCountry = "RAJASTHAN · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Blue City",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_jodhpur,
            location = GeoPoint(26.2389, 73.0243)
        ),
        ExploreDestination(
            name = "Amritsar",
            description = "Home of the Golden Temple and its langar, plus the Wagah Border ceremony and legendary Punjabi food.",
            stateCountry = "PUNJAB · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Spiritual",
            tagCategory = "religious",
            imageRes = R.drawable.dest_amritsar,
            location = GeoPoint(31.634, 74.8723)
        ),
        ExploreDestination(
            name = "Munnar",
            description = "Rolling tea estates, misty hills and the Eravikulam National Park in the Western Ghats.",
            stateCountry = "KERALA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – Mar",
            tag = "Tea Gardens",
            tagCategory = "nature",
            imageRes = R.drawable.dest_munnar,
            location = GeoPoint(10.0889, 77.0595)
        ),
        ExploreDestination(
            name = "Coorg",
            description = "Coffee plantations, waterfalls and cool, green hills — often called the Scotland of India.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Coffee Country",
            tagCategory = "nature",
            imageRes = R.drawable.dest_coorg,
            location = GeoPoint(12.4244, 75.7382)
        ),
        ExploreDestination(
            name = "Darjeeling",
            description = "Toy-train rides, tea gardens and sunrise views of Kanchenjunga from Tiger Hill.",
            stateCountry = "WEST BENGAL · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Hill Station",
            tagCategory = "nature",
            imageRes = R.drawable.dest_darjeeling,
            location = GeoPoint(27.041, 88.2663)
        ),
        ExploreDestination(
            name = "Gangtok",
            description = "Sikkim's hillside capital, with monasteries, mountain views and trips to Tsomgo Lake and Nathula.",
            stateCountry = "SIKKIM · INDIA",
            idealDuration = "4–5 days ideal",
            bestSeason = "Best: Mar – May",
            tag = "Mountain Town",
            tagCategory = "nature",
            imageRes = R.drawable.dest_gangtok,
            location = GeoPoint(27.3389, 88.6065)
        ),
        ExploreDestination(
            name = "Andaman",
            description = "Turquoise water, coral reefs and white-sand beaches like Radhanagar — great for snorkelling and diving.",
            stateCountry = "ANDAMAN & NICOBAR · INDIA",
            idealDuration = "5–7 days ideal",
            bestSeason = "Best: Oct – May",
            tag = "Island Paradise",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_andaman,
            location = GeoPoint(11.6234, 92.7265)
        ),
        ExploreDestination(
            name = "Pondicherry",
            description = "French-quarter streets, seaside promenades, cafés and the nearby community of Auroville.",
            stateCountry = "PUDUCHERRY · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "French Colony",
            tagCategory = "cultural",
            imageRes = R.drawable.dest_pondicherry,
            location = GeoPoint(11.9416, 79.8083)
        ),
        ExploreDestination(
            name = "Mussoorie",
            description = "A classic hill station with Mall Road strolls, Kempty Falls and views over the Doon Valley.",
            stateCountry = "UTTARAKHAND · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Mar – Jun",
            tag = "Queen of Hills",
            tagCategory = "nature",
            imageRes = R.drawable.dest_mussoorie,
            location = GeoPoint(30.4598, 78.0644)
        ),
        ExploreDestination(
            name = "Spiti Valley",
            description = "A remote cold desert of ancient monasteries, tiny villages and stark high-altitude landscapes.",
            stateCountry = "HIMACHAL PRADESH · INDIA",
            idealDuration = "7–10 days ideal",
            bestSeason = "Best: Jun – Sep",
            tag = "Cold Desert",
            tagCategory = "adventure",
            imageRes = R.drawable.dest_spiti,
            location = GeoPoint(32.2276, 78.071)
        ),
        ExploreDestination(
            name = "Gokarna",
            description = "A temple town with quieter beaches than Goa, including Om Beach and scenic coastal treks.",
            stateCountry = "KARNATAKA · INDIA",
            idealDuration = "2–3 days ideal",
            bestSeason = "Best: Oct – Mar",
            tag = "Beach & Temple",
            tagCategory = "religious",
            imageRes = R.drawable.dest_gokarna,
            location = GeoPoint(14.5479, 74.3188)
        ),
        ExploreDestination(
            name = "Shillong",
            description = "Meghalaya's capital, gateway to living root bridges, waterfalls and some of the wettest places on Earth.",
            stateCountry = "MEGHALAYA · INDIA",
            idealDuration = "3–4 days ideal",
            bestSeason = "Best: Sep – May",
            tag = "Scotland of East",
            tagCategory = "nature",
            imageRes = R.drawable.dest_shillong,
            location = GeoPoint(25.5788, 91.8933)
        )
    )
}
