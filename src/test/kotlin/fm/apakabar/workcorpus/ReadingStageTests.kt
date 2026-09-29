package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ReadingStageTests {
    private fun piece(
        number: Int,
        lines: Int,
        cuts: Map<String, List<Int>> = emptyMap(),
    ): Piece =
        Piece(
            number = number,
            title = "Piece $number",
            lines = (1..lines).map { "line $it" },
            cutSizes = cuts,
        )

    @Test
    fun `every stage covers the whole piece, in order, with nothing dropped`() {
        val pieces =
            listOf(
                piece(1, lines = 14, cuts = mapOf("block" to listOf(4, 4, 4, 2))),
                piece(99, lines = 15, cuts = mapOf("block" to listOf(5, 4, 4, 2))),
                piece(126, lines = 12, cuts = mapOf("block" to listOf(4, 4, 4))),
            )
        for (poem in pieces) {
            for (stage in ReadingStage.entries) {
                val cuts = poem.cuts(stage)
                assertEquals(0, cuts.first().first)
                assertEquals(poem.lines.size - 1, cuts.last().last)
                for ((earlier, later) in cuts.zipWithNext()) {
                    assertEquals(later.first, earlier.last + 1)
                }
            }
        }
    }

    @Test
    fun `a line is a cut of its own at the first stage`() {
        val cuts = piece(1, lines = 14).cuts(ReadingStage.LINE)
        assertEquals(14, cuts.size)
        assertTrue(cuts.all { it.count() == 1 })
    }

    @Test
    fun `a stage is cut the way the work says it is cut`() {
        assertEquals(
            listOf(4, 4, 4, 2),
            piece(1, lines = 14, cuts = mapOf("block" to listOf(4, 4, 4, 2))).cuts(ReadingStage.BLOCK).map { it.count() },
        )
        assertEquals(
            listOf(5, 4, 4, 2),
            piece(99, lines = 15, cuts = mapOf("block" to listOf(5, 4, 4, 2))).cuts(ReadingStage.BLOCK).map { it.count() },
        )
        assertEquals(
            listOf(4, 4, 4),
            piece(126, lines = 12, cuts = mapOf("block" to listOf(4, 4, 4))).cuts(ReadingStage.BLOCK).map { it.count() },
        )
    }

    @Test
    fun `a stage the work says nothing about is read line by line`() {
        assertEquals(14, piece(1, lines = 14).cuts(ReadingStage.BLOCK).size)
    }

    @Test
    fun `a work whose cuts leave lines past the last cut is refused, naming piece and stage`() {
        val short =
            HeldPiece(
                number = 1,
                title = "Piece 1",
                lines = (1..14).map { "line $it" },
                partTitle = "The work",
                partShort = null,
                partSummary = "",
                cutSizes = mapOf("block" to listOf(4, 4)),
            )
        val reading =
            HeldReading(
                untouchedBelow = 0.001,
                begunBelow = 0.5,
                mostBelow = 1.0,
                difficultWordScore = 3,
                free = listOf(1),
            )

        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 8, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                WorkCorpus.work(
                    language = "eng",
                    interiorMarks = "'’-",
                    elisions = emptyMap(),
                    pieces = listOf(short),
                    reading = reading,
                )
            },
        )
    }

    @Test
    fun `both stages are offered, and the whole piece is in each`() {
        assertEquals(listOf(ReadingStage.LINE, ReadingStage.BLOCK), ReadingStage.entries)
        for (stage in ReadingStage.entries) {
            val cuts = piece(1, lines = 14, cuts = mapOf("block" to listOf(4, 4, 4, 2))).cuts(stage)
            assertEquals(0, cuts.first().first)
            assertEquals(13, cuts.last().last)
        }
    }
}
