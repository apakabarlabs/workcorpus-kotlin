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
 * a bound decoded from YAML or JSON is not an integer within 32 bits.
 */
@Serializable(with = Part.Serializer::class)
data class Part(
    val title: String,
    val summary: String,
    val first: Int,
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

    internal object Serializer : KSerializer<Part> {
        override val descriptor: SerialDescriptor =
            buildClassSerialDescriptor("fm.apakabar.workcorpus.Part") {
                element("title", String.serializer().descriptor)
                element("summary", String.serializer().descriptor)
                element("first", Int.serializer().descriptor)
                element("last", Int.serializer().descriptor)
                element("short", String.serializer().descriptor, isOptional = true)
            }

        override fun deserialize(decoder: Decoder): Part = readPart(treeOf(decoder))

        override fun serialize(
            encoder: Encoder,
            value: Part,
        ) = encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.title)
            encodeStringElement(descriptor, 1, value.summary)
            encodeIntElement(descriptor, 2, value.first)
            encodeIntElement(descriptor, 3, value.last)
            value.short?.let { encodeStringElement(descriptor, 4, it) }
        }
    }
}
