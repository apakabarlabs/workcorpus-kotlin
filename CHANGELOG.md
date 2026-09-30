# Changelog

WorkCorpus reads the text a read-aloud practice app is built on, such as a book
of poems, from a YAML or JSON file: what is read, how it is divided for practice,
and the app's settings for it. This is the Kotlin/JVM version of
[workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift), with the
same behaviour and, where Kotlin allows, the same names.

Terms used below. A *work* is the whole text, such as Shakespeare's sonnets. A
*piece* is what a reader practises in one sitting: a sonnet, a stanza, a scene. A
*part* is a run of consecutive pieces, such as a chapter or an act. A *stage* is
how much of a piece is attempted at once: `line`, one line, or `block`, a group of
lines. A piece's *cuts* say how many lines each `block` group has. A work comes as
a *work file*, sections holding pieces as an author writes them, as an assembled
*book*, the flat form with `pieces`, `parts`, `stage_field` and `difficult_words`,
or is built in code with `WorkCorpus.work` from values the app already holds.

## 0.7.0

Nothing changes in the Kotlin library. The version follows workcorpus-swift 0.7.0,
whose changes concern Swift only, so that the two ports keep the same major and
minor version for the same behaviour.

## 0.6.0

### Added

- `Work.interiorMarks`, the characters that stay inside a word once the word has
  begun, such as the apostrophe in `beauty’s` and the hyphen in `self-love`. It is
  read from the work's `interior_marks` key as one text, each character of which
  is a mark: `"'’-"` for Shakespeare's sonnets. `""` names none.
- `Work.elisions`, a `Map<String, List<String>>` of the shortened spellings the
  work prints, each mapped to the full forms a speech recogniser writes for it,
  such as `tatter’d` to `tattered`. It is read from the work's `elisions` key, a
  mapping from a printed spelling to a list of full forms; one spelling may have
  several, kept in the order written. `{}` names none.
