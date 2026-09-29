package fm.apakabar.workcorpus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * What a reader reads in one sitting: a sonnet, a stanza, a scene.
 *
 * Creating a piece directly does not validate its number or cuts against a work.
 *
 * @property number One-based position of the piece in its work.
 * @property title Reader-facing title.
 * @property lines Printed lines in reading order.
 * @property cutSizes Per-stage sizes of consecutive line groups, keyed by [ReadingStage.label].
 * A work that names no cuts decodes with an empty table.
 */
@Serializable
data class Piece(
    val number: Int,
    val title: String,
    val lines: List<String>,
    @SerialName("cuts") val cutSizes: Map<String, List<Int>> = emptyMap(),
) {
    /** Stable identity, equal to the piece number. */
    val id: Int get() = number

    /** First printed line, or an empty string when the piece has no lines. */
    val openingLine: String get() = lines.firstOrNull() ?: ""

    /**
     * The line ranges a reader takes in one attempt at this stage.
     *
     * Line by line needs no saying, so a work names only the cuts that group lines,
     * and it names them itself: how a sonnet falls into quatrains, a stanza into
     * couplets or a scene into speeches is the work's own shape, not a rule anyone
     * outside it can compute. A stage a work says nothing about is read line by line.
     */
    fun cuts(stage: ReadingStage): List<IntRange> {
        val sizes = cutSizes[stage.label]
        if (stage == ReadingStage.LINE || sizes.isNullOrEmpty()) {
            return lines.indices.map { it..it }
        }

        val ranges = mutableListOf<IntRange>()
        var start = 0
        for (size in sizes) {
            val end = minOf(start + size - 1, lines.size - 1)
            if (start > end) break
            ranges += start..end
            start = end + 1
        }
        if (start <= lines.size - 1) {
            ranges += start..(lines.size - 1)
        }
        return ranges
    }
}
