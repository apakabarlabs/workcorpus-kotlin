package fm.apakabar.workcorpus

import com.charleskorn.kaml.DuplicateKeyException
import com.charleskorn.kaml.ForbiddenAnchorOrAliasException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlException
import com.charleskorn.kaml.YamlInput
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlNull
import com.charleskorn.kaml.YamlPathSegment
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
    val root =
        try {
            Yaml.default.parseToYamlNode(yaml)
        } catch (reference: ForbiddenAnchorOrAliasException) {
            throw WorkCorpus.WorkShapeError.YamlReference(referencePlace(reference.path.segments), reference)
        } catch (repeated: DuplicateKeyException) {
            val key =
                repeated.duplicatePath.segments
                    .filterIsInstance<YamlPathSegment.MapElementKey>()
                    .lastOrNull()
                    ?.key
            throw WorkCorpus.WorkShapeError.RepeatedKey(key ?: repeated.key, repeated)
        } catch (failure: YamlException) {
            val segments = failure.path.segments
            val merge = segments.indexOfFirst(::isMerge)
            if (merge >= 0) throw WorkCorpus.WorkShapeError.YamlReference(referencePlace(segments.take(merge)), failure)
            throw WorkCorpus.DocumentError("The work cannot be read as YAML: ${failure.message}", failure)
        }
    return yamlNode(root, place = "")
}

private fun isMerge(segment: YamlPathSegment): Boolean =
    segment is YamlPathSegment.Merge || (segment is YamlPathSegment.MapElementKey && segment.key == "<<")

internal fun treeOf(decoder: Decoder): Node =
    when (decoder) {
        is YamlInput -> yamlNode(decoder.node, place = "")
        is JsonDecoder -> jsonNode(decoder.decodeJsonElement(), place = "")
        else -> throw WorkCorpus.DocumentError("A work is read from YAML or JSON, not from ${decoder::class.simpleName}.")
    }

private fun yamlNode(
    node: YamlNode,
    place: String,
): Node {
    val segments = node.path.segments
    val merge = segments.indexOfFirst { it is YamlPathSegment.Merge }
    if (merge >= 0) throw WorkCorpus.WorkShapeError.YamlReference(referencePlace(segments.take(merge)))
    return when (node) {
        is YamlNull -> Node.Null(place)
        is YamlScalar -> Node.Scalar(place, node.content, bare = node.plain, json = false)
        is YamlList -> Node.Sequence(place, node.items.mapIndexed { index, item -> yamlNode(item, "$place[$index]") })
        is YamlMap ->
            Node.Mapping(
                place,
                node.entries.entries.associate { (key, value) ->
                    yamlNode(key, place)
                    key.content to yamlNode(value, within(place, key.content))
                },
            )
        is YamlTaggedNode -> yamlNode(node.innerNode, place)
    }
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

private fun referencePlace(segments: List<YamlPathSegment>): String =
    segments
        .joinToString("") { segment ->
            when (segment) {
                is YamlPathSegment.ListEntry -> "[${segment.index}]"
                is YamlPathSegment.MapElementKey -> ".${segment.key}"
                else -> ""
            }
        }.removePrefix(".")
        .ifEmpty { "top level" }
