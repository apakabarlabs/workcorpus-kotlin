package fm.apakabar.workcorpus

import java.util.Locale

/**
 * Names the files of one work: its readings, their word times, and what a reader
 * shares of their own attempt.
 *
 * The stem is the work's, because the files of two works sit side by side on a phone
 * and in a bucket, and a name that says only a number says nothing about which work
 * the number belongs to.
 *
 * @property stem Work-specific prefix placed in every generated asset name.
 */
class PieceAsset(
    val stem: String,
) {
    /** Returns the zero-padded base name of a numbered piece. */
    fun name(piece: Int): String = "$stem-${padded(piece, PIECE_DIGITS)}"

    /** Returns the relative MP3 path for a piece and narration voice. */
    fun recording(
        piece: Int,
        voice: NarrationVoice,
    ): String = "${voice.rawValue}/${name(piece)}.mp3"

    /** Returns the JSON alignment filename for a piece and narration voice. */
    fun alignment(
        piece: Int,
        voice: NarrationVoice,
    ): String = "${name(piece)}-${voice.rawValue}.json"

    /** Returns a stable base name for a shared line attempt. */
    fun sharedReading(
        piece: Int,
        line: Int,
        heard: String?,
    ): String {
        val place = "s${padded(piece, PIECE_DIGITS)}-l${padded(line, LINE_DIGITS)}"
        val said = slug(heard ?: "")
        return if (said.isEmpty()) place else "$place-$said"
    }

    /** Extracts a piece number from a filename belonging to this work. */
    fun number(inName: String): Int? {
        val bare = withoutExtension(inName)
        val prefix = "$stem-"
        if (!bare.startsWith(prefix)) return null
        val digits =
            bare
                .removePrefix(prefix)
                .characters()
                .takeWhile { it.isNumber() }
                .joinToString("")
        return if (digits.all { it in '0'..'9' }) digits.toIntOrNull() else null
    }

    /**
     * Extracts the voice suffix from an alignment filename.
     *
     * Pass the filename returned by [alignment]. Recording paths and
     * shared-attempt names do not have the supported shape.
     */
    fun voice(inName: String): NarrationVoice? {
        val bare = withoutExtension(inName)
        val dash = bare.lastIndexOf('-')
        if (dash <= 0) return null
        val tail = bare.substring(dash + 1)
        return if (tail.characters().all { it.isNumber() }) null else NarrationVoice(tail)
    }

    private companion object {
        const val PIECE_DIGITS = 3
        const val LINE_DIGITS = 2
        const val WORDS_WORTH_READING_IN_A_FILE_NAME = 8

        fun padded(
            number: Int,
            digits: Int,
        ): String = String.format(Locale.ROOT, "%0${digits}d", number)

        fun slug(text: String): String =
            text
                .lowercase()
                .characters()
                .joinToString("") { if (it.isLetter() || it.isNumber()) it else " " }
                .split(' ')
                .filter { it.isNotEmpty() }
                .take(WORDS_WORTH_READING_IN_A_FILE_NAME)
                .joinToString("-")

        fun withoutExtension(name: String): String {
            val dot = name.lastIndexOf('.')
            if (dot <= 0 || name.substring(dot).contains('/')) return name
            return name.substring(0, dot)
        }
    }
}
