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
        val book = workYaml.decodeFromString(Work.serializer(), Fixtures.text("book-with-listening"))

        WorkCorpus.validateConfiguration(book)
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
    fun `a book whose piece is cut short of its lines is refused, naming piece and stage`() {
        val book =
            Fixtures.text("book-with-listening").replace(
                "    lines: [The first line.]\n",
                "    lines: [The first line., The second line.]\n    cuts:\n      block: [1]\n",
            )

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = 1, lines = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { WorkCorpus.decodeWorkFromBook(book) },
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\u200B", "\u0085", "eng\n", "EN", "e", "english", "en-", "en-GB-"])
    fun `a language that is not a language tag is refused, naming the value`(language: String) {
        assertEquals(
            WorkCorpus.WorkShapeError.InvalidLanguage(language),
            assertFailsWith<WorkCorpus.WorkShapeError.InvalidLanguage> {
                WorkCorpus.validateConfiguration(work(language = language))
            },
        )
    }

    @ParameterizedTest
    @ValueSource(strings = ["en", "eng", "srp", "en-GB", "sr-Latn-RS", "zh-Hant", "es-419"])
    fun `a language tag of two or three letters and its subtags is accepted`(language: String) {
        WorkCorpus.validateConfiguration(work(language = language))
    }

    @Test
    fun `what is wrong with a language is said with the value written out`() {
        assertEquals(
            "The work names its language as \"\\u{200B}\", which is not a language tag such as en, eng or en-GB.",
            WorkCorpus.WorkShapeError.InvalidLanguage("\u200B").message,
        )
        assertEquals(
            "The work names its language as \"\\u{85}en\", which is not a language tag such as en, eng or en-GB.",
            WorkCorpus.WorkShapeError.InvalidLanguage("\u0085en").message,
        )
    }

    @Test
    fun `a book piece whose cuts are null is read as having none`() {
        val book =
            Fixtures.text("book-with-listening").replace(
                "    lines: [The first line.]\n",
                "    lines: [The first line.]\n    cuts: null\n",
            )

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        assertEquals(emptyMap(), WorkCorpus.decodeWorkFromBook(book).pieces[0].cutSizes)
    }

    @Test
    fun `a cut too large to count is refused naming the piece and the stage`() {
        val book =
            Fixtures.text("book-with-listening").replace(
                "    lines: [The first line.]\n",
                "    lines: [The first line.]\n    cuts:\n      block: [99999999999999999999]\n",
            )

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        assertEquals(
            WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece(piece = 1, stage = "block", cut = Long.MAX_VALUE, lines = 1),
            assertFailsWith<WorkCorpus.WorkShapeError.CutsDoNotCoverThePiece> { WorkCorpus.decodeWorkFromBook(book) },
        )
    }

    private fun work(
        language: String = "eng",
        threshold: Int = 3,
        parts: List<Part>? = null,
        free: List<Long> = listOf(1),
    ): Work {
        val pieces =
            (1L..20L).map { number ->
                Piece(number = number, title = "Piece $number", lines = listOf("A line of verse,"))
            }
        return Work(
            language = language,
            pieces = pieces,
            parts = parts ?: listOf(Part(title = "The work", summary = "", first = 1, last = pieces.size.toLong())),
            free = free,
            stageField = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0),
            difficultWords = DifficultWordsConfiguration(scoreThreshold = threshold),
        )
    }
}
