package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
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

    @Test
    fun `a book carries the language it names`() {
        val book = Fixtures.text("book-with-listening")

        assertEquals("eng", WorkCorpus.decodeWorkFromBook(book).language)
    }

    @Test
    fun `a book that does not name its language is refused, and says so`() {
        val book = Fixtures.text("book-with-listening").replace("language: eng\n", "")

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        val error = assertFailsWith<Exception> { WorkCorpus.decodeWorkFromBook(book) }
        assertTrue("language" in error.message.orEmpty(), error.message)
    }

    @Test
    fun `a work has to name its language`() {
        assertFailsWith<WorkCorpus.WorkShapeError.UnnamedLanguage> {
            WorkCorpus.validateConfiguration(work(language = ""))
        }
    }

    @Test
    fun `cuts that cover the piece exactly are accepted`() {
        WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(4, 4, 4, 2))))
    }

    @Test
    fun `cuts shorter than the piece are refused rather than given a cut of the rest`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 8, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(4, 4))))
            },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 0, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to emptyList())))
            },
        )
    }

    @Test
    fun `a stage the work says nothing about is accepted, and read line by line`() {
        val work = work()

        WorkCorpus.validateConfiguration(work)
        assertEquals(14, work.pieces[0].cuts(ReadingStage.BLOCK).size)
    }

    @Test
    fun `a cut of no lines is refused, naming the piece and the stage`() {
        assertEquals(
            WorkCorpus.WorkShapeError.EmptyCut(piece = 1, stage = "block", size = 0),
            assertFailsWith<WorkCorpus.WorkShapeError.EmptyCut> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(4, 0, 4))))
            },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.EmptyCut(piece = 1, stage = "block", size = -2),
            assertFailsWith<WorkCorpus.WorkShapeError.EmptyCut> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(-2, 16))))
            },
        )
    }

    @Test
    fun `cuts longer than the piece are refused rather than cut short`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 16, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(4, 4, 4, 4))))
            },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = Int.MAX_VALUE, lines = 14),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("block" to listOf(Int.MAX_VALUE, 1))))
            },
        )
    }

    @Test
    fun `cuts for a stage there is no such thing as are refused rather than ignored`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsForUnknownStage(piece = 1, stage = "stanza"),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsForUnknownStage> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("stanza" to listOf(7, 7))))
            },
        )
    }

    @Test
    fun `cuts for the line stage are refused, since that stage is never cut`() {
        assertEquals(
            WorkCorpus.WorkShapeError.CutsForLineStage(piece = 1),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsForLineStage> {
                WorkCorpus.validateConfiguration(work(cuts = mapOf("line" to listOf(2, 2))))
            },
        )
    }

    @Test
    fun `what is wrong with a cut is said in words that name the piece and the stage`() {
        val error = WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 99, stage = "block", cut = 16, lines = 15)

        assertEquals("Piece 99 is cut at the block stage into 16 lines, but it has 15.", error.message)
    }

    private fun work(
        language: String = "eng",
        threshold: Int = 3,
        parts: List<Part>? = null,
        free: List<Int> = listOf(1),
        cuts: Map<String, List<Int>> = emptyMap(),
    ): Work {
        val first = Piece(number = 1, title = "Piece 1", lines = List(14) { "A line of verse," }, cutSizes = cuts)
        val pieces =
            listOf(first) +
                (2..20).map { number ->
                    Piece(number = number, title = "Piece $number", lines = listOf("A line of verse,"))
                }
        return Work(
            language = language,
            pieces = pieces,
            parts = parts ?: listOf(Part(title = "The work", summary = "", first = 1, last = pieces.size)),
            free = free,
            stageField = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0),
            difficultWords = DifficultWordsConfiguration(scoreThreshold = threshold),
        )
    }
}
