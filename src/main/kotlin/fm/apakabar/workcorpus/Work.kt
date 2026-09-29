package fm.apakabar.workcorpus

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.encodeStructure

/**
 * A reading work and the configuration used to present it.
 *
 * Decode through [WorkCorpus.decodeWork] or [WorkCorpus.decodeWorkFromBook] to validate
 * the complete work before use: they parse the YAML themselves, so they alone refuse the
 * YAML problems below. [serializer] also reads a work from JSON with kotlinx.serialization,
 * holding its numbers, fractions and texts to the rules below, as the shared cases test with
 * the default `Json`, but it does not validate relationships between the fields. A syntax
 * error in the JSON reaches the caller as kotlinx.serialization's own error, raised before
 * the library sees the document.
 *
 * Every number a work carries, from piece numbers to cut sizes, is a YAML integer that
 * fits in 32 bits, written as plain decimal digits: `0`, or digits that do not start
 * with `0` after an optional `-`. A `+`, `-0`, underscores, `0x`, `0o` or `0b`
 * prefixes and sexagesimal `1:30` are refused, so that every port reads a number the
 * same way.
 *
 * Decoded from JSON, a number is a JSON number whose value is a whole number that fits
 * in 32 bits, so `5` and `5.0` both read as 5. A string such as `"5"`, a boolean, null,
 * a fraction, a value past 32 bits and `-0` are refused as in YAML. The value is read as
 * a decimal from the literal rather than through a binary float, so
 * `5.000000000000000001` is a fraction. A number of 38 or more significant digits is
 * refused, since the lead's `Decimal` holds no more.
 *
 * The stage field bounds are fractions held to the same one writing: in YAML, plain
 * decimal digits with an optional `-` and fractional part, such as `0.001` or `1`, and
 * never quoted, with `_`, an exponent or as sexagesimal; in JSON, a JSON number of fewer
 * than 38 significant digits that a `Double` holds without rounding it to zero or
 * infinity.
 *
 * A work is written out in full. A YAML anchor, an alias or a `<<` merge key, quoted or
 * not, is refused with [WorkCorpus.WorkShapeError.YamlReference], since YAML readers do
 * not resolve them alike; an explicit YAML tag with [WorkCorpus.WorkShapeError.ExplicitTag];
 * a key named twice in one mapping with [WorkCorpus.WorkShapeError.RepeatedKey]. Of several
 * such problems, the one first in the document is reported.
 *
 * @property language Language the work is written in, as the work names it: a language
 * tag such as `en`, `eng` or `en-GB`. It arrives with the work rather than being
 * assumed, because the same reading mechanics carry works in other languages.
 * @property pieces Reading pieces, expected to be numbered from one and ordered by number.
 * @property parts Parts, expected to cover the pieces consecutively and exactly once.
 * @property free Piece numbers intended to be available without purchase.
 * @property stageField Thresholds used to display stage progress.
 * @property difficultWords Threshold used to identify difficult words.
 * @throws WorkCorpus.WorkShapeError.InvalidNumber, WorkCorpus.WorkShapeError.InvalidFraction
 * or WorkCorpus.WorkShapeError.NullText naming the field, when decoded, as [Piece], [Part],
 * [StageFieldScale] and [DifficultWordsConfiguration] throw them, and any of their shape
 * errors.
 * @throws WorkCorpus.DocumentError when a decoded work misses a field or a field holds another
 * kind of value than it names.
 */
@Serializable(with = WorkSerializer::class)
@ConsistentCopyVisibility
data class Work internal constructor(
    val language: String,
    val pieces: List<Piece>,
    val parts: List<Part>,
    val free: List<Int>,
    val stageField: StageFieldScale,
    val difficultWords: DifficultWordsConfiguration,
)

internal object WorkSerializer : KSerializer<Work> {
    private val pieces = ListSerializer(PieceSerializer)
    private val parts = ListSerializer(Part.Serializer)
    private val free = ListSerializer(Int.serializer())

    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("fm.apakabar.workcorpus.Work") {
            element("language", String.serializer().descriptor)
            element("pieces", pieces.descriptor)
            element("parts", parts.descriptor)
            element("free", free.descriptor)
            element("stage_field", StageFieldScaleSerializer.descriptor)
            element("difficult_words", DifficultWordsConfigurationSerializer.descriptor)
        }

    override fun deserialize(decoder: Decoder): Work = readWork(treeOf(decoder))

    override fun serialize(
        encoder: Encoder,
        value: Work,
    ) = encoder.encodeStructure(descriptor) {
        encodeStringElement(descriptor, 0, value.language)
        encodeSerializableElement(descriptor, 1, pieces, value.pieces)
        encodeSerializableElement(descriptor, 2, parts, value.parts)
        encodeSerializableElement(descriptor, 3, free, value.free)
        encodeSerializableElement(descriptor, 4, StageFieldScaleSerializer, value.stageField)
        encodeSerializableElement(descriptor, 5, DifficultWordsConfigurationSerializer, value.difficultWords)
    }
}

/**
 * Configuration for classifying repeatedly missed words.
 *
 * Creating a configuration directly does not validate it against a work.
 *
 * @property scoreThreshold Minimum accumulated score at which a word is difficult.
 * @throws WorkCorpus.WorkShapeError.InvalidNumber naming the field when a decoded threshold
 * is not a whole number within 32 bits.
 * @throws WorkCorpus.DocumentError when a decoded configuration misses its threshold or is not
 * a mapping.
 */
@Serializable(with = DifficultWordsConfigurationSerializer::class)
data class DifficultWordsConfiguration(
    val scoreThreshold: Int,
)

internal object DifficultWordsConfigurationSerializer : KSerializer<DifficultWordsConfiguration> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("fm.apakabar.workcorpus.DifficultWordsConfiguration") {
            element("score_threshold", Int.serializer().descriptor)
        }

    override fun deserialize(decoder: Decoder): DifficultWordsConfiguration = readDifficultWords(treeOf(decoder))

    override fun serialize(
        encoder: Encoder,
        value: DifficultWordsConfiguration,
    ) = encoder.encodeStructure(descriptor) {
        encodeIntElement(descriptor, 0, value.scoreThreshold)
    }
}
