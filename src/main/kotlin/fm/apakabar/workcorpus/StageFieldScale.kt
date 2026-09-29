package fm.apakabar.workcorpus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Divides a stage-completion fraction into display bands.
 *
 * Creating a scale directly does not validate the order of its bounds.
 *
 * @property untouchedBelow Upper bound of the untouched band.
 * @property begunBelow Upper bound of the begun band.
 * @property mostBelow Upper bound of the mostly-complete band.
 */
@Serializable
data class StageFieldScale(
    @SerialName("untouched_below") val untouchedBelow: Double,
    @SerialName("begun_below") val begunBelow: Double,
    @SerialName("most_below") val mostBelow: Double,
) {
    /** Returns the display band containing [fraction]. */
    fun band(fraction: Double): Band =
        when {
            fraction < untouchedBelow -> Band.UNTOUCHED
            fraction < begunBelow -> Band.BEGUN
            fraction < mostBelow -> Band.MOST
            else -> Band.WHOLE
        }

    /** A coarse display state for a stage-completion fraction. */
    enum class Band {
        /** Below the first configured bound. */
        UNTOUCHED,

        /** Between the first and second bounds. */
        BEGUN,

        /** Between the second and third bounds. */
        MOST,

        /** At or above the third bound. */
        WHOLE,
    }
}
