package fm.apakabar.workcorpus

private class WorkSection(
    val title: String,
    val short: String?,
    val summary: String?,
    val sections: List<WorkSection>?,
    val pieces: List<WorkPiece>?,
)

private class WorkPiece(
    val id: String,
    val title: String,
    val lines: List<String>,
    val cuts: Map<String, List<Int>>,
)

internal fun assembleWork(yaml: String): Work {
    val work = parseYaml(yaml).fields()
    work["slug"].text()
    val writing = readWriting(work)
    work["title"].text()
    val reading = work["reading"].fields()
    val untouchedBelow = reading["untouched_below"].fraction()
    val begunBelow = reading["begun_below"].fraction()
    val mostBelow = reading["most_below"].fraction()
    val difficultWordScore = reading["difficult_word_score"].whole()
    val free = reading["free"].items().map { it.text() }
    val sections = work["sections"].items().map(::readSection)
    val pieces =
        parts(sections).flatMap { part ->
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
        writing = writing,
        pieces = pieces,
        reading =
            HeldReading(
                untouchedBelow = untouchedBelow,
                begunBelow = begunBelow,
                mostBelow = mostBelow,
                difficultWordScore = difficultWordScore,
                free = free.map(::numbered),
            ),
    )
}

private fun readSection(node: Node): WorkSection {
    val fields = node.fields()
    return WorkSection(
        title = fields["title"].text(),
        short = fields.optional("short")?.text(),
        summary = fields.optional("summary")?.text(),
        sections = fields.optional("sections")?.items()?.map(::readSection),
        pieces = fields.optional("pieces")?.items()?.map(::readWorkPiece),
    )
}

private fun readWorkPiece(node: Node): WorkPiece {
    val fields = node.fields()
    return WorkPiece(
        id = fields["id"].text(),
        title = fields["title"].text(),
        lines = fields["lines"].items().map { it.text() },
        cuts = readCuts(fields.optional("cuts")),
    )
}

private fun numbered(id: String): Int =
    id.takeIf(::isPlainDecimal)?.toIntOrNull()
        ?: throw WorkCorpus.WorkError.PieceIsNotNumbered(id)

private fun parts(sections: List<WorkSection>): List<WorkSection> =
    sections.flatMap { section ->
        if (!section.pieces.isNullOrEmpty()) listOf(section) else parts(section.sections.orEmpty())
    }
