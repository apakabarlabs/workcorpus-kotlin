package fm.apakabar.workcorpus

import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlPath
import com.charleskorn.kaml.YamlPathSegment
import com.charleskorn.kaml.YamlScalar
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.nullable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
 * divide the lines, or naming the field when a number decoded from YAML is not an
 * integer within 32 bits.
 */
@Serializable
data class Piece(
    @Serializable(with = WholeNumberSerializer::class)
    val number: Int,
    val title: String,
    val lines: List<String>,
    @SerialName("cuts")
    @Serializable(with = CutsSerializer::class)
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

internal object WholeNumberSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("fm.apakabar.workcorpus.WholeNumber", PrimitiveKind.INT)

    override fun deserialize(decoder: Decoder): Int {
        if (decoder !is YamlInput) return decoder.decodeInt()
        val place = place(decoder.node.path)
        val scalar = decoder.node as? YamlScalar
        if (scalar == null || !scalar.plain || !isPlainDecimal(scalar.content)) {
            throw WorkCorpus.WorkShapeError.InvalidNumber(place)
        }
        return try {
            decoder.decodeInt()
        } catch (failure: YamlException) {
            throw WorkCorpus.WorkShapeError.InvalidNumber(place, failure)
        }
    }

    override fun serialize(
        encoder: Encoder,
        value: Int,
    ) = encoder.encodeInt(value)

    private fun isPlainDecimal(written: String): Boolean {
        val digits = written.removePrefix("-")
        return digits.isNotEmpty() && digits.all { it in '0'..'9' } && (digits[0] != '0' || digits.length == 1)
    }

    private fun place(path: YamlPath): String =
        path.segments
            .joinToString("") { segment ->
                when (segment) {
                    is YamlPathSegment.ListEntry -> "[${segment.index}]"
                    is YamlPathSegment.MapElementKey -> ".${segment.key}"
                    else -> ""
                }
            }.removePrefix(".")
}

internal object CutsSerializer : KSerializer<Map<String, List<Int>>> {
    private val table = MapSerializer(String.serializer(), ListSerializer(WholeNumberSerializer))

    override val descriptor: SerialDescriptor = table.nullable.descriptor

    override fun deserialize(decoder: Decoder): Map<String, List<Int>> = decoder.decodeSerializableValue(table.nullable) ?: emptyMap()

    override fun serialize(
        encoder: Encoder,
        value: Map<String, List<Int>>,
    ) = encoder.encodeSerializableValue(table, value)
}
