package com.example.smarttravelplanner.data.rag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The score labels shown on the Sources card are user-facing evidence that retrieval
 * is algorithmic, so their arithmetic and formatting are pinned down here.
 */
class ScoreLabelTest {

    private fun retrieved(lexical: Double, total: Double) = RetrievedChunk(
        chunk = KbChunk(id = "goa-costs", destination = "Goa", destinationId = "goa",
            topic = "costs", title = "Typical Goa costs", text = "...",
            sourceName = "Goa Tourism Development Corporation",
            sourceUrl = "https://goatourism.gov.in/", asOf = "2026-10"),
        lexicalScore = lexical,
        score = total
    )

    @Test
    fun relevanceLabelShowsTheFinalRankingScoreToTwoDecimals() {
        assertEquals("Relevance 4.82", retrieved(3.92, 4.82).relevanceLabel())
        assertEquals("Relevance 0.90", retrieved(0.0, 0.90).relevanceLabel())
    }

    @Test
    fun bm25LabelSeparatesTheRawScoreFromTheTopicPrior() {
        assertEquals("BM25 3.92 + topic prior 0.90", retrieved(3.92, 4.82).bm25Label())
    }

    @Test
    fun topicPriorIsTheDifferenceBetweenTotalAndLexical() {
        assertEquals(0.90, retrieved(3.92, 4.82).topicPrior, 0.0001)
    }

    @Test
    fun aPassageSelectedPurelyByPriorShowsAZeroBm25RatherThanHidingIt() {
        // This is the case worth being able to explain: no keyword matched, so BM25 is
        // zero and the passage was retrieved only because cost always matters.
        val label = retrieved(0.0, 0.90).bm25Label()
        assertTrue(label.contains("BM25 0.00"))
        assertTrue(label.contains("topic prior 0.90"))
    }

    @Test
    fun labelsAreStableForScoresThatNeedRounding() {
        assertEquals("Relevance 1.00", retrieved(0.0, 0.999).relevanceLabel())
        assertEquals("Relevance 2.35", retrieved(0.0, 2.345).relevanceLabel())
    }

    @Test
    fun scoresFromTheRealEngineCarryIntoTheLabels() {
        val corpus = listOf(
            KbChunk(id = "goa-food", destination = "Goa", destinationId = "goa", topic = "food",
                title = "Eating in Goa", text = "Fish curry rice, prawn balchao and xacuti.",
                sourceName = "Goa Tourism", sourceUrl = "https://goatourism.gov.in/", asOf = "2026-10"),
            KbChunk(id = "goa-costs", destination = "Goa", destinationId = "goa", topic = "costs",
                title = "Typical Goa costs", text = "ESTIMATE, budget travel per person per day.",
                sourceName = "Goa Tourism", sourceUrl = "https://goatourism.gov.in/", asOf = "2026-10",
                costBasis = "ESTIMATE - test")
        )
        val hits = RetrievalEngine.retrieve(corpus, "Goa", listOf("food"), "xacuti and fish curry", topK = 2)
        val food = hits.first { it.chunk.id == "goa-food" }

        assertTrue("A keyword match must produce a non-zero BM25", food.lexicalScore > 0.0)
        assertTrue(food.relevanceLabel().startsWith("Relevance "))
        assertTrue(food.bm25Label().startsWith("BM25 "))
        assertEquals(food.score - food.lexicalScore, food.topicPrior, 0.0001)
    }
}
