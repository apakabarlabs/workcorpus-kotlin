package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class ReadmeTests {
    @Test
    fun `decodeWork reads a work and its first piece is cut the way the work says`() {
        val work = WorkCorpus.decodeWork(Fixtures.work())
        val piece = work.pieces[0]
        val firstBlock = piece.cuts(ReadingStage.BLOCK)[0]

        assertEquals(0..1, firstBlock)
    }

    @Test
    fun `the formats the reference shows are read`() {
        val file =
            """
            slug: poems
            language: eng
            title: Poems
            reading:
              untouched_below: 0.001
              begun_below: 0.5
              most_below: 1.0
              difficult_word_score: 3
              free: ['1']
            sections:
              - title: Opening poems
                summary: The first part.
                pieces:
                  - id: '1'
                    title: First poem
                    lines: [The first line.]
                    cuts:
                      block: [1]
            """.trimIndent()
        val book =
            """
            language: eng
            pieces:
              - number: 1
                title: First poem
                lines: [The first line.]
            parts:
              - title: Opening poems
                summary: The first part.
                first: 1
                last: 1
            free: [1]
            stage_field:
              untouched_below: 0.001
              begun_below: 0.5
              most_below: 1.0
            difficult_words:
              score_threshold: 3
            """.trimIndent()

        assertEquals(WorkCorpus.decodeWork(file).pieces.map { it.title }, WorkCorpus.decodeWorkFromBook(book).pieces.map { it.title })
    }
}
