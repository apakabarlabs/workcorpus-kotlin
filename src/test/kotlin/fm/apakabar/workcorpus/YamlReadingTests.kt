package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class YamlReadingTests {
    private val book = Fixtures.text("book-with-listening")

    @Test
    fun `a book followed by a second document is refused, whatever the second holds`() {
        val seconds =
            listOf(
                "---\n$book",
                "---\n",
                "---\nfoo: 1\n",
                "--- &a\nfoo: 1\n",
                "---\nfoo: &a 1\n",
                "---\nfoo: !!str 1\n",
                "---\nfree: [1]\nfree: [2]\n",
                "---\nfoo: [1\n",
            )
        for (second in seconds) {
            val error = assertFailsWith<WorkCorpus.DocumentError>(second) { WorkCorpus.decodeWorkFromBook(book + second) }
            assertEquals(SECOND_DOCUMENT, error.message)
        }
    }

    @Test
    fun `a second document after an ended one, without a start of its own, is refused by the parser`() {
        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWorkFromBook("$book...\n$book") }

        assertNotNull(error.cause)
    }

    @Test
    fun `a work file followed by a second document is refused the same way`() {
        val work = Fixtures.work()

        val error = assertFailsWith<WorkCorpus.DocumentError> { WorkCorpus.decodeWork("$work---\n$work") }
        assertEquals(SECOND_DOCUMENT, error.message)
    }

    @Test
    fun `a problem in the work is named before the second document that follows it`() {
        val problems =
            listOf(
                "    title: &poem First poem\n" to WorkCorpus.WorkShapeError.YamlReference("pieces[0].title"),
                "    title: *nowhere\n" to WorkCorpus.WorkShapeError.YamlReference("pieces[0].title"),
                "    title: First poem\n    title: Again\n" to WorkCorpus.WorkShapeError.RepeatedKey("title"),
            )
        for ((written, problem) in problems) {
            val first = book.replace("    title: First poem\n", written)

            assertEquals(problem, assertFailsWith<WorkCorpus.WorkShapeError> { WorkCorpus.decodeWorkFromBook("$first---\nx: 1\n") })
        }
    }

    @Test
    fun `one document marked with a start and an end is read`() {
        val work = WorkCorpus.decodeWorkFromBook("---\n$book...\n# after the end\n")

        assertEquals(listOf("First poem"), work.pieces.map { it.title })
    }

    @Test
    fun `a key written as YAML null is text, and as a key the work does not know it is skipped`() {
        for (key in listOf("null: 1\n", "~: 1\n", "NULL: 1\n", "\"\": 1\n", "? \n: 1\n")) {
            val work = WorkCorpus.decodeWorkFromBook(book + key)

            assertEquals(listOf("First poem"), work.pieces.map { it.title }, key)
        }
    }

    @Test
    fun `a character YAML does not allow is refused as a document error, the parser's own kept as its cause`() {
        val error =
            assertFailsWith<WorkCorpus.DocumentError> {
                WorkCorpus.decodeWorkFromBook(book.replace("First poem", "First\u0001poem"))
            }

        assertNotNull(error.cause)
    }

    private companion object {
        const val SECOND_DOCUMENT = "The work cannot be read as YAML: a second document follows it."
    }
}