- Both come with the work rather than from a rule about English in the library,
  so a book in another language names its own. Marks, spellings and full forms
  are kept exactly as written: WorkCorpus only carries them. What a mark does at
  the start or end of a word, and whether case and punctuation count when a full
  form is compared with what was heard, is decided by the code that uses them,
  such as `WordTokenizer` and `Elisions` in
  [ReadAloudKit](https://github.com/apakabarlabs/readaloudkit-kotlin).

### Changed

- Breaking: a work file, a book and a JSON work need both keys, beside
  `language`; `decodeWork`, `decodeWorkFromBook` and `Work.serializer()` refuse
  one without either with `WorkCorpus.DocumentError`, such as "The work's
  interior_marks is missing." A work with no such marks or elisions writes them
  empty:

  ```yaml
  # 0.5
  slug: sonnets
  language: eng
  title: Sonnets
  # 0.6
  slug: sonnets
  language: eng
  interior_marks: "'’-"
  elisions:
    tatter’d: [tattered]
    th’: [the]
  title: Sonnets
  # 0.6, a work with neither
  slug: sonnets
  language: eng
  interior_marks: ""
  elisions: {}
  title: Sonnets
  ```
- Breaking: `WorkCorpus.work` takes the marks and the elisions after the
  language, since values held in code have no file to read them from:

  ```kotlin
  // 0.5
  WorkCorpus.work(language = "eng", pieces = pieces, reading = reading)
  // 0.6
  WorkCorpus.work(
      language = "eng",
      interiorMarks = "'’-",
      elisions = mapOf("tatter’d" to listOf("tattered"), "th’" to listOf("the")),
      pieces = pieces,
      reading = reading,
  )
  ```

  Reading a work does not change, apart from the new keys the document needs:

  ```kotlin
  // 0.5 and 0.6
  val work = WorkCorpus.decodeWork(yaml)
  // 0.6
  val marks = work.interiorMarks
  val fullForms = work.elisions["tatter’d"].orEmpty()
  ```
- The two keys are held to the rules the other fields are. `interior_marks`
  written as null — `null`, `~`, or nothing after the key — is refused with
  `WorkShapeError.NullText`, naming `interior_marks`, and so is a full form
  written as null, naming it, such as `elisions.th’[0]`. A missing key is
  refused with `DocumentError`, and so is a value of another kind: interior marks
  given as a list, or in JSON as a number; elisions given as null, left empty or
  as a list; the full forms of a spelling given as one text rather than a list,
  or as null. A spelling named twice in one YAML mapping is refused with
  `WorkShapeError.RepeatedKey`, naming it; in one JSON object, as for any key,
  one of its values is kept.
- Where workcorpus-swift 0.6.0 throws Swift's `DecodingError` for these keys,
  this library throws `WorkCorpus.DocumentError`, as it does for every other
  field.

## 0.5.0

### Added

- First release, on Maven Central as `fm.apakabar:workcorpus-kotlin`, for JDK 17
  or later:

  ```kotlin
  implementation("fm.apakabar:workcorpus-kotlin:0.5.0")
  ```

  It reads YAML with kotaml, a fork of kaml that keeps kaml's package
  `com.charleskorn.kaml`, so do not also depend on `com.charleskorn.kaml:kaml`.
- The API of workcorpus-swift 0.5.0: `WorkCorpus.decodeWork` reads a work file,
  `WorkCorpus.decodeWorkFromBook` a book, and `WorkCorpus.work(language, pieces,
  reading)` builds a work from `HeldPiece` and `HeldReading` values. A `Work`
  gives its `language`, its pieces and their cuts for each `ReadingStage`, its
  parts, its `free` pieces (those readable without purchase), its progress bands
  (`StageFieldScale`) and the score at which a word counts as difficult.
  `PieceAsset` names a work's recordings, word-timing files and shared attempts,
  and reads piece numbers and narration voices back from those names.
- Errors are nested in `WorkCorpus` and named below without that prefix:
  `WorkShapeError` when part of a work is malformed, `CorpusError` when pieces
  are not numbered from one in order, `WorkError` when a work-file piece id is
  not a number, and `DocumentError` when a document cannot be read as a work.
  Each names what is wrong and where.
- A work is held to the same rules as in workcorpus-swift 0.5.0:
  - `language` must be a language tag: two or three lowercase letters,
    optionally followed by `-` and subtags of two to eight ASCII letters or
    digits, such as `en`, `eng` or `en-GB`. Two- and three-letter codes are
    equally accepted and kept as written. Any other value is refused with
    `WorkShapeError.InvalidLanguage`, which names the value.
  - A piece's `block` sizes must each be at least one and add up to exactly the
    piece's number of lines, and cuts for `line` or for a stage the library does
    not know are refused. The errors, `WorkShapeError.CutsDoNotCoverThePiece`,
    `WorkShapeError.EmptyCut`, `WorkShapeError.CutsForLineStage` and
    `WorkShapeError.CutsForUnknownStage`, name the piece and the stage.
    A piece without `cuts`, or with `cuts: null`, is read line by line.
  - A `Part` that starts before piece one or ends before it starts, and a work
    whose last part runs past its last piece, are refused with
    `WorkShapeError.PartOutOfRange`. Pieces not numbered from one in order are
    refused with `CorpusError.OutOfOrder` before their parts are checked.
  - The `free` list must be non-empty, name no piece twice and name only pieces
    of the work, or it is refused with `WorkShapeError.InvalidFreePieces`; so a
    work without pieces is refused too, since none of its pieces can be free.
  - Every whole number in a work — piece numbers, cut sizes, a part's `first`
    and `last`, the `free` pieces and the difficult-word score — must be a YAML
    integer that fits in 32 bits, written as plain decimal digits with an
    optional `-` and no leading zero. A larger value, a quoted string, a float, a
    boolean, null, a list or a mapping, or a number written with `+`, a leading
    zero, as `-0`, with underscores, `0x`/`0o`/`0b` or as sexagesimal `1:30` is
    refused with `WorkShapeError.InvalidNumber`, naming the field, such as
    `pieces[0].cuts.block[1]`. A work file writes piece ids and `free` entries as
    strings, such as `'3'`; one that is not written that way within 32 bits, such
    as `'+3'`, `'03'` or `'-0'`, is refused with `WorkError.PieceIsNotNumbered`.
    `PieceAsset.number` returns `null` for a file name whose number does not fit
    in 32 bits.
  - The progress bounds — `stage_field.untouched_below`, `begun_below` and
    `most_below` in a book, `reading.*_below` in a work file, which split a
    stage's completion into bands — must be written in YAML as plain decimal
    digits with an optional `-` and fractional part, such as `0.001` or `1`. A
    quoted value, a boolean, null, `0.5_0`, `.5`, `5e-1` or sexagesimal `1:00` is
    refused with `WorkShapeError.InvalidFraction`, naming the field.
  - A text a work needs — its language, a piece's title, id and lines, a part's
    title and summary, a work file's `slug` and `title` — is refused with
    `WorkShapeError.NullText`, naming the field, when it is YAML null: `null`,
    `Null`, `NULL`, `~`, or nothing after its key or dash. Write empty text as
    `""`, as for a blank line of a poem. A null `short` (a part's short title), a
    null `summary` of a work-file section and a null `cuts` read as none.
  - `decodeWork` and `decodeWorkFromBook` refuse an anchor, an alias or a `<<`
    merge key, quoted or not, with `WorkShapeError.YamlReference`, naming the
    anchored value or the merging mapping, such as `parts[0]`; an alias with no
    anchor, such as `*nowhere`, names its field, or the enclosing mapping when
    the alias is a key. They refuse an explicit tag — `!!str 3`, `!!int 3`,
    `!poem`, `!`, or a tag on a list or mapping — with
    `WorkShapeError.ExplicitTag`, naming the field, and a key repeated in one
    mapping with `WorkShapeError.RepeatedKey`, naming the key. Of several such
    problems the first in the document is reported, even when a syntax error
    follows it. A key the work does not know is skipped, in YAML and JSON alike.
  - `Work`, `Piece`, `Part`, `StageFieldScale` and `DifficultWordsConfiguration`
    are `@Serializable`. Decoded from JSON with kotlinx.serialization, `Work`
    holds its numbers, progress bounds and texts to the rules above; the
    YAML-only rules and the checks between fields, such as parts covering the
    pieces, apply only through `decodeWork` and `decodeWorkFromBook`. A JSON
    number is judged by its value: a whole number within 32 bits reads, so `5.0`
    reads as 5 and `1e2` as 100, while a string such as `"5"`, a boolean, null, a
    fraction, a value past 32 bits or `-0` is refused with
    `WorkShapeError.InvalidNumber`, naming the field. The value is read exactly,
    not through a binary float, so `5.000000000000000001` and
    `4.9999999999999999999` are fractions and refused. A number of 38 or more
    significant digits, trailing zeros aside, is refused: a whole number with
    `InvalidNumber`, a progress bound with `InvalidFraction`. A progress bound a
    `Double` cannot hold, such as `1e-400` or `1e400`, is refused with
    `InvalidFraction`. A key repeated in one JSON object is not refused; one of
    its values is kept.
  - `PieceAsset` compares its stem with a file name in Unicode normalization form
    C, and reads only the ASCII digits `0` to `9` as digits, both in a piece
    number and when telling a narration voice suffix, such as `onyx` in
    `s-001-onyx.json`, from a number.
- A work file or book of any length is read: the default limit of 3,145,728 code
  points that kaml and snakeyaml-engine put on a document does not apply.
- Where this library differs from workcorpus-swift 0.5.0:
  - It throws `WorkCorpus.DocumentError` where the Swift library throws Swift's
    `DecodingError`: for YAML the parser cannot read, a missing field, a value of
    the wrong kind, and a key that is a list or a mapping. A JSON syntax error is
    kotlinx.serialization's own `SerializationException`.
  - A JSON number is judged by all its digits, so
    `5.00000000000000000000000000000000000001` is refused; the Swift library
    reads it as 5, because Foundation first cuts it to its first 38 digits.
  - `!!str` on a quoted value, and `!!str` or `!` on a key, are refused with
    `ExplicitTag`; the Swift library reads them as untagged.
  - Keys that differ only in how their accented letters are composed in Unicode
    count as two keys; the Swift library counts them as one.
  - Of several keys repeated on one line, in a flow mapping `{…}`, the first
    written is named; the Swift library names the one whose name sorts first.
    Inside a flow collection the first problem is always the one reported; the
    Swift library may name a later one.
  - The Swift library's `PieceStanding` (a reader's progress through the stages
    of one piece) and `Piece.passage` (a piece's lines for the read-aloud
    checker) are not included, since both are built on a Swift-only library.
