package fm.apakabar.workcorpus

import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.api.copy
import it.krzeminski.snakeyaml.engine.kmp.api.lowlevel.Parse
import it.krzeminski.snakeyaml.engine.kmp.events.AliasEvent
import it.krzeminski.snakeyaml.engine.kmp.events.CollectionEndEvent
import it.krzeminski.snakeyaml.engine.kmp.events.CollectionStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.DocumentStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.Event
import it.krzeminski.snakeyaml.engine.kmp.events.MappingStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.NodeEvent
import it.krzeminski.snakeyaml.engine.kmp.events.ScalarEvent
import it.krzeminski.snakeyaml.engine.kmp.exceptions.YamlEngineException

// Kotlin compiles LoadSettings() to the synthetic constructor that takes every setting,
// whose signature snakeyaml-engine-kmp 5.0.0 changed; the no-argument one is in 4 and 5.
private val UNLIMITED =
    LoadSettings::class.java
        .getConstructor()
        .newInstance()
        .copy { codePointLimit = Int.MAX_VALUE }

private val CORE_SCHEMA_NULLS = setOf("", "~", "null", "Null", "NULL")

private class Collection(
    val place: String,
    val mapping: Boolean,
) {
    val entries = LinkedHashMap<String, Node>()
    val items = ArrayList<Node>()
    var key: String? = null

    val awaitingKey: Boolean get() = mapping && key == null

    fun placeOfNext(): String = if (mapping) within(place, checkNotNull(key)) else "$place[${items.size}]"

    fun add(node: Node) {
        if (mapping) {
            entries[checkNotNull(key)] = node
            key = null
        } else {
            items += node
        }
    }

    fun closed(): Node = if (mapping) Node.Mapping(place, entries) else Node.Sequence(place, items)
}

private class YamlTree {
    private val open = ArrayDeque<Collection>()
    var root: Node? = null
        private set

    fun visit(event: Event) {
        when (event) {
            is DocumentStartEvent ->
                if (root != null) throw WorkCorpus.DocumentError("The work cannot be read as YAML: a second document follows it.")
            is CollectionEndEvent -> placed(open.removeLast().closed())
            is NodeEvent -> visitNode(event)
            else -> Unit
        }
    }

    private fun visitNode(event: NodeEvent) {
        val parent = open.lastOrNull()
        if (parent != null && parent.awaitingKey) return visitKey(event, parent)
        val place = parent?.placeOfNext().orEmpty()
        val shown = place.ifEmpty { "top level" }
        if (event is AliasEvent || event.anchor != null) throw WorkCorpus.WorkShapeError.YamlReference(shown)
        if (isTagged(event)) throw WorkCorpus.WorkShapeError.ExplicitTag(shown)
        when (event) {
            is CollectionStartEvent -> open.addLast(Collection(place, mapping = event is MappingStartEvent))
            is ScalarEvent -> placed(scalar(event, place))
        }
    }

    private fun visitKey(
        event: NodeEvent,
        mapping: Collection,
    ) {
        val shown = mapping.place.ifEmpty { "top level" }
        if (event is AliasEvent || event.anchor != null) throw WorkCorpus.WorkShapeError.YamlReference(shown)
        if (event !is ScalarEvent) throw WorkCorpus.DocumentError("The work's $shown has a key that is not text.")
        if (event.value == "<<") throw WorkCorpus.WorkShapeError.YamlReference(shown)
        if (isTagged(event)) throw WorkCorpus.WorkShapeError.ExplicitTag(within(mapping.place, event.value))
        if (event.value in mapping.entries) throw WorkCorpus.WorkShapeError.RepeatedKey(event.value)
        mapping.key = event.value
    }

    private fun placed(node: Node) {
        val parent = open.lastOrNull()
        if (parent == null) root = node else parent.add(node)
    }
}

internal fun parseYaml(yaml: String): Node {
    val tree = YamlTree()
    try {
        for (event in Parse(UNLIMITED).parse(yaml)) tree.visit(event)
    } catch (unreadable: YamlEngineException) {
        throw WorkCorpus.DocumentError("The work cannot be read as YAML: ${unreadable.message}", unreadable)
    }
    return tree.root ?: Node.Null(place = "")
}

private fun scalar(
    event: ScalarEvent,
    place: String,
): Node =
    if (event.plain && event.value in CORE_SCHEMA_NULLS) {
        Node.Null(place)
    } else {
        Node.Scalar(place, event.value, bare = event.plain, json = false)
    }

private fun isTagged(event: NodeEvent): Boolean =
    when (event) {
        is ScalarEvent -> event.tag != null
        is CollectionStartEvent -> !event.implicit
        else -> false
    }
