package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PartTests {
    private val parts =
        listOf(
            Part(
                title = "The Procreation Sonnets",
                summary = "Marry, and let your beauty outlive you.",
                first = 1,
                last = 17,
                short = "The Procreation",
            ),
            Part(
                title = "The Fair Youth",
                summary = "Praise, and the verse that outlasts what it praises.",
                first = 18,
                last = 77,
            ),
        )

    @Test
    fun `a part carries its range and the pieces inside it`() {
        assertEquals(1L..17L, parts.first().pieces)
        assertTrue(parts.first().contains(piece = 17))
        assertFalse(parts.first().contains(piece = 18))
    }

    @Test
    fun `the short name stands in for the full one, and the full one fills its place`() {
        assertEquals("The Procreation", parts.first().shortTitle)
        assertEquals("The Fair Youth", parts.last().shortTitle)
    }

    @Test
    fun `a piece finds the part it belongs to`() {
        assertEquals("The Procreation Sonnets", WorkCorpus.part(1, parts)?.title)
        assertNull(WorkCorpus.part(130, parts))
    }
}
