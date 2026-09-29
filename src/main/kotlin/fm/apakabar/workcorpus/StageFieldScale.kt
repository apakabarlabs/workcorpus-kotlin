package fm.apakabar.workcorpus

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

/**
 * Divides a stage-completion fraction into display bands.
 *
 * Creating a scale directly does not validate the order of its bounds.
 *
 * @property untouchedBelow Upper bound of the untouched band.
 * @property begunBelow Upper bound of the begun band.
 * @property mostBelow Upper bound of the mostly-complete band.
 * @throws WorkCorpus.WorkShapeError.InvalidFraction naming the bound when one decoded from YAML
 * is not written in plain decimal digits, or one decoded from JSON is not a number a `Double`
 * holds with fewer than 38 significant digits.
 * @throws WorkCorpus.DocumentError when a decoded scale misses a bound or is not a mapping.
 */
@Serializable(with = StageFieldScaleSerializer::class)
data class StageFieldScale(
    val untouchedBelow: Double,
    val begunBelow: Double,
    val mostBelow: Double,
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

internal object StageFieldScaleSerializer : KSerializer<StageFieldScale> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("fm.apakabar.workcorpus.StageFieldScale") {
            element("untouched_below", Double.serializer().descriptor)
            element("begun_below", Double.serializer().descriptor)
            element("most_below", Double.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): StageFieldScale = readStageFieldScale(treeOf(decoder))

    override fun serialize(
        encoder: Encoder,
        value: StageFieldScale,
    ) = encoder.encodeStructure(descriptor) {
        encodeDoubleElement(descriptor, 0, value.untouchedBelow)
        encodeDoubleElement(descriptor, 1, value.begunBelow)
        encodeDoubleElement(descriptor, 2, value.mostBelow)
    }
}
