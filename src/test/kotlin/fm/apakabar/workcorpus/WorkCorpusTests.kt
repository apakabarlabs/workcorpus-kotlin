package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WorkCorpusTests {
    private fun pieces(count: Int): List<Piece> =
        (1L..count).map { number ->
            Piece(number = number, title = "Piece $number", lines = listOf("A line of verse,"))
        }

    @Test
    fun `pieces numbered from one and in order are accepted`() {
        WorkCorpus.validate(pieces(3))
    }

    @Test
    fun `pieces out of order are refused`() {
        val out = pieces(3).toMutableList()
        out[0] = out[1].also { out[1] = out[0] }

        val error = assertFailsWith<WorkCorpus.CorpusError> { WorkCorpus.validate(out) }
        assertEquals(WorkCorpus.CorpusError.OutOfOrder(expected = 1, found = 2), error)
        assertEquals("Expected piece 1, found 2.", error.message)
    }

    @Test
    fun `a work of any length is accepted - how many pieces it has is its own business`() {
        WorkCorpus.validate(pieces(1))
        WorkCorpus.validate(pieces(400))
    }

    @Test
    fun `a piece of any length is accepted - how many lines it has is its own business`() {
        val uneven =
            listOf(
                Piece(number = 1, title = "One", lines = listOf("A single line.")),
                Piece(number = 2, title = "Two", lines = List(30) { "A line of verse," }),
            )

        WorkCorpus.validate(uneven)
    }

    @Test
    fun `a piece carries the title the work gave it, and its opening line`() {
        val piece =
            Piece(
                number = 18,
                title = "Sonnet 18",
                lines = listOf("Shall I compare thee to a summer’s day?", "Thou art more lovely"),
            )

        assertEquals("Sonnet 18", piece.title)
        assertEquals("Shall I compare thee to a summer’s day?", piece.openingLine)
    }
}
