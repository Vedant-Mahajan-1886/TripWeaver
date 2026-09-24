package com.example.smarttravelplanner.data.remote.ai

import com.example.smarttravelplanner.data.remote.dto.GeneratedItineraryDto
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Robust JSON parser with auto-repair capability for AI-generated itineraries.
 */
object ItineraryJsonParser {

    fun parse(rawResponse: String, modelName: String, gson: Gson): GeneratedItineraryDto {
        // 1. Remove <think>...</think> block if model emitted reasoning tags
        val noThinking = rawResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()

        // 2. Extract JSON object from first '{' to last '}'
        val firstBrace = noThinking.indexOf('{')
        val lastBrace  = noThinking.lastIndexOf('}')
        val cleaned = if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            noThinking.substring(firstBrace, lastBrace + 1)
        } else {
            noThinking.removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        }

        if (cleaned.isBlank()) {
            throw IllegalStateException("AI model '$modelName' returned an empty response. Please try again.")
        }

        try {
            val dto = tryParseOrRepairItineraryJson(cleaned, gson)
            if (dto.days.isNullOrEmpty()) {
                throw IllegalStateException("AI returned an itinerary with 0 days — please try again.")
            }
            return dto
        } catch (e: JsonSyntaxException) {
            throw IllegalStateException("AI response could not be parsed as JSON: ${e.message}")
        }
    }

    private fun tryParseOrRepairItineraryJson(jsonStr: String, gson: Gson): GeneratedItineraryDto {
        // Attempt 1: Direct standard parse
        try {
            val dto = gson.fromJson(jsonStr, GeneratedItineraryDto::class.java)
            if (dto != null && !dto.days.isNullOrEmpty()) return dto
        } catch (_: Exception) { }

        // Attempt 2: If truncated (EOFException / unclosed object/array), auto-close braces
        try {
            var candidate = jsonStr.trim()
            val quoteCount = candidate.count { it == '"' }
            if (quoteCount % 2 != 0) {
                val lastQuote = candidate.lastIndexOf('"')
                if (lastQuote != -1) {
                    candidate = candidate.substring(0, lastQuote)
                }
            }
            candidate = candidate.trim().trimEnd(',', ':', '{', '[')

            var openBraces = candidate.count { it == '{' } - candidate.count { it == '}' }
            var openBrackets = candidate.count { it == '[' } - candidate.count { it == ']' }

            val sb = StringBuilder(candidate)
            while (openBrackets > 0) {
                sb.append("]")
                openBrackets--
            }
            while (openBraces > 0) {
                sb.append("}")
                openBraces--
            }

            val repaired = sb.toString()
            val dto = gson.fromJson(repaired, GeneratedItineraryDto::class.java)
            if (dto != null && !dto.days.isNullOrEmpty()) return dto
        } catch (_: Exception) { }

        // Attempt 3: Slice to last complete day object
        try {
            val lastDayEnd = jsonStr.lastIndexOf("},")
            if (lastDayEnd != -1) {
                val candidate = jsonStr.substring(0, lastDayEnd + 1).trim() + "]}"
                val dto = gson.fromJson(candidate, GeneratedItineraryDto::class.java)
                if (dto != null && !dto.days.isNullOrEmpty()) return dto
            }
        } catch (_: Exception) { }

        // Final attempt: rethrow standard parse exception so user gets detailed syntax error
        return gson.fromJson(jsonStr, GeneratedItineraryDto::class.java)
            ?: throw IllegalStateException("AI model returned an empty or invalid JSON structure.")
    }
}
