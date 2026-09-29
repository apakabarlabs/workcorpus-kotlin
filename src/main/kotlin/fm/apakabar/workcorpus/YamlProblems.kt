package fm.apakabar.workcorpus

import it.krzeminski.snakeyaml.engine.kmp.api.LoadSettings
import it.krzeminski.snakeyaml.engine.kmp.api.lowlevel.Parse
import it.krzeminski.snakeyaml.engine.kmp.events.AliasEvent
import it.krzeminski.snakeyaml.engine.kmp.events.CollectionEndEvent
import it.krzeminski.snakeyaml.engine.kmp.events.CollectionStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.Event
import it.krzeminski.snakeyaml.engine.kmp.events.MappingStartEvent
import it.krzeminski.snakeyaml.engine.kmp.events.NodeEvent
import it.krzeminski.snakeyaml.engine.kmp.events.ScalarEvent
import it.krzeminski.snakeyaml.engine.kmp.exceptions.YamlEngineException

private class Collection(
    val place: String,
    val mapping: Boolean,
) {
    val keys = HashSet<String>()
    var key: String? = null
    var index = 0

    val awaitingKey: Boolean get() = mapping && key == null

    fun next(): String =
        if (mapping) {
            within(place, checkNotNull(key)).also { key = null }
        } else {
            "$place[$index]".also { index += 1 }
        }
}

internal fun refuseYamlProblems(yaml: String) {
    val open = ArrayDeque<Collection>()
    try {
        for (event in Parse(LoadSettings(codePointLimit = Int.MAX_VALUE)).parse(yaml)) visit(event, open)
    } catch (unreadable: YamlEngineException) {
        return
    }
}

private fun visit(
    event: Event,
    open: ArrayDeque<Collection>,
) {
    if (event is CollectionEndEvent) {
        open.removeLast()
        return
    }
    if (event !is NodeEvent) return
    val parent = open.lastOrNull()
    if (parent != null && parent.awaitingKey) return visitKey(event, parent)
    val place = parent?.next().orEmpty()
    val shown = place.ifEmpty { "top level" }
    if (event is AliasEvent || event.anchor != null) throw WorkCorpus.WorkShapeError.YamlReference(shown)
    if (isTagged(event)) throw WorkCorpus.WorkShapeError.ExplicitTag(shown)
    if (event is CollectionStartEvent) open.addLast(Collection(place, mapping = event is MappingStartEvent))
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
    if (!mapping.keys.add(event.value)) throw WorkCorpus.WorkShapeError.RepeatedKey(event.value)
    mapping.key = event.value
}

private fun isTagged(event: NodeEvent): Boolean =
    when (event) {
        is ScalarEvent -> event.tag != null
        is CollectionStartEvent -> !event.implicit
        else -> false
    }
