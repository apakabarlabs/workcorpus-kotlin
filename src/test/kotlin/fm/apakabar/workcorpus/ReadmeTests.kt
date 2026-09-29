package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class ReadmeTests {
    private fun yamlShownIn(path: String): List<String> =
        Regex("```yaml\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(path).readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `decodeWork reads a work and its first piece is cut the way the work says`() {
        val work = WorkCorpus.decodeWork(Fixtures.work())
        val piece = work.pieces[0]
        val firstBlock = piece.cuts(ReadingStage.BLOCK)[0]

        assertEquals(0..1, firstBlock)
    }

    @Test
    fun `the work file and the book the reference shows are both read, as the same work`() {
        val shown = yamlShownIn("docs/module.md")

        assertEquals(2, shown.size, "docs/module.md shows a work file and a book")
        val file = WorkCorpus.decodeWork(shown[0])
        val book = WorkCorpus.decodeWorkFromBook(shown[1])
        assertEquals(file.pieces.map { it.title }, book.pieces.map { it.title })
        assertEquals(file.language, book.language)
    }
}
