package fm.apakabar.workcorpus

import com.charleskorn.kaml.ForbiddenAnchorOrAliasException
import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import com.charleskorn.kaml.YamlList
import com.charleskorn.kaml.YamlMap
import com.charleskorn.kaml.YamlNode
import com.charleskorn.kaml.YamlPathSegment
import com.charleskorn.kaml.YamlTaggedNode
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal val workYaml = Yaml(configuration = YamlConfiguration(strictMode = false))

internal fun <T> decodeYaml(
    deserializer: DeserializationStrategy<T>,
    yaml: String,
): T {
    val root =
        try {
            workYaml.parseToYamlNode(yaml)
        } catch (reference: ForbiddenAnchorOrAliasException) {
            throw WorkCorpus.WorkShapeError.YamlReference(referencePlace(reference.path.segments), reference)
        }
    refuseMerges(root)
    return workYaml.decodeFromYamlNode(deserializer, root)
}

private fun refuseMerges(node: YamlNode) {
    val segments = node.path.segments
    val merge = segments.indexOfFirst { it is YamlPathSegment.Merge }
    if (merge >= 0) throw WorkCorpus.WorkShapeError.YamlReference(referencePlace(segments.take(merge)))
    when (node) {
        is YamlMap ->
            node.entries.forEach { (key, value) ->
                refuseMerges(key)
                refuseMerges(value)
            }
        is YamlList -> node.items.forEach(::refuseMerges)
        is YamlTaggedNode -> refuseMerges(node.innerNode)
        else -> Unit
    }
}

private fun referencePlace(segments: List<YamlPathSegment>): String = place(segments).ifEmpty { "top level" }

internal fun place(segments: List<YamlPathSegment>): String =
    segments
        .joinToString("") { segment ->
            when (segment) {
                is YamlPathSegment.ListEntry -> "[${segment.index}]"
                is YamlPathSegment.MapElementKey -> ".${segment.key}"
                else -> ""
            }
        }.removePrefix(".")

@Serializable
internal data class WorkFile(
    val slug: String,
    val language: String,
    val title: String,
    val reading: WorkReading,
    val sections: List<WorkSection>,
)

@Serializable
internal data class WorkReading(
    @SerialName("untouched_below") val untouchedBelow: Double,
    @SerialName("begun_below") val begunBelow: Double,
    @SerialName("most_below") val mostBelow: Double,
    @SerialName("difficult_word_score")
    @Serializable(with = WholeNumberSerializer::class)
    val difficultWordScore: Int,
    val free: List<String>,
)

@Serializable
internal data class WorkSection(
    val title: String,
    val short: String? = null,
    val summary: String? = null,
    val sections: List<WorkSection>? = null,
    val pieces: List<WorkPiece>? = null,
)

@Serializable
internal data class WorkPiece(
    val id: String,
    val title: String,
    val lines: List<String>,
    @Serializable(with = CutsSerializer::class)
    val cuts: Map<String, List<Int>> = emptyMap(),
)

internal fun assembleWork(yaml: String): Work {
    val work = decodeYaml(WorkFile.serializer(), yaml)
    val pieces =
        parts(work.sections).flatMap { part ->
            part.pieces.orEmpty().map { piece ->
                HeldPiece(
                    number = numbered(piece.id),
                    title = piece.title,
                    lines = piece.lines,
                    partTitle = part.title,
                    partShort = part.short,
                    partSummary = part.summary ?: "",
                    cutSizes = piece.cuts,
                )
            }
        }
    return assemble(
        language = work.language,
        pieces = pieces,
        reading =
            HeldReading(
                untouchedBelow = work.reading.untouchedBelow,
                begunBelow = work.reading.begunBelow,
                mostBelow = work.reading.mostBelow,
                difficultWordScore = work.reading.difficultWordScore,
                free = work.reading.free.map(::numbered),
            ),
    )
}

private fun numbered(id: String): Int =
    id.takeIf(WholeNumberSerializer::isPlainDecimal)?.toIntOrNull()
        ?: throw WorkCorpus.WorkError.PieceIsNotNumbered(id)

private fun parts(sections: List<WorkSection>): List<WorkSection> =
    sections.flatMap { section ->
        if (!section.pieces.isNullOrEmpty()) listOf(section) else parts(section.sections.orEmpty())
    }
