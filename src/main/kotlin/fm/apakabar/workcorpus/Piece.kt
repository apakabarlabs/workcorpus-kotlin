package fm.apakabar.workcorpus

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
 * divide the lines.
 */
@Serializable
data class Piece(
    val number: Long,
    val title: String,
    val lines: List<String>,
    @SerialName("cuts")
    @Serializable(with = CutsSerializer::class)
    val cutSizes: Map<String, List<Long>> = emptyMap(),
) {
    init {
        WorkCorpus.validateCuts(piece = number, lines = lines.size, cutSizes = cutSizes)
    }

    /** Stable identity, equal to the piece number. */
    val id: Long get() = number

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
        return sizes.map { size -> (start until start + size.toInt()).also { start += size.toInt() } }
    }
}

internal object CutSizeSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("fm.apakabar.workcorpus.CutSize", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val text = decoder.decodeString()
        return saturatedLong(text) ?: throw IllegalArgumentException("A cut size of $text is not a whole number of lines.")
    }

    override fun serialize(
        encoder: Encoder,
        value: Long,
    ) = encoder.encodeLong(value)
}

internal object CutsSerializer : KSerializer<Map<String, List<Long>>> {
    private val table = MapSerializer(String.serializer(), ListSerializer(CutSizeSerializer))

    override val descriptor: SerialDescriptor = table.nullable.descriptor

    override fun deserialize(decoder: Decoder): Map<String, List<Long>> = decoder.decodeSerializableValue(table.nullable) ?: emptyMap()

    override fun serialize(
        encoder: Encoder,
        value: Map<String, List<Long>>,
    ) = encoder.encodeSerializableValue(table, value)
}

internal fun saturatedLong(text: String): Long? {
    val negative = text.startsWith("-")
    val digits = if (negative || text.startsWith("+")) text.substring(1) else text
    if (digits.isEmpty() || !digits.all { it in '0'..'9' }) return null
    var magnitude = 0L
    for (digit in digits) {
        val value = (digit - '0').toLong()
        if (magnitude > (Long.MAX_VALUE - value) / 10) return if (negative) Long.MIN_VALUE else Long.MAX_VALUE
        magnitude = magnitude * 10 + value
    }
    return if (negative) -magnitude else magnitude
}
