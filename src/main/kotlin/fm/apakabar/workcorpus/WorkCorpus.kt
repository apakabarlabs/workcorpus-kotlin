package fm.apakabar.workcorpus

/**
 * Decodes, assembles, and validates portable reading works.
 *
 * Every number a work carries, from piece numbers to cut sizes, is a YAML integer that
 * fits in 32 bits, written as plain decimal digits; see [Work].
 */
object WorkCorpus {
    /** Piece numbering does not form the required sequence beginning at one. */
    sealed class CorpusError(
        message: String,
    ) : Exception(message) {
        /** The piece at one position carries another number. */
        data class OutOfOrder(
            val expected: Int,
            val found: Int,
        ) : CorpusError("Expected piece $expected, found $found.")

        final override fun toString(): String = "${javaClass.name}: $message"
    }

    /** A work-file piece identifier is not an integer. */
    sealed class WorkError(
        message: String,
    ) : Exception(message) {
        /**
         * A piece or free-piece identifier is not a number written as a work writes one:
         * plain decimal digits that fit in 32 bits, as [Work] describes.
         */
        data class PieceIsNotNumbered(
            val id: String,
        ) : WorkError("The work calls a piece $id, which is not a number.")

        final override fun toString(): String = "${javaClass.name}: $message"
    }

    /** The work, or one of its pieces, is not shaped the way a work has to be. */
    sealed class WorkShapeError(
        message: String,
    ) : Exception(message) {
        /** The parts leave a gap, overlap, or stop short of the last piece. */
        class PartsDoNotCoverTheWork : WorkShapeError("The parts do not cover the work exactly once.")

        /**
         * A part does not start at piece one or later, ends before it starts, or runs past
         * the last piece of the work.
         */
        data class PartOutOfRange(
            val first: Int,
            val last: Int,
        ) : WorkShapeError("A part runs from piece $first to piece $last, which is not a range of pieces in the work.")

        /** The free pieces are empty, repeated, or outside the work. */
        class InvalidFreePieces : WorkShapeError("The list of pieces free to read is empty, repeated, or outside the work.")

        /** The stage field bounds are not increasing values between zero and one. */
        class InvalidStageFieldScale : WorkShapeError("The stage field bounds are not increasing values between zero and one.")

        class InvalidDifficultWordThreshold : WorkShapeError("The difficult-word score threshold must be positive.")

        /**
         * A field that holds a number holds something else: a quoted string, a float, a
         * boolean, null, a list or a mapping, an integer that does not fit in 32 bits, or
         * one not written as plain decimal digits. The parser's own error, when there is
         * one, is kept as the cause.
         */
        data class InvalidNumber(
            val place: String,
        ) : WorkShapeError("The work's $place is not a whole number that fits in 32 bits.") {
            internal constructor(place: String, cause: Throwable) : this(place) {
                initCause(cause)
            }
        }

        data class InvalidLanguage(
            val value: String,
        ) : WorkShapeError(
                "The work names its language as \"${shown(value)}\", which is not a language tag such as en, eng or en-GB.",
            )

        data class CutsForUnknownStage(
            val piece: Int,
            val stage: String,
        ) : WorkShapeError("Piece $piece is cut for a stage called $stage, which is not a reading stage.")

        /** A piece is cut for the line stage, which is never cut. */
        data class CutsForLineStage(
            val piece: Int,
        ) : WorkShapeError("Piece $piece is cut for the line stage, which is read one line at a time.")

        /** A piece has a cut of zero or fewer lines at a stage. */
        data class EmptyCut(
            val piece: Int,
            val stage: String,
            val size: Int,
        ) : WorkShapeError("Piece $piece has a $stage cut of $size lines; a cut holds at least one.")

        data class CutsDoNotCoverThePiece(
            val piece: Int,
            val stage: String,
            val cut: Int,
            val lines: Int,
        ) : WorkShapeError("Piece $piece is cut at the $stage stage into $cut lines, but it has $lines.")

        final override fun toString(): String = "${javaClass.name}: $message"
    }

    /**
     * The pieces of a work are numbered from one and read in that order, which is what
     * a reader's place in the work is counted by. What each piece holds is the work's
     * own business and is read from its file, not decided here.
     *
     * @throws CorpusError.OutOfOrder when a piece carries a number other than its position.
     */
    fun validate(pieces: List<Piece>) {
        pieces.forEachIndexed { index, piece ->
            val expected = index + 1
            if (piece.number != expected) {
                throw CorpusError.OutOfOrder(expected = expected, found = piece.number)
            }
        }
    }

    /** Returns the part containing [piece], or `null` when no part covers it. */
    fun part(
        piece: Int,
        parts: List<Part>,
    ): Part? = parts.firstOrNull { it.contains(piece) }

