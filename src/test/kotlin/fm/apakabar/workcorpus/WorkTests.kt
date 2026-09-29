package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class WorkTests {
    @Test
    fun `configuration is validated from the YAML model itself`() {
        val book = Yaml.default.decodeFromString(Work.serializer(), Fixtures.text("book-with-listening"))

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
        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWorkFromBook(book) }
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

    @ParameterizedTest
    @MethodSource("invalidNumbers")
    fun `a book number that is not a YAML integer within 32 bits is refused, naming the field`(
        written: String,
        replaced: String,
        place: String,
    ) {
        val book = Fixtures.text("book-with-listening").replace(written, replaced)

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        val error = assertFailsWith<WorkCorpus.WorkShapeError.InvalidNumber> { WorkCorpus.decodeWorkFromBook(book) }
        assertEquals(WorkCorpus.WorkShapeError.InvalidNumber(place), error)
    }

    @ParameterizedTest
    @ValueSource(strings = ["010", "0x4", "0o4", "0b1", "1_0", "+1", "1:30"])
    fun `a number not written as plain decimal digits is refused, whatever YAML makes of it`(written: String) {
        val fixture = Fixtures.text("book-with-listening")
        val threshold = fixture.replace("  score_threshold: 3\n", "  score_threshold: $written\n")
        val cut = fixture.replace(LINES, "$LINES    cuts:\n      block: [$written]\n")

        assertNotEquals(fixture, threshold)
        assertNotEquals(fixture, cut)
        assertEquals(
            WorkCorpus.WorkShapeError.InvalidNumber("difficult_words.score_threshold"),
            assertFailsWith<WorkCorpus.WorkShapeError.InvalidNumber> { WorkCorpus.decodeWorkFromBook(threshold) },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.InvalidNumber("pieces[0].cuts.block[0]"),
            assertFailsWith<WorkCorpus.WorkShapeError.InvalidNumber> { WorkCorpus.decodeWorkFromBook(cut) },
        )
    }

    @Test
    fun `what is wrong with a number is said naming the field`() {
        val book = Fixtures.text("book-with-listening").replace("free: [1]\n", "free: [2147483648]\n")
        val error = assertFailsWith<WorkCorpus.WorkShapeError.InvalidNumber> { WorkCorpus.decodeWorkFromBook(book) }

        assertEquals("The work's free[0] is not a whole number that fits in 32 bits.", error.message)
    }

    @Test
    fun `a part that runs past the last piece is refused rather than overflowing`() {
        val work = work(parts = listOf(Part(title = "All", summary = "", first = 1, last = Int.MAX_VALUE)))

        assertEquals(
            WorkCorpus.WorkShapeError.PartOutOfRange(first = 1, last = Int.MAX_VALUE),
            assertFailsWith<WorkCorpus.WorkShapeError.PartOutOfRange> { WorkCorpus.validateConfiguration(work) },
        )
    }

    @Test
    fun `a part that ends before it starts, or starts before piece one, cannot be made`() {
        assertEquals(
            WorkCorpus.WorkShapeError.PartOutOfRange(first = 3, last = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.PartOutOfRange> { Part(title = "Back", summary = "", first = 3, last = 2) },
        )
        assertEquals(
            WorkCorpus.WorkShapeError.PartOutOfRange(first = 0, last = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.PartOutOfRange> { Part(title = "Early", summary = "", first = 0, last = 2) },
        )
    }

    private fun work(
        language: String = "eng",
        threshold: Int = 3,
        parts: List<Part>? = null,
        free: List<Int> = listOf(1),
    ): Work {
        val pieces =
            (1..20).map { number ->
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

    companion object {
        private const val LINES = "    lines: [The first line.]\n"

        @JvmStatic
        fun invalidNumbers(): List<Arguments> =
            listOf(
                Arguments.of(LINES, "$LINES    cuts:\n      block: [2147483648]\n", "pieces[0].cuts.block[0]"),
                Arguments.of(LINES, "$LINES    cuts:\n      block: [99999999999999999999]\n", "pieces[0].cuts.block[0]"),
                Arguments.of(LINES, "$LINES    cuts:\n      block: ['1']\n", "pieces[0].cuts.block[0]"),
                Arguments.of(LINES, "$LINES    cuts:\n      block: [1.0]\n", "pieces[0].cuts.block[0]"),
                Arguments.of(LINES, "$LINES    cuts:\n      block: [true]\n", "pieces[0].cuts.block[0]"),
                Arguments.of("  - number: 1\n", "  - number: '1'\n", "pieces[0].number"),
                Arguments.of("  - number: 1\n", "  - number: 01\n", "pieces[0].number"),
                Arguments.of("    last: 1\n", "    last: '1'\n", "parts[0].last"),
                Arguments.of("free: [1]\n", "free: [2147483648]\n", "free[0]"),
                Arguments.of("  score_threshold: 3\n", "  score_threshold: 3.5\n", "difficult_words.score_threshold"),
            )
    }
}
