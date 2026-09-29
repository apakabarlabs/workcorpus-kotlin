package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WorkTests {
    @Test
    fun `configuration is validated from the YAML model itself`() {
        WorkCorpus.validateConfiguration(work())
    }

    @Test
    fun `a book that still carries listening limits is read as before`() {
        val book = Fixtures.text("book-with-listening")

        assertEquals(listOf("First poem"), WorkCorpus.decodeWorkFromBook(book).pieces.map { it.title })
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "listening:\n  shortest_attempt_seconds: 0\n",
            "listening:\n  shortest_attempt_seconds: 200\n",
            "listening: {}\n",
            "listening: quick\n",
            "",
        ],
    )
    fun `a book is read whatever it says about listening, or without it`(listening: String) {
        val book =
            Fixtures.text("book-with-listening").replace(
                "listening:\n  shortest_attempt_seconds: 0.2\n",
                listening,
            )

        assertTrue(book.endsWith("score_threshold: 3\n$listening"))
        assertEquals(listOf("First poem"), WorkCorpus.decodeWorkFromBook(book).pieces.map { it.title })
    }

    @Test
    fun `difficult words need a positive score threshold`() {
        assertFailsWith<WorkCorpus.WorkShapeError.InvalidDifficultWordThreshold> {
            WorkCorpus.validateConfiguration(work(threshold = 0))
        }
    }

    @Test
    fun `parts have to cover the whole work`() {
        val parts =
            listOf(
                Part(title = "First", summary = "", first = 1, last = 10),
                Part(title = "Second", summary = "", first = 12, last = 20),
            )
        assertFailsWith<WorkCorpus.WorkShapeError.PartsDoNotCoverTheWork> {
            WorkCorpus.validateConfiguration(work(parts = parts))
        }
    }

    @Test
    fun `the pieces free to read have to be unique members of the work`() {
        assertFailsWith<WorkCorpus.WorkShapeError.InvalidFreePieces> {
            WorkCorpus.validateConfiguration(work(free = listOf(1, 1)))
        }
    }

    private fun work(
        threshold: Int = 3,
        parts: List<Part>? = null,
        free: List<Int> = listOf(1),
    ): Work {
        val pieces =
            (1..20).map { number ->
                Piece(number = number, title = "Piece $number", lines = listOf("A line of verse,"))
            }
        return Work(
            pieces = pieces,
            parts = parts ?: listOf(Part(title = "The work", summary = "", first = 1, last = pieces.size)),
            free = free,
            stageField = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0),
            difficultWords = DifficultWordsConfiguration(scoreThreshold = threshold),
        )
    }
}
