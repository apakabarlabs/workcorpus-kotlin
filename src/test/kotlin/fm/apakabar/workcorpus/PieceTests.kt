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
    fun `a piece held with cuts that do not divide its lines is refused as a decoded one is`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 8, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { piece(mapOf("block" to listOf(4, 4))) },
        )
    }
}
