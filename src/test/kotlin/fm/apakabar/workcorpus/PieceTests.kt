package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PieceTests {
    private fun piece(cuts: Map<String, List<Int>>): Piece =
        Piece(number = 1, title = "Piece 1", lines = List(14) { "A line of verse," }, cutSizes = cuts)

    @Test
    fun `cuts that cover the piece exactly are accepted`() {
        assertEquals(mapOf("block" to listOf(4, 4, 4, 2)), piece(mapOf("block" to listOf(4, 4, 4, 2))).cutSizes)
    }

    @Test
    fun `a stage the piece says nothing about is accepted, and read line by line`() {
        assertEquals(14, piece(emptyMap()).cuts(ReadingStage.BLOCK).size)
    }

    @Test
    fun `a piece whose cuts leave lines past the last cut is refused, naming piece and stage`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 8, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { piece(mapOf("block" to listOf(4, 4))) },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 0, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { piece(mapOf("block" to emptyList())) },
        )
    }

    @Test
    fun `a piece whose cuts run past its lines is refused rather than cut short`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 16, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                piece(mapOf("block" to listOf(4, 4, 4, 4)))
            },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = Int.MAX_VALUE, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                piece(mapOf("block" to listOf(Int.MAX_VALUE, 1)))
            },
        )
    }

    @Test
    fun `a cut of no lines is refused, naming the piece and the stage`() {
        assertEquals(
            WorkCorpus.WorkShapeError.EmptyCut(piece = 1, stage = "block", size = 0),
            assertFailsWith<WorkCorpus.WorkShapeError.EmptyCut> { piece(mapOf("block" to listOf(4, 0, 4))) },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.EmptyCut(piece = 1, stage = "block", size = -2),
            assertFailsWith<WorkCorpus.WorkShapeError.EmptyCut> { piece(mapOf("block" to listOf(-2, 16))) },
        )
    }

    @Test
    fun `cuts for a stage there is no such thing as are refused rather than ignored`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsForUnknownStage(piece = 1, stage = "stanza"),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsForUnknownStage> { piece(mapOf("stanza" to listOf(7, 7))) },
        )
    }

    @Test
    fun `cuts for the line stage are refused, since that stage is never cut`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsForLineStage(piece = 1),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsForLineStage> { piece(mapOf("line" to listOf(2, 2))) },
        )
    }

    @Test
    fun `what is wrong with a cut is said in words that name the piece and the stage`() {
        val error = WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 99, stage = "block", cut = 16, lines = 15)

        assertEquals("Piece 99 is cut at the block stage into 16 lines, but it has 15.", error.message)
    }
}
