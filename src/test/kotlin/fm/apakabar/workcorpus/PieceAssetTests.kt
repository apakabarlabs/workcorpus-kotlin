package fm.apakabar.workcorpus

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PieceAssetTests {
    private val asset = PieceAsset(stem = "sonnet")

    @Test
    fun `the number is padded so the names sort in the order of the work`() {
        assertEquals("sonnet-004", asset.name(4))
        assertEquals("sonnet-154", asset.name(154))
    }

    @Test
    fun `the stem is the work's own, so two works do not share a name`() {
        assertEquals("onegin-004", PieceAsset(stem = "onegin").name(4))
        assertNull(PieceAsset(stem = "onegin").number(inName = "sonnet-004.mp3"))
    }

    @Test
    fun `the stem is written as it is, even where it looks like a format`() {
        val percent = PieceAsset(stem = "100%d-%@")

        assertEquals("100%d-%@-004", percent.name(4))
        assertEquals(4L, percent.number(inName = "100%d-%@-004.mp3"))
    }

    @Test
    fun `a number past 32 bits is written in full`() {
        assertEquals("sonnet-5000000000", asset.name(5_000_000_000))
        assertEquals("sonnet--04", asset.name(-4))
        assertEquals(5_000_000_000L, asset.number(inName = "sonnet-5000000000.mp3"))
        assertEquals("s5000000000-l03", asset.sharedReading(piece = 5_000_000_000, line = 3, heard = null))
    }

    @Test
    fun `a stem and a name spelled with combining marks still match`() {
        val composed = PieceAsset(stem = "café")
        val decomposed = PieceAsset(stem = "café")

        assertEquals(4L, composed.number(inName = "café-004.mp3"))
        assertEquals(4L, decomposed.number(inName = "café-004.mp3"))
        assertEquals(4L, decomposed.number(inName = "café-004.mp3"))
    }

    @Test
    fun `only ASCII digits are read as the digits of a number`() {
        val stem = PieceAsset(stem = "s")

        assertEquals(12L, stem.number(inName = "s-12三"))
        assertNull(stem.number(inName = "s-٣"))
        assertEquals(NarrationVoice("三"), stem.voice(inName = "s-001-三"))
        assertEquals(NarrationVoice("٣"), stem.voice(inName = "s-001-٣"))
        assertNull(stem.voice(inName = "s-001-042"))
    }

    @Test
    fun `a number is read from a bare name, never from a path`() {
        assertEquals(4L, asset.number(inName = "sonnet-004.mp3"))
        assertEquals(4L, asset.number(inName = "sonnet-004"))
        assertEquals(18L, asset.number(inName = "sonnet-018-onyx.json"))
        assertNull(asset.number(inName = asset.recording(4, voice = NarrationVoice.onyx)))
        assertNull(asset.number(inName = ""))
    }

    @Test
    fun `a voice is read off the alignment name it was written into`() {
        assertEquals(NarrationVoice.onyx, asset.voice(inName = asset.alignment(18, voice = NarrationVoice.onyx)))
        assertNull(asset.voice(inName = "sonnet-018.json"))
        assertNull(asset.voice(inName = ""))
    }

    @Test
    fun `a shared reading is named by piece, line and what was heard`() {
        assertEquals(
            "s004-l09-then-beauty-is-sniggered",
            asset.sharedReading(piece = 4, line = 9, heard = "Then beauty is sniggered"),
        )
    }

    @Test
    fun `a shared reading keeps its place when what was heard cannot be used`() {
        assertEquals("s001-l01", asset.sharedReading(piece = 1, line = 1, heard = null))
        assertEquals("s001-l01", asset.sharedReading(piece = 1, line = 1, heard = "  "))
        assertEquals(
            "s012-l13-o-then-love-hate",
            asset.sharedReading(piece = 12, line = 13, heard = "O! Then, love/hate?"),
        )
    }

    @Test
    fun `a shared reading takes the first eight words of what was heard`() {
        val line = "When forty winters shall besiege thy brow and dig deep trenches"
        assertEquals(
            "s002-l01-when-forty-winters-shall-besiege-thy-brow-and",
            asset.sharedReading(piece = 2, line = 1, heard = line),
        )
    }

    @Test
    fun `a letter written with a combining mark stays in its word`() {
        assertEquals(
            "s001-l01-नमस्ते-café",
            asset.sharedReading(piece = 1, line = 1, heard = "नमस्ते, Café!"),
        )
    }
}
