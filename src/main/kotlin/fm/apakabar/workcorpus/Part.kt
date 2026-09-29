package fm.apakabar.workcorpus

import kotlinx.serialization.Serializable

/**
 * A run of pieces a work is divided into: a group of sonnets, a chapter, an act.
 *
 * Creating a part directly does not validate it against a work.
 *
 * @property title Full title shown when the part is introduced.
 * @property summary Reader-facing account of what the part contains.
 * @property first Number of the first piece in the part.
 * @property last Number of the last piece in the part.
 * @throws WorkCorpus.WorkShapeError.PartOutOfRange when the part does not start at piece
 * one or later, or ends before it starts; [WorkCorpus.WorkShapeError.InvalidNumber] when
 * a bound decoded from YAML is not an integer within 32 bits.
 */
@Serializable
data class Part(
    val title: String,
    val summary: String,
    @Serializable(with = WholeNumberSerializer::class)
    val first: Int,
    @Serializable(with = WholeNumberSerializer::class)
    val last: Int,
    private val short: String? = null,
) {
    init {
        if (first < 1 || last < first) throw WorkCorpus.WorkShapeError.PartOutOfRange(first = first, last = last)
    }

    /** Compact title when one was supplied, otherwise [title]. */
    val shortTitle: String get() = short ?: title

    /** Inclusive piece-number range occupied by the part. */
    val pieces: IntRange get() = first..last

    /** Stable identity, equal to the first piece number. */
    val id: Int get() = first

    /** Reports whether a numbered piece belongs to the part. */
    fun contains(piece: Int): Boolean = piece in pieces
}
