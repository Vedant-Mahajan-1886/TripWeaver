package com.example.smarttravelplanner.data.remote.ai

import android.util.Log
import com.example.smarttravelplanner.BuildConfig

/**
 * Explainability aid for TripWeaver's RAG pipeline.
 *
 * Logs the exact prompt string handed to the model, so the augmentation step can be
 * inspected rather than taken on trust. Filter Logcat by the tag `RAG_PROOF`.
 *
 * Two details that matter:
 *
 *  - Logcat drops any single entry over roughly 4 KB, and an itinerary prompt with
 *    retrieved passages is comfortably larger than that. The prompt is therefore
 *    emitted in numbered segments rather than silently truncated.
 *  - Logging is debug-only. A release build prints nothing, because a prompt contains
 *    the traveller's destination, dates and free-text notes.
 */
object RagProofLogger {

    const val TAG = "RAG_PROOF"

    /** Logcat truncates around 4 KB; stay well under it. */
    private const val SEGMENT = 3000

    /**
     * @param label which engine is about to send the prompt
     * @param prompt the final user message, retrieved context already injected
     * @param retrievedContext the injected block, used only to report its size
     */
    fun logPrompt(label: String, prompt: String, retrievedContext: String) {
        if (!BuildConfig.DEBUG) return

        val injected = retrievedContext.isNotBlank()
        Log.i(TAG, "================ FINAL PROMPT SENT TO $label ================")
        Log.i(
            TAG,
            "Retrieval: " + if (injected) {
                "ACTIVE - ${retrievedContext.length} chars of knowledge-base context injected"
            } else {
                "NONE - destination not covered by the knowledge base; prompt is unaugmented"
            }
        )
        Log.i(TAG, "Total prompt length: ${prompt.length} chars")

        val segments = (prompt.length + SEGMENT - 1) / SEGMENT
        for (i in 0 until segments) {
            val start = i * SEGMENT
            val end = minOf(start + SEGMENT, prompt.length)
            Log.i(TAG, "---- part ${i + 1}/$segments ----\n" + prompt.substring(start, end))
        }
        Log.i(TAG, "================ END OF PROMPT ================")
    }
}
