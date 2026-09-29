package fm.apakabar.workcorpus

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A reading work and the configuration used to present it.
 *
 * Decode through [WorkCorpus.decodeWork] or [WorkCorpus.decodeWorkFromBook] to validate
 * the complete work before use. Decoding this type directly with [serializer] does not
 * validate relationships between its fields.
 *
 * @property language Language the work is written in, as the work names it. It arrives
 * with the work rather than being assumed, because the same reading mechanics carry
 * works in other languages, each held to its own alphabet.
 * @property pieces Reading pieces, expected to be numbered from one and ordered by number.
 * @property parts Parts, expected to cover the pieces consecutively and exactly once.
 * @property free Piece numbers intended to be available without purchase.
 * @property stageField Thresholds used to display stage progress.
 * @property difficultWords Threshold used to identify difficult words.
 */
@Serializable
@ConsistentCopyVisibility
data class Work internal constructor(
    val language: String,
    val pieces: List<Piece>,
    val parts: List<Part>,
    val free: List<Int>,
    @SerialName("stage_field") val stageField: StageFieldScale,
    @SerialName("difficult_words") val difficultWords: DifficultWordsConfiguration,
)

/**
 * Configuration for classifying repeatedly missed words.
 *
 * Creating a configuration directly does not validate it against a work.
 *
 * @property scoreThreshold Minimum accumulated score at which a word is difficult.
 */
@Serializable
data class DifficultWordsConfiguration(
    @SerialName("score_threshold") val scoreThreshold: Int,
)
