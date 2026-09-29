package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class HeldBookTests {
    private val reading =
        HeldReading(
            untouchedBelow = 0.001,
            begunBelow = 0.5,
            mostBelow = 1.0,
            difficultWordScore = 3,
            free = listOf(1, 2),
        )

    private val pieces =
        listOf(
            HeldPiece(
                number = 1,
                title = "Sonnet 1",
                lines = listOf("From fairest creatures we desire increase,"),
                partTitle = "The Procreation Sonnets",
                partShort = "The Procreation",
                partSummary = "Marry, and let your beauty outlive you.",
            ),
            HeldPiece(
                number = 2,
                title = "Sonnet 2",
                lines = listOf("When forty winters shall besiege thy brow,"),
                partTitle = "The Procreation Sonnets",
                partShort = "The Procreation",
                partSummary = "Marry, and let your beauty outlive you.",
            ),
            HeldPiece(
                number = 3,
                title = "Sonnet 3",
                lines = listOf("Look in thy glass and tell the face thou viewest"),
                partTitle = "The Fair Youth",
                partShort = null,
                partSummary = "The poet writes to the young man.",
            ),
        )

    @Test
    fun `the pieces come out numbered and in the order they are held`() {
        val work = assemble("eng", pieces, reading)

        assertEquals(listOf(1, 2, 3), work.pieces.map { it.number })
        assertEquals(listOf("When forty winters shall besiege thy brow,"), work.pieces[1].lines)
        assertEquals("Sonnet 2", work.pieces[1].title)
    }

    @Test
    fun `a part covers the pieces filed under it and keeps its two names`() {
        val work = assemble("eng", pieces, reading)

        assertEquals(listOf("The Procreation Sonnets", "The Fair Youth"), work.parts.map { it.title })
        assertEquals("The Procreation", work.parts[0].shortTitle)
        assertEquals(1..2, work.parts[0].pieces)
        assertEquals("The Fair Youth", work.parts[1].shortTitle)
        assertEquals(3..3, work.parts[1].pieces)
    }

    @Test
    fun `the reading thresholds come off the reading it was given`() {
        val work = assemble("eng", pieces, reading)

        assertEquals(listOf(1, 2), work.free)
        assertEquals(StageFieldScale.Band.UNTOUCHED, work.stageField.band(0.0005))
        assertEquals(3, work.difficultWords.scoreThreshold)
    }

    @Test
    fun `a part interrupted and taken up again is two parts, not one spanning the gap`() {
        val returning =
            pieces +
                HeldPiece(
                    number = 4,
                    title = "Sonnet 4",
                    lines = listOf("Unthrifty loveliness, why dost thou spend"),
                    partTitle = "The Procreation Sonnets",
                    partShort = "The Procreation",
                    partSummary = "Marry, and let your beauty outlive you.",
                )

        val work = assemble("eng", returning, reading)

        assertEquals(listOf(1..2, 3..3, 4..4), work.parts.map { it.pieces })
    }

    @Test
    fun `a work whose pieces are out of order is refused`() {
        val outOfOrder = listOf(pieces[1], pieces[0], pieces[2])

        val error = assertFailsWith<WorkCorpus.CorpusError> { WorkCorpus.work("eng", outOfOrder, reading) }
        assertEquals(WorkCorpus.CorpusError.OutOfOrder(expected = 1, found = 2), error)
    }

    @Test
    fun `the language is the one the work was held with, whatever it is`() {
        val work = WorkCorpus.work(language = "srp", pieces = pieces, reading = reading)

        assertEquals("srp", work.language)
    }

    @Test
    fun `a work held without naming its language is refused`() {
        assertFailsWith<WorkCorpus.WorkShapeError.UnnamedLanguage> {
            WorkCorpus.work(language = " ", pieces = pieces, reading = reading)
        }
    }
}
