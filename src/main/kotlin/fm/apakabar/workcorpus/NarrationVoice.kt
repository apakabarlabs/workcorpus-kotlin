package fm.apakabar.workcorpus

import kotlinx.serialization.Serializable

/**
 * Stable identifier of a narration voice used in asset names.
 *
 * Callers are responsible for supplying a value that is safe as a path component.
 *
 * @property rawValue Identifier used verbatim in generated asset names.
 */
@Serializable
@JvmInline
value class NarrationVoice(
    val rawValue: String,
) {
    companion object {
        /** The built-in Onyx narration voice. */
        val onyx = NarrationVoice("onyx")

        /** Built-in narration voices known to this release. */
        val all: List<NarrationVoice> = listOf(onyx)
    }
}
