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

    private val writing = Writing(language = "eng", interiorMarks = "'’-", elisions = emptyMap())

    @Test
    fun `the pieces come out numbered and in the order they are held`() {
        val work = assemble(writing, pieces, reading)

        assertEquals(listOf(1, 2, 3), work.pieces.map { it.number })
        assertEquals(listOf("When forty winters shall besiege thy brow,"), work.pieces[1].lines)
        assertEquals("Sonnet 2", work.pieces[1].title)
    }

    @Test
    fun `a part covers the pieces filed under it and keeps its two names`() {
        val work = assemble(writing, pieces, reading)

        assertEquals(listOf("The Procreation Sonnets", "The Fair Youth"), work.parts.map { it.title })
        assertEquals("The Procreation", work.parts[0].shortTitle)
        assertEquals(1..2, work.parts[0].pieces)
        assertEquals("The Fair Youth", work.parts[1].shortTitle)
        assertEquals(3..3, work.parts[1].pieces)
    }

    @Test
    fun `the reading thresholds come off the reading it was given`() {
        val work = assemble(writing, pieces, reading)

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

        val work = assemble(writing, returning, reading)

        assertEquals(listOf(1..2, 3..3, 4..4), work.parts.map { it.pieces })
    }

    @Test
    fun `a work whose pieces are out of order is refused`() {
        val outOfOrder = listOf(pieces[1], pieces[0], pieces[2])

        val error =
            assertFailsWith<WorkCorpus.CorpusError> {
                WorkCorpus.work(
                    language = "eng",
                    interiorMarks = "'’-",
                    elisions = emptyMap(),
                    pieces = outOfOrder,
                    reading = reading,
                )
            }
        assertEquals(WorkCorpus.CorpusError.OutOfOrder(expected = 1, found = 2), error)
    }

    @Test
    fun `the language is the one the work was held with, whatever it is`() {
        val work =
            WorkCorpus.work(
                language = "srp",
                interiorMarks = "-",
                elisions = emptyMap(),
                pieces = pieces,
                reading = reading,
            )

        assertEquals("srp", work.language)
    }

    @Test
    fun `the marks inside a word and the elisions are the ones the work was held with`() {
        val work =
            WorkCorpus.work(
                language = "eng",
                interiorMarks = "'’-",
                elisions = mapOf("tatter’d" to listOf("tattered"), "th’" to listOf("the", "thee")),
                pieces = pieces,
                reading = reading,
            )

        assertEquals("'’-", work.interiorMarks)
        assertEquals(mapOf("tatter’d" to listOf("tattered"), "th’" to listOf("the", "thee")), work.elisions)
    }

    @Test
    fun `a work held with no marks inside a word and no elisions is read as such`() {
        val work =
            WorkCorpus.work(
                language = "eng",
                interiorMarks = "",
                elisions = emptyMap(),
                pieces = pieces,
                reading = reading,
            )

        assertEquals("", work.interiorMarks)
        assertEquals(emptyMap(), work.elisions)
    }

    @Test
    fun `a work held without naming its language is refused`() {
        assertEquals(
            WorkCorpus.WorkShapeError.InvalidLanguage(" "),
            assertFailsWith<WorkCorpus.WorkShapeError.InvalidLanguage> {
                WorkCorpus.work(
                    language = " ",
                    interiorMarks = "'’-",
                    elisions = emptyMap(),
                    pieces = pieces,
                    reading = reading,
                )
            },
        )
    }
}
