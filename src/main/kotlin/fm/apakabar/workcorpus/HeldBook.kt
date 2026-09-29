package fm.apakabar.workcorpus

/**
 * Piece and part values already held by a caller, before assembling a [Work].
 *
 * @property number One-based piece number.
 * @property title Piece title.
 * @property lines Printed lines in reading order.
 * @property partTitle Full title of the containing part.
 * @property partShort Optional compact title of the containing part.
 * @property partSummary Summary of the containing part.
 * @property cutSizes Per-stage sizes of consecutive line groups.
 */
data class HeldPiece(
    val number: Int,
    val title: String,
    val lines: List<String>,
    val partTitle: String,
    val partShort: String?,
    val partSummary: String,
    val cutSizes: Map<String, List<Int>> = emptyMap(),
)

/**
 * Reading configuration already held by a caller before assembling a [Work].
 *
 * @property untouchedBelow Upper bound of the untouched progress band.
 * @property begunBelow Upper bound of the begun progress band.
 * @property mostBelow Upper bound of the mostly-complete progress band.
 * @property difficultWordScore Difficult-word score threshold.
 * @property free Piece numbers available without purchase.
 */
data class HeldReading(
    val untouchedBelow: Double,
    val begunBelow: Double,
    val mostBelow: Double,
    val difficultWordScore: Int,
    val free: List<Int>,
)

private data class OpenPart(
    val title: String,
    val short: String?,
    val summary: String,
    val first: Int,
    val last: Int,
)

internal fun assemble(
    pieces: List<HeldPiece>,
    reading: HeldReading,
): Work =
    Work(
        pieces = pieces.map { Piece(number = it.number, title = it.title, lines = it.lines, cutSizes = it.cutSizes) },
        parts =
            assembleParts(pieces).map {
                Part(title = it.title, summary = it.summary, first = it.first, last = it.last, short = it.short)
            },
        free = reading.free,
        stageField =
            StageFieldScale(
                untouchedBelow = reading.untouchedBelow,
                begunBelow = reading.begunBelow,
                mostBelow = reading.mostBelow,
            ),
        difficultWords = DifficultWordsConfiguration(scoreThreshold = reading.difficultWordScore),
    )

private fun assembleParts(pieces: List<HeldPiece>): List<OpenPart> {
    val parts = mutableListOf<OpenPart>()
    for (piece in pieces) {
        val open = parts.lastOrNull()
        if (open != null && open.title == piece.partTitle && open.last + 1 == piece.number) {
            parts[parts.lastIndex] = open.copy(last = piece.number)
        } else {
            parts +=
                OpenPart(
                    title = piece.partTitle,
                    short = piece.partShort,
                    summary = piece.partSummary,
                    first = piece.number,
                    last = piece.number,
                )
        }
    }
    return parts
}
