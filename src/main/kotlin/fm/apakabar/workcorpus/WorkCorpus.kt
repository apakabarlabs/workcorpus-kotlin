package fm.apakabar.workcorpus

/** Decodes, assembles, and validates portable reading works. */
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
        /** A piece or free-piece identifier cannot be converted to its number. */
        data class PieceIsNotNumbered(
            val id: String,
        ) : WorkError("The work calls a piece $id, which is not a number.")

        final override fun toString(): String = "${javaClass.name}: $message"
    }

    internal sealed class WorkShapeError(
        message: String,
    ) : Exception(message) {
        class PartsDoNotCoverTheWork : WorkShapeError("The parts do not cover the work exactly once.")

        class InvalidFreePieces : WorkShapeError("The list of pieces free to read is empty, repeated, or outside the work.")

        class InvalidStageFieldScale : WorkShapeError("The stage field bounds are not increasing values between zero and one.")

        class InvalidDifficultWordThreshold : WorkShapeError("The difficult-word score threshold must be positive.")

        class UnnamedLanguage : WorkShapeError("The work does not name the language it is written in.")

        data class CutsForUnknownStage(
            val piece: Int,
            val stage: String,
        ) : WorkShapeError("Piece $piece is cut for a stage called $stage, which is not a reading stage.")

        data class CutsForLineStage(
            val piece: Int,
        ) : WorkShapeError("Piece $piece is cut for the line stage, which is read one line at a time.")

        data class EmptyCut(
            val piece: Int,
            val stage: String,
            val size: Int,
        ) : WorkShapeError("Piece $piece has a $stage cut of $size lines; a cut holds at least one.")

        data class CutsOverrunThePiece(
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
     * @throws WorkError.PieceIsNotNumbered when a piece or free-piece identifier is not a number.
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun decodeWork(yaml: String): Work = validated(assembleWork(yaml))

    /**
     * Decodes an assembled book YAML document and validates the resulting work.
     *
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun decodeWorkFromBook(yaml: String): Work = validated(workYaml.decodeFromString(Work.serializer(), yaml))

    /**
     * Assembles held values into a work and validates its complete shape.
     *
     * @param language The language the work names itself as written in.
     * @param pieces The pieces in reading order, each with the part it is filed under.
     * @param reading What a reading of the work is held to.
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

        if (work.language.isBlank()) throw WorkShapeError.UnnamedLanguage()

        work.pieces.forEach(::validateCuts)
    }

    private fun validateCuts(piece: Piece) {
        for ((label, sizes) in piece.cutSizes.toSortedMap()) {
            val stage =
                ReadingStage.entries.firstOrNull { it.label == label }
                    ?: throw WorkShapeError.CutsForUnknownStage(piece = piece.number, stage = label)
            if (stage == ReadingStage.LINE) throw WorkShapeError.CutsForLineStage(piece = piece.number)
            sizes.firstOrNull { it <= 0 }?.let { empty ->
                throw WorkShapeError.EmptyCut(piece = piece.number, stage = label, size = empty)
            }
            val cut = sizes.sumOf { it.toLong() }.coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            if (cut > piece.lines.size) {
                throw WorkShapeError.CutsOverrunThePiece(
                    piece = piece.number,
                    stage = label,
                    cut = cut,
                    lines = piece.lines.size,
                )
            }
        }
    }
}
