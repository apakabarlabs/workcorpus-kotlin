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
 */
@Serializable
data class Part(
    val title: String,
    val summary: String,
    val first: Long,
    val last: Long,
    private val short: String? = null,
) {
    /** Compact title when one was supplied, otherwise [title]. */
    val shortTitle: String get() = short ?: title

    /** Inclusive piece-number range occupied by the part. */
    val pieces: LongRange get() = first..last

    /** Stable identity, equal to the first piece number. */
    val id: Long get() = first

    /** Reports whether a numbered piece belongs to the part. */
    fun contains(piece: Long): Boolean = piece in pieces
}
