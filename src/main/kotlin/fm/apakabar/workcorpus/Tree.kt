package fm.apakabar.workcorpus

import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

internal sealed class Node {
    abstract val place: String

    class Scalar(
        override val place: String,
        val text: String,
        val bare: Boolean,
        val json: Boolean,
    ) : Node()

    class Null(
        override val place: String,
    ) : Node()

    class Mapping(
        override val place: String,
        val entries: Map<String, Node>,
    ) : Node()

    class Sequence(
        override val place: String,
        val items: List<Node>,
    ) : Node()
}

internal fun treeOf(decoder: Decoder): Node =
    when (decoder) {
        is JsonDecoder -> jsonNode(decoder.decodeJsonElement(), place = "")
        else -> throw WorkCorpus.DocumentError(
            "A work is read from JSON, not from ${decoder::class.simpleName}; " +
                "YAML is read with WorkCorpus.decodeWork or decodeWorkFromBook.",
        )
    }

private fun jsonNode(
    element: JsonElement,
    place: String,
): Node =
    when (element) {
        is JsonNull -> Node.Null(place)
        is JsonPrimitive -> Node.Scalar(place, element.content, bare = !element.isString, json = true)
        is JsonArray -> Node.Sequence(place, element.mapIndexed { index, item -> jsonNode(item, "$place[$index]") })
        is JsonObject -> Node.Mapping(place, element.mapValues { (key, value) -> jsonNode(value, within(place, key)) })
    }

internal fun within(
    place: String,
    name: String,
): String = if (place.isEmpty()) name else "$place.$name"
