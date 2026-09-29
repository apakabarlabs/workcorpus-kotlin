package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WorkFileTests {
    @Test
    fun `the pieces come out of the work file numbered and in order`() {
        val read = assembleWork(Fixtures.work())

        assertEquals(listOf(1, 2, 3), read.pieces.map { it.number })
        assertEquals(listOf("When forty winters shall besiege thy brow,"), read.pieces[1].lines)
    }

    @Test
    fun `a piece carries the title the work gave it`() {
        val read = assembleWork(Fixtures.work())

        assertEquals(listOf("Sonnet 1", "Sonnet 2", "Sonnet 3"), read.pieces.map { it.title })
    }

    @Test
    fun `a piece is cut the way the work file says it is cut`() {
        val read = assembleWork(Fixtures.work())

        assertEquals(listOf(2), read.pieces[0].cuts(ReadingStage.BLOCK).map { it.count() })
        assertEquals(listOf(1), read.pieces[1].cuts(ReadingStage.BLOCK).map { it.count() })
    }

    @Test
    fun `a part covers the pieces under it and keeps its two names`() {
        val read = assembleWork(Fixtures.work())

        assertEquals(listOf("The Procreation Sonnets", "The Fair Youth"), read.parts.map { it.title })
        assertEquals("The Procreation", read.parts[0].shortTitle)
        assertEquals(1..2, read.parts[0].pieces)
        assertEquals("The Fair Youth", read.parts[1].shortTitle)
    }

    @Test
    fun `the reading thresholds come off the reading block`() {
        val read = assembleWork(Fixtures.work())

        assertEquals(listOf(1, 2), read.free)
        assertEquals(StageFieldScale.Band.UNTOUCHED, read.stageField.band(0.0005))
        assertEquals(3, read.difficultWords.scoreThreshold)
    }

    @Test
    fun `a work file that does not name its language is refused, and says so`() {
        val unnamed = Fixtures.work().replace("language: eng\n", "")

        assertNotEquals(Fixtures.work(), unnamed)
        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWork(unnamed) }
        assertTrue("language" in error.message.orEmpty(), error.message)
    }

    @Test
    fun `a work file whose cuts overrun a piece is refused`() {
        val overrun =
            Fixtures.work().replace(
                "          block:\n            - 2\n",
                "          block:\n            - 3\n",
            )

        assertNotEquals(Fixtures.work(), overrun)
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 3, lines = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { WorkCorpus.decodeWork(overrun) },
        )
    }

    @Test
    fun `a work file whose cuts fall short of a piece is refused`() {
        val short =
            Fixtures.work().replace(
                "          block:\n            - 2\n",
                "          block:\n            - 1\n",
            )

        assertNotEquals(Fixtures.work(), short)
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 1, lines = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { WorkCorpus.decodeWork(short) },
        )
    }

    @Test
    fun `a piece identifier past 32 bits is refused as not a number`() {
        val past = Fixtures.work().replace("id: '3'", "id: '2147483648'")

        assertEquals(
            WorkCorpus.WorkError.PieceIsNotNumbered("2147483648"),
            assertFailsWith<WorkCorpus.WorkError> { assembleWork(past) },
        )
    }

    @Test
    fun `a piece that is not numbered is refused rather than renumbered`() {
        val prose = Fixtures.work().replace("id: '3'", "id: prologue")

        val error = assertFailsWith<WorkCorpus.WorkError> { assembleWork(prose) }
        assertEquals(WorkCorpus.WorkError.PieceIsNotNumbered("prologue"), error)
    }

    @Test
    fun `a piece without a title is refused, and the refusal names what is missing`() {
        val untitled = Fixtures.work().replace("        title: Sonnet 2\n", "")

        assertNotEquals(Fixtures.work(), untitled)
        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWork(untitled) }
        assertTrue("title" in error.message.orEmpty(), error.message)
    }

    @Test
    fun `a work file whose pieces skip a number is refused as out of order`() {
        val skipped = Fixtures.work().replace("id: '2'", "id: '4'")

        val error = assertFailsWith<WorkCorpus.CorpusError> { WorkCorpus.decodeWork(skipped) }
        assertEquals(WorkCorpus.CorpusError.OutOfOrder(expected = 2, found = 4), error)
    }
}