    /**
     * Decodes a nested work-file YAML document and validates the resulting work.
     *
     * @throws com.charleskorn.kaml.YamlException when the document is not YAML, or a field
     * is missing or is not text where text belongs.
     * @throws WorkError.PieceIsNotNumbered when a piece or free-piece identifier is not a
     * whole number within 32 bits.
     * @throws WorkShapeError when a number is not a YAML integer within 32 bits, a piece's
     * cuts do not divide its lines, or the parts, free pieces, thresholds or language are
     * not shaped as a work's must be.
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun decodeWork(yaml: String): Work = validated(assembleWork(yaml))

    /**
     * Decodes an assembled book YAML document and validates the resulting work.
     *
     * @throws com.charleskorn.kaml.YamlException when the document is not YAML, or a field
     * is missing or is not text where text belongs.
     * @throws WorkShapeError when a number is not a YAML integer within 32 bits, a piece's
     * cuts do not divide its lines, or the parts, free pieces, thresholds or language are
     * not shaped as a work's must be.
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun decodeWorkFromBook(yaml: String): Work = validated(workYaml.decodeFromString(Work.serializer(), yaml))

    /**
     * Assembles held values into a work and validates its complete shape.
     *
     * @param language The language the work names itself as written in.
     * @param pieces The pieces in reading order, each with the part it is filed under.
     * @param reading What a reading of the work is held to.
     * @throws WorkShapeError when a piece's cuts do not divide its lines, or the parts, free
     * pieces, thresholds or language are not shaped as a work's must be.
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun work(
        language: String,
        pieces: List<HeldPiece>,
        reading: HeldReading,
    ): Work = validated(assemble(language, pieces, reading))

    private fun validated(work: Work): Work {
        validate(work.pieces)
        validateConfiguration(work)
        return work
    }

    internal fun validateConfiguration(work: Work) {
        var next = 1
        for (part in work.parts) {
            if (part.first != next || part.last < part.first) throw WorkShapeError.PartsDoNotCoverTheWork()
            if (part.last > work.pieces.size) throw WorkShapeError.PartOutOfRange(first = part.first, last = part.last)
            next = part.last + 1
        }
        if (next != work.pieces.size + 1) throw WorkShapeError.PartsDoNotCoverTheWork()

        val free = work.free.toSet()
        val numbered = 1..maxOf(work.pieces.size, 1)
        if (free.isEmpty() || free.size != work.free.size || !free.all { it in numbered }) {
            throw WorkShapeError.InvalidFreePieces()
        }

        val scale = work.stageField
        val increasing =
            scale.untouchedBelow > 0 &&
                scale.untouchedBelow < scale.begunBelow &&
                scale.begunBelow < scale.mostBelow &&
                scale.mostBelow <= 1
        if (!increasing) throw WorkShapeError.InvalidStageFieldScale()

        if (work.difficultWords.scoreThreshold <= 0) throw WorkShapeError.InvalidDifficultWordThreshold()

        if (!isLanguageTag(work.language)) throw WorkShapeError.InvalidLanguage(work.language)
    }

    private fun isLanguageTag(value: String): Boolean {
        val subtags = value.split('-')
        val language = subtags.first()
        if (language.length !in 2..3 || !language.all { it in 'a'..'z' }) return false
        return subtags.drop(1).all { subtag ->
            subtag.length in 2..8 && subtag.all { it in 'a'..'z' || it in 'A'..'Z' || it in '0'..'9' }
        }
    }

    internal fun validateCuts(
        piece: Int,
        lines: Int,
        cutSizes: Map<String, List<Int>>,
    ) {
        val labels = cutSizes.entries.sortedWith { a, b -> compareCodePoints(a.key, b.key) }
        for ((label, sizes) in labels) {
            val stage =
                ReadingStage.entries.firstOrNull { it.label == label }
                    ?: throw WorkShapeError.CutsForUnknownStage(piece = piece, stage = label)
            if (stage == ReadingStage.LINE) throw WorkShapeError.CutsForLineStage(piece = piece)
            sizes.firstOrNull { it <= 0 }?.let { empty ->
                throw WorkShapeError.EmptyCut(piece = piece, stage = label, size = empty)
            }
            val cut = sizes.sumOf { it.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (cut != lines) {
                throw WorkShapeError.CutsDoNotCoverThePiece(piece = piece, stage = label, cut = cut, lines = lines)
            }
        }
    }

    private fun compareCodePoints(
        a: String,
        b: String,
    ): Int = java.util.Arrays.compare(a.codePoints().toArray(), b.codePoints().toArray())
}

private fun shown(value: String): String =
    value.codePoints().toArray().joinToString("") { point ->
        if (point in 0x20..0x7E && point != '"'.code && point != '\\'.code) {
            point.toChar().toString()
        } else {
            "\\u{${Integer.toHexString(point).uppercase()}}"
        }
    }
