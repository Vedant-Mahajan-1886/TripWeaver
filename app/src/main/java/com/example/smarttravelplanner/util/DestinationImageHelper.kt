package com.example.smarttravelplanner.util

import androidx.annotation.DrawableRes
import com.example.smarttravelplanner.R

object DestinationImageHelper {

    /**
     * Resolves the best image representation for a destination.
     * Returns either a web URL (String) if available, or a local drawable resource ID (Int).
     */
    fun getDestinationImage(destination: String, coverImageUrl: String? = null): Any {
        if (!coverImageUrl.isNullOrBlank() && (coverImageUrl.startsWith("http://") || coverImageUrl.startsWith("https://"))) {
            return coverImageUrl
        }
        return getDestinationDrawableRes(destination)
    }

    /**
     * Checks if a local bundled drawable resource exists for the given destination.
     * If false, the app should fetch a photo dynamically from the internet (e.g. Wikipedia).
     */
    fun hasLocalDrawable(destination: String): Boolean {
        return getExplicitDrawableRes(destination) != null
    }

    @DrawableRes
    fun getDestinationDrawableRes(destination: String): Int {
        return getExplicitDrawableRes(destination) ?: R.drawable.hero_mountain_bg
    }

    @DrawableRes
    fun getExplicitDrawableRes(destination: String): Int? {
        val dest = destination.lowercase().trim()
        return when {
            // Himachal Pradesh & Mountain destinations
            dest.contains("himachal") || dest.contains("manali") || dest.contains("shimla") ||
            dest.contains("kullu") || dest.contains("dharamshala") || dest.contains("kasol") ||
            dest.contains("solang") || dest.contains("rohtang") || dest.contains("dalhousie") ||
            dest.contains("chamba") || dest.contains("mandi") -> R.drawable.dest_manali

            dest.contains("spiti") || dest.contains("lahaul") || dest.contains("kaza") ||
            dest.contains("tabo") -> R.drawable.dest_spiti

            // Rajasthan
            dest.contains("jaipur") || dest.contains("rajasthan") || dest.contains("pink city") -> R.drawable.dest_jaipur
            dest.contains("udaipur") -> R.drawable.dest_udaipur
            dest.contains("jodhpur") -> R.drawable.dest_jodhpur

            // Goa & Coast
            dest.contains("goa") -> R.drawable.dest_goa
            dest.contains("gokarna") -> R.drawable.dest_gokarna

            // Kerala
            dest.contains("munnar") -> R.drawable.dest_munnar
            dest.contains("kerala") || dest.contains("alleppey") || dest.contains("kochi") ||
            dest.contains("cochin") || dest.contains("wayanad") || dest.contains("alappuzha") ||
            dest.contains("kovalam") || dest.contains("varkala") -> R.drawable.dest_kerala

            // Kashmir & Ladakh
            dest.contains("kashmir") || dest.contains("srinagar") || dest.contains("gulmarg") ||
            dest.contains("pahalgam") || dest.contains("sonamarg") -> R.drawable.dest_kashmir

            dest.contains("ladakh") || dest.contains("leh") || dest.contains("pangong") ||
            dest.contains("nubra") || dest.contains("zanskar") -> R.drawable.dest_leh

            // Uttarakhand
            dest.contains("mussoorie") -> R.drawable.dest_mussoorie
            dest.contains("rishikesh") || dest.contains("uttarakhand") || dest.contains("haridwar") ||
            dest.contains("dehradun") || dest.contains("nainital") || dest.contains("auli") ||
            dest.contains("kedarnath") || dest.contains("badrinath") -> R.drawable.dest_rishikesh

            // Uttar Pradesh
            dest.contains("agra") || dest.contains("taj") -> R.drawable.dest_agra
            dest.contains("varanasi") || dest.contains("kashi") || dest.contains("banaras") -> R.drawable.dest_varanasi

            // Punjab
            dest.contains("amritsar") || dest.contains("golden temple") || dest.contains("punjab") -> R.drawable.dest_amritsar

            // Andaman
            dest.contains("andaman") || dest.contains("nicobar") || dest.contains("havelock") ||
            dest.contains("port blair") || dest.contains("neil island") -> R.drawable.dest_andaman

            // Karnataka
            dest.contains("coorg") || dest.contains("kodagu") -> R.drawable.dest_coorg
            dest.contains("mysore") || dest.contains("mysuru") || dest.contains("bengaluru") ||
            dest.contains("bangalore") || dest.contains("karnataka") || dest.contains("hampi") -> R.drawable.dest_mysore

            // Northeast & West Bengal
            dest.contains("darjeeling") -> R.drawable.dest_darjeeling
            dest.contains("gangtok") || dest.contains("sikkim") -> R.drawable.dest_gangtok
            dest.contains("shillong") || dest.contains("meghalaya") || dest.contains("cherrapunji") -> R.drawable.dest_shillong

            // Pondicherry
            dest.contains("pondicherry") || dest.contains("puducherry") -> R.drawable.dest_pondicherry

            else -> null
        }
    }
}
