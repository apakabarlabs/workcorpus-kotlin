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
     * @throws CorpusError.OutOfOrder when the pieces are not numbered from one in order.
     */
    fun work(
        pieces: List<HeldPiece>,
        reading: HeldReading,
    ): Work = validated(assemble(pieces, reading))

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
    }
}
