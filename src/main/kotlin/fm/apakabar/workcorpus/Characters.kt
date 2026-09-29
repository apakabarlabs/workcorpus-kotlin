package fm.apakabar.workcorpus

private val USER_PERCEIVED_CHARACTER = Regex("""\X""")

private val NUMERIC_CATEGORIES =
    setOf(
        Character.DECIMAL_DIGIT_NUMBER.toInt(),
        Character.LETTER_NUMBER.toInt(),
        Character.OTHER_NUMBER.toInt(),
    )

internal fun String.characters(): Sequence<String> = USER_PERCEIVED_CHARACTER.findAll(this).map { it.value }

internal fun String.isLetter(): Boolean = isNotEmpty() && Character.isAlphabetic(codePointAt(0))

internal fun String.isNumber(): Boolean = isNotEmpty() && Character.getType(codePointAt(0)) in NUMERIC_CATEGORIES
