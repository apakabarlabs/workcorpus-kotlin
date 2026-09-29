package fm.apakabar.workcorpus

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.YamlConfiguration
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

internal val workYaml = Yaml(configuration = YamlConfiguration(strictMode = false))

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
    val work = workYaml.decodeFromString(WorkFile.serializer(), yaml)
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

private fun numbered(id: String): Int = id.toStrictIntOrNull() ?: throw WorkCorpus.WorkError.PieceIsNotNumbered(id)

private fun String.toStrictIntOrNull(): Int? {
    val digits = if (startsWith("+") || startsWith("-")) substring(1) else this
    return if (digits.isNotEmpty() && digits.all { it in '0'..'9' }) toIntOrNull() else null
}

private fun parts(sections: List<WorkSection>): List<WorkSection> =
    sections.flatMap { section ->
        if (!section.pieces.isNullOrEmpty()) listOf(section) else parts(section.sections.orEmpty())
    }
