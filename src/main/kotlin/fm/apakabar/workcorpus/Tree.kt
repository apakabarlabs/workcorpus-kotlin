package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlScalar
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * One value of a work as it was written, in YAML or JSON, with the place it stands at.
 *
 * Every reader of a work reads this tree rather than a format's own decoder, so that YAML
 * and JSON are held to the same rules and every refusal names the field it found.
 */
internal sealed class Node {
    abstract val place: String

    /** A scalar: [bare] when written without quotes, as a YAML plain scalar or a JSON literal. */
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

internal fun parseYaml(yaml: String): Node {
    refuseYamlProblems(yaml)
    val root =
        try {
            Yaml.default.parseToYamlNode(yaml)
        } catch (failure: YamlException) {
            throw WorkCorpus.DocumentError("The work cannot be read as YAML: ${failure.message}", failure)
        }
    return yamlNode(root, place = "")
}

internal fun treeOf(decoder: Decoder): Node =
    when (decoder) {
        is YamlInput -> yamlNode(decoder.node, place = "")
        is JsonDecoder -> jsonNode(decoder.decodeJsonElement(), place = "")
        else -> throw WorkCorpus.DocumentError("A work is read from YAML or JSON, not from ${decoder::class.simpleName}.")
    }

private val CORE_SCHEMA_NULLS = setOf("", "~", "null", "Null", "NULL")

private fun yamlNode(
    node: YamlNode,
    place: String,
): Node =
    when (node) {
        is YamlNull -> Node.Null(place)
        is YamlScalar ->
            if (node.plain && node.content in CORE_SCHEMA_NULLS) {
                Node.Null(place)
            } else {
                Node.Scalar(place, node.content, bare = node.plain, json = false)
            }
        is YamlList -> Node.Sequence(place, node.items.mapIndexed { index, item -> yamlNode(item, "$place[$index]") })
        is YamlMap ->
            Node.Mapping(
                place,
                node.entries.entries.associate { (key, value) ->
                    key.content to yamlNode(value, within(place, key.content))
                },
            )
        is YamlTaggedNode -> yamlNode(node.innerNode, place)
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
