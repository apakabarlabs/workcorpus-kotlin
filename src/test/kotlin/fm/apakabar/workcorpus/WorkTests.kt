package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
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
    fun `difficult words need a positive score threshold`() {
        assertFailsWith<WorkCorpus.WorkShapeError.InvalidDifficultWordThreshold> {
            WorkCorpus.validateConfiguration(work(threshold = 0))
        }
    }

    @Test
    fun `the pieces free to read have to be unique members of the work`() {
        assertFailsWith<WorkCorpus.WorkShapeError.InvalidFreePieces> {
            WorkCorpus.validateConfiguration(work(free = listOf(1, 1)))
        }
    }

    @Test
    fun `a book that does not name its language is refused, and says so`() {
        val book = Fixtures.text("book-with-listening").replace("language: eng\n", "")

        assertNotEquals(Fixtures.text("book-with-listening"), book)
        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWorkFromBook(book) }
        assertTrue("language" in error.message.orEmpty(), error.message)
    }

    @Test
    fun `a book that leaves out the marks inside a word or its elisions is refused, naming the key`() {
        for ((left, key) in listOf("interior_marks: \"'’-\"\n" to "interior_marks", ELISIONS to "elisions")) {
            val book = Fixtures.text("book-with-listening").replace(left, "")

            assertNotEquals(Fixtures.text("book-with-listening"), book)
            val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWorkFromBook(book) }
            assertEquals("The work's $key is missing.", error.message)
        }
    }

    @Test
    fun `a book whose marks or elisions are not the kind of value they hold is refused there`() {
        val wrongs =
            listOf(
                Triple("interior_marks: \"'’-\"\n", "interior_marks: [\"'\"]\n", "interior_marks is not text"),
                Triple(ELISIONS, "elisions:\n  - th’\n", "elisions is not a mapping"),
                Triple(ELISIONS, "elisions: null\n", "elisions is not a mapping"),
                Triple("  th’: [the]\n", "  th’: the\n", "elisions.th’ is not a list"),
            )
        for ((written, wrong, said) in wrongs) {
            val book = Fixtures.text("book-with-listening").replace(written, wrong)

            assertNotEquals(Fixtures.text("book-with-listening"), book)
            val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWorkFromBook(book) }
            assertEquals("The work's $said.", error.message)
        }
    }

    @Test
    fun `a JSON work whose elisions are null is refused, naming the key`() {
        val json = """{"language": "eng", "interior_marks": "", "elisions": null}"""

        val error = assertFailsWith<WorkCorpus.DocumentError> { Json.decodeFromString(Work.serializer(), json) }
        assertEquals("The work's elisions is not a mapping.", error.message)
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
    fun `what is wrong with a number is said naming the field`() {
        assertEquals(
            "The work's pieces[0].number is not a whole number that fits in 32 bits.",
            WorkCorpus.WorkShapeError.InvalidNumber("pieces[0].number").message,
        )
    }

    @Test
    fun `a part held so that it ends before it starts cannot be made`() {
        assertEquals(
            WorkCorpus.WorkShapeError.PartOutOfRange(first = 3, last = 2),
            assertFailsWith<WorkCorpus.WorkShapeError.PartOutOfRange> { Part(title = "Back", summary = "", first = 3, last = 2) },
        )
    }

    private fun work(
        threshold: Int = 3,
        free: List<Int> = listOf(1),
    ): Work {
        val pieces =
            (1..20).map { number ->
                Piece(number = number, title = "Piece $number", lines = listOf("A line of verse,"))
            }
        return Work(
            language = "eng",
            interiorMarks = "'’-",
            elisions = emptyMap(),
            pieces = pieces,
            parts = listOf(Part(title = "The work", summary = "", first = 1, last = pieces.size)),
            free = free,
            stageField = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0),
            difficultWords = DifficultWordsConfiguration(scoreThreshold = threshold),
        )
    }

    private companion object {
        const val ELISIONS = "elisions:\n  tatter’d: [tattered]\n  th’: [the]\n"
    }
}
