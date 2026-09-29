package fm.apakabar.workcorpus

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

/**
 * What a reader reads in one sitting: a sonnet, a stanza, a scene.
 *
 * Creating or decoding a piece refuses cuts that do not divide its lines: each stage
 * named in [cutSizes] has to be a reading stage other than [ReadingStage.LINE], and its
 * sizes have to be positive and add up to exactly the number of lines. The number is
 * not validated against a work.
 *
 * @property number One-based position of the piece in its work.
 * @property title Reader-facing title.
 * @property lines Printed lines in reading order.
 * @property cutSizes Per-stage sizes of consecutive line groups, keyed by [ReadingStage.label].
 * A work that names no cuts, or names them as null, decodes with an empty table.
 * @throws WorkCorpus.WorkShapeError naming the piece and the stage when the cuts do not
 * divide the lines, or naming the field when a number decoded from YAML or JSON is not
 * an integer within 32 bits.
 */
@Serializable(with = PieceSerializer::class)
data class Piece(
    val number: Int,
    val title: String,
    val lines: List<String>,
    val cutSizes: Map<String, List<Int>> = emptyMap(),
) {
    init {
        WorkCorpus.validateCuts(piece = number, lines = lines.size, cutSizes = cutSizes)
    }

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

        var start = 0
        return sizes.map { size -> (start until start + size).also { start += size } }
    }
}

internal object PieceSerializer : KSerializer<Piece> {
    private val lines = ListSerializer(String.serializer())
    private val cuts = MapSerializer(String.serializer(), ListSerializer(Int.serializer()))

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("fm.apakabar.workcorpus.Piece") {
            element("number", Int.serializer().descriptor)
            element("title", String.serializer().descriptor)
            element("lines", lines.descriptor)
            element("cuts", cuts.descriptor, isOptional = true)
        }

    override fun deserialize(decoder: Decoder): Piece = readPiece(treeOf(decoder))

    override fun serialize(
        encoder: Encoder,
        value: Piece,
    ) = encoder.encodeStructure(descriptor) {
        encodeIntElement(descriptor, 0, value.number)
        encodeStringElement(descriptor, 1, value.title)
        encodeSerializableElement(descriptor, 2, lines, value.lines)
        if (value.cutSizes.isNotEmpty()) encodeSerializableElement(descriptor, 3, cuts, value.cutSizes)
    }
}
