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

    fun parseSingleActivity(rawResponse: String, modelName: String, gson: Gson): com.example.smarttravelplanner.data.remote.dto.ActivityDto {
        val noThinking = rawResponse.replace(Regex("<think>[\\s\\S]*?</think>"), "").trim()
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

        // 1. Inspect JSON element in case the model wrapped the activity inside an object or array
        try {
            val element = com.google.gson.JsonParser.parseString(cleaned)
            if (element.isJsonObject) {
                val obj = element.asJsonObject
                if (obj.has("activity") && obj.get("activity").isJsonObject) {
                    return gson.fromJson(obj.get("activity"), com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
                }
                if (obj.has("activities") && obj.get("activities").isJsonArray) {
                    val arr = obj.getAsJsonArray("activities")
                    if (arr.size() > 0 && arr.get(0).isJsonObject) {
                        return gson.fromJson(arr.get(0), com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
                    }
                }
                if (obj.has("days") && obj.get("days").isJsonArray) {
                    val days = obj.getAsJsonArray("days")
                    if (days.size() > 0 && days.get(0).isJsonObject) {
                        val day = days.get(0).asJsonObject
                        if (day.has("activities") && day.get("activities").isJsonArray) {
                            val acts = day.getAsJsonArray("activities")
                            if (acts.size() > 0) {
                                return gson.fromJson(acts.get(0), com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
                            }
                        }
                    }
                }
                val direct = gson.fromJson(obj, com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
                if (direct != null && (direct.title.isNotBlank() || direct.placeName.isNotBlank())) {
                    return direct
                }
            }
        } catch (_: Exception) {}

        // 2. Direct standard parse
        try {
            val direct = gson.fromJson(cleaned, com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
            if (direct != null && (direct.title.isNotBlank() || direct.placeName.isNotBlank())) {
                return direct
            }
        } catch (_: Exception) {}

        // 3. Fallback auto-repair
        try {
            var candidate = cleaned.trim().trimEnd(',', ':', '{', '[')
            val quoteCount = candidate.count { it == '"' }
            if (quoteCount % 2 != 0) {
                val lastQuote = candidate.lastIndexOf('"')
                if (lastQuote != -1) candidate = candidate.substring(0, lastQuote)
            }
            var openBraces = candidate.count { it == '{' } - candidate.count { it == '}' }
            val sb = StringBuilder(candidate)
            while (openBraces > 0) {
                sb.append("}")
                openBraces--
            }
            val repaired = gson.fromJson(sb.toString(), com.example.smarttravelplanner.data.remote.dto.ActivityDto::class.java)
            if (repaired != null) return repaired
        } catch (_: Exception) {}

        throw IllegalStateException("AI returned an unparseable activity: $cleaned")
    }
}

