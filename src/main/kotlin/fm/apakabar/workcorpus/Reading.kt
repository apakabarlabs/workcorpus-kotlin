package fm.apakabar.workcorpus

import java.math.BigDecimal

internal class Fields(
    private val mapping: Node.Mapping,
) {
    operator fun get(name: String): Node =
        mapping.entries[name] ?: throw WorkCorpus.DocumentError("The work's ${shown(within(mapping.place, name))} is missing.")

    fun optional(name: String): Node? = mapping.entries[name]?.takeUnless { it is Node.Null }

    val entries: Map<String, Node> get() = mapping.entries
}

internal fun Node.fields(): Fields = Fields(this as? Node.Mapping ?: throw mismatch("a mapping"))

internal fun Node.items(): List<Node> = (this as? Node.Sequence)?.items ?: throw mismatch("a list")

internal fun Node.text(): String =
    when {
        this is Node.Null -> throw WorkCorpus.WorkShapeError.NullText(shown(place))
        this is Node.Scalar && !(json && bare) -> text
        else -> throw mismatch("text")
    }

internal fun Node.whole(): Int {
    val scalar = this as? Node.Scalar
    val number =
        when {
            scalar == null || !scalar.bare -> null
            scalar.json -> exactWhole(scalar.text)
            isPlainDecimal(scalar.text) -> scalar.text.toIntOrNull()
            else -> null
        }
    return number ?: throw WorkCorpus.WorkShapeError.InvalidNumber(place)
}

internal fun Node.fraction(): Double {
    val scalar = this as? Node.Scalar
    val number =
        scalar
            ?.takeIf { it.bare }
            ?.text
            ?.toDoubleOrNull()
            ?.takeIf { it.isFinite() }
    return number ?: throw mismatch("a number")
}

internal fun isPlainDecimal(written: String): Boolean {
    val digits = written.removePrefix("-")
    return digits.isNotEmpty() && digits.all { it in '0'..'9' } && (digits[0] != '0' || written == "0")
}

private fun exactWhole(written: String): Int? {
    val value = written.toBigDecimalOrNull()?.stripTrailingZeros() ?: return null
    val negativeZero = value.signum() == 0 && written.startsWith("-")
    if (negativeZero || value.scale() > 0 || value !in INT_RANGE) return null
    return value.intValueExact()
}

private val INT_RANGE = BigDecimal(Int.MIN_VALUE)..BigDecimal(Int.MAX_VALUE)

private fun Node.mismatch(what: String) = WorkCorpus.DocumentError("The work's ${shown(place)} is not $what.")

private fun shown(place: String): String = place.ifEmpty { "top level" }

internal fun readWork(node: Node): Work {
    val fields = node.fields()
    return Work(
        language = fields["language"].text(),
        pieces = fields["pieces"].items().map(::readPiece),
        parts = fields["parts"].items().map(::readPart),
        free = fields["free"].items().map { it.whole() },
        stageField = readStageFieldScale(fields["stage_field"]),
        difficultWords = readDifficultWords(fields["difficult_words"]),
    )
}

internal fun readPiece(node: Node): Piece {
    val fields = node.fields()
    val cuts = readCuts(fields.optional("cuts"))
    return Piece(
        number = fields["number"].whole(),
        title = fields["title"].text(),
        lines = fields["lines"].items().map { it.text() },
        cutSizes = cuts,
    )
}

internal fun readCuts(node: Node?): Map<String, List<Int>> =
    node
        ?.fields()
        ?.entries
        ?.mapValues { (_, sizes) -> sizes.items().map { it.whole() } }
        .orEmpty()

internal fun readPart(node: Node): Part {
    val fields = node.fields()
    return Part(
        title = fields["title"].text(),
        summary = fields["summary"].text(),
        first = fields["first"].whole(),
        last = fields["last"].whole(),
        short = fields.optional("short")?.text(),
    )
}

internal fun readStageFieldScale(node: Node): StageFieldScale {
    val fields = node.fields()
    return StageFieldScale(
        untouchedBelow = fields["untouched_below"].fraction(),
        begunBelow = fields["begun_below"].fraction(),
        mostBelow = fields["most_below"].fraction(),
    )
}

internal fun readDifficultWords(node: Node): DifficultWordsConfiguration =
    DifficultWordsConfiguration(scoreThreshold = node.fields()["score_threshold"].whole())
