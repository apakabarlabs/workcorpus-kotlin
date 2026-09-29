package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class ReadmeTests {
    private fun shownIn(
        path: String,
        language: String,
    ): List<String> =
        Regex("```$language\n(.*?)```", RegexOption.DOT_MATCHES_ALL)
            .findAll(File(path).readText())
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `the README's work is read the way the README says, by the code it shows`() {
        val yaml = shownIn("README.md", "yaml").single()
        val code = shownIn("README.md", "kotlin").first()

        assertEquals(README_USE, code, "README.md shows other code than this test runs")
        val work = WorkCorpus.decodeWork(yaml)
        val piece = work.pieces[0]
        val firstBlock = piece.cuts(ReadingStage.BLOCK)[0]

        check(work.language == "eng")
        check(firstBlock == 0..1)
    }

    @Test
    fun `the work file and the book the reference shows are both read, as the same work`() {
        val shown = shownIn("docs/module.md", "yaml")

        assertEquals(2, shown.size, "docs/module.md shows a work file and a book")
        val file = WorkCorpus.decodeWork(shown[0])
        val book = WorkCorpus.decodeWorkFromBook(shown[1])
        assertEquals(file.pieces.map { it.title }, book.pieces.map { it.title })
        assertEquals(file.language, book.language)
    }

    private companion object {
        val README_USE =
            """
            import fm.apakabar.workcorpus.ReadingStage
            import fm.apakabar.workcorpus.WorkCorpus

            val work = WorkCorpus.decodeWork(yaml)
            val piece = work.pieces[0]
            val firstBlock = piece.cuts(ReadingStage.BLOCK)[0]

            check(work.language == "eng")
            check(firstBlock == 0..1)
            """.trimIndent() + "\n"
    }
}
