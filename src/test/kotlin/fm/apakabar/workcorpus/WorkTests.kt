package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
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
            pieces = pieces,
            parts = listOf(Part(title = "The work", summary = "", first = 1, last = pieces.size)),
            free = free,
            stageField = StageFieldScale(untouchedBelow = 0.001, begunBelow = 0.5, mostBelow = 1.0),
            difficultWords = DifficultWordsConfiguration(scoreThreshold = threshold),
        )
    }
}
