package fm.apakabar.workcorpus

/**
 * The size of text a reader must complete in one attempt.
 *
 * @property rawValue Stable numeric value of the stage, also its position in stored progress.
 */
enum class ReadingStage(
    val rawValue: Int,
) {
    /** One printed line per attempt. */
    LINE(0),

    /** Work-defined groups of consecutive lines per attempt. */
    BLOCK(1),
    ;

    /** Stable numeric identity of the stage. */
    val id: Int get() = rawValue

    /** Key used by a work's `cuts` table. */
    val label: String
        get() =
            when (this) {
                BLOCK -> "block"
                LINE -> "line"
            }
}
