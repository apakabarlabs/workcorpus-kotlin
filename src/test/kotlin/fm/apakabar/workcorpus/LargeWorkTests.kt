package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds
import kotlin.time.measureTimedValue

class LargeWorkTests {
    private fun book(repeatingInLast: Boolean): String =
        buildString {
            append("language: eng\npieces:\n")
            for (number in 1..PIECES) {
                append("  - number: $number\n    title: Piece $number\n")
                if (repeatingInLast && number == PIECES) append("    title: Again\n")
                append("    lines: [A line of verse.]\n")
            }
            append("parts:\n  - title: All\n    summary: Every piece.\n    first: 1\n")
            append("    last: $PIECES\n")
            append("free: [1]\n")
            append("stage_field:\n  untouched_below: 0.001\n  begun_below: 0.5\n  most_below: 1.0\n")
            append("difficult_words:\n  score_threshold: 3\n")
        }

    @Test
    fun `a large book with a key repeated near its end is refused within the budget`() {
        val book = book(repeatingInLast = true)

        val (refusal, elapsed) =
            measureTimedValue {
                assertFailsWith<WorkCorpus.WorkShapeError.RepeatedKey> { WorkCorpus.decodeWorkFromBook(book) }
            }
        assertEquals(WorkCorpus.WorkShapeError.RepeatedKey("title"), refusal)
        assertTrue(elapsed < BUDGET, "took $elapsed")
    }

    @Test
    fun `a large book without a repeat is read within the budget`() {
        val book = book(repeatingInLast = false)

        val (work, elapsed) = measureTimedValue { WorkCorpus.decodeWorkFromBook(book) }
        assertEquals(PIECES, work.pieces.size)
        assertTrue(elapsed < BUDGET, "took $elapsed")
    }

    @Test
    fun `a book of more code points than kaml reads by default is read`() {
        val line = "A line of verse, ".repeat(40)
        val book = book(repeatingInLast = false).replace("[A line of verse.]", "['$line']")

        assertTrue(book.codePointCount(0, book.length) > DEFAULT_CODE_POINT_LIMIT, "only ${book.length}")
        assertEquals(
            line,
            WorkCorpus
                .decodeWorkFromBook(book)
                .pieces
                .last()
                .lines
                .single(),
        )
    }

    private companion object {
        const val DEFAULT_CODE_POINT_LIMIT = 3_145_728
        const val PIECES = 5000
        val BUDGET = 5.seconds
    }
}
