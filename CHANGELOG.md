# Changelog

## 0.5.0

- First release: the Kotlin/JVM port of
  [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift) 0.5.0, with
  its behaviour and, where the languages allow, its names; where the lead reports
  an unreadable document with Swift's `DecodingError`, this port reports
  `WorkCorpus.DocumentError`. It reads a work from its work file or assembled
  book, or assembles it from values a caller already holds, and gives its pieces,
  parts, cuts, reading stages and progress bands, and the language the work names.
- A work is refused, with a `WorkCorpus.WorkShapeError` or `WorkCorpus.CorpusError`
  that names what is wrong and where, when its pieces are not numbered from one in
  order, its parts do not cover it exactly once or a part runs past its last
  piece, its free pieces or thresholds are out of shape, its language is not a
  language tag such as `en`, `eng` or `en-GB`, or a piece's cuts do not add up to
  exactly the lines of that piece. A stage a work says nothing about is read line
  by line.
- Every number a work carries is a YAML integer that fits in 32 bits, written as
  plain decimal digits with an optional `-` and no leading zero, and read as an
  `Int`. A larger value, a quoted string, a float, a boolean, null, a list or a
  mapping in place of the number, or a number written with `+`, a leading zero, as
  `-0`, with underscores, `0x`/`0o`/`0b` or as sexagesimal `1:30` is refused with
  `WorkShapeError.InvalidNumber`, naming the field, such as
  `pieces[0].cuts.block[1]`. The piece and free-piece identifiers of a work file
  are held to the same writing and the same 32 bits, and one that is not, such as
  `'+3'`, `'03'` or `'-0'`, is refused with `WorkError.PieceIsNotNumbered`.
- A document that cannot be read as a work at all — not YAML or JSON, a field
  missing, or a list where text belongs — is refused with
  `WorkCorpus.DocumentError`, naming the field. No error of the YAML or JSON
  parser reaches a caller except as the cause of one of the library's own.
- A text a work needs — its language, a piece's title, identifier and lines, a
  part's title and summary, a work file's slug and title — is refused with
  `WorkShapeError.NullText`, naming the field, when it is null as the YAML 1.2
  core schema reads null: written as `null`, `Null`, `NULL` or `~`, or left empty
  after its key or dash. Empty text is written as `""`, as a
  blank line of a poem is. A null `short`, `summary` of a work-file section or
  `cuts` reads as none.
- The stage field bounds, `stage_field.*` in a book and `reading.*_below` in a
  work file, follow the one writing numbers follow: in YAML plain decimal digits
  with an optional `-` and fractional part, such as `0.001` or `1`; in JSON a JSON
  number. A quoted value, a boolean, null, or in YAML `0.5_0`, `.5`, `5e-1` or
  sexagesimal `1:00` is refused with `WorkShapeError.InvalidFraction`, naming the
  field, and so is a JSON bound of 38 or more significant digits or one a `Double`
  cannot hold, such as `1e-400` or `1e400`.
- A YAML mapping that names a key twice is refused with
  `WorkShapeError.RepeatedKey`, naming the key; of several, the key repeated first
  in the document is named. A key the work does not know is
  skipped, in YAML and in JSON alike, whatever the `Json` configuration says of
  unknown keys, since the library reads the JSON tree itself; the tests decode
  with the default `Json`. A key repeated in one JSON object is not refused:
  kotlinx.serialization keeps one of the values in the tree the library reads,
  as `JSONDecoder` does for the lead.
- `Work`, `Piece`, `Part`, `StageFieldScale` and `DifficultWordsConfiguration`
  are `@Serializable` with serializers of their own on the stable
  kotlinx.serialization API. Decoded from YAML through kaml or from JSON through
  kotlinx.serialization, they read the document's tree themselves and hold its
  numbers, fractions and texts to the rules above, whatever `Yaml` or `Json`
  configuration decodes them. The YAML rules — anchors, aliases, merge keys and
  repeated keys — hold only through `decodeWork` and `decodeWorkFromBook`, which
  parse the YAML themselves; a caller's own `Yaml` resolves or refuses anchors
  and repeated keys by its configuration and errors, and merge keys are refused
  either way. Nothing decoded directly is checked for relationships between its
  fields. A JSON
  number is held to its value, as the lead's `Decodable` holds it: a whole number
  within 32 bits reads, so `5.0` reads as 5, while a string such as `"5"`, a
  boolean, null, a fraction, a value past 32 bits or `-0` is refused with
  `WorkShapeError.InvalidNumber`, naming the field. The value is read as a decimal
  from the literal, and a JSON number of 38 or more significant digits is refused,
  as the lead's `Decimal` holds no more.
- A work file or book that gives a value a YAML anchor, takes one from an alias,
  or merges a mapping in with a `<<` key, quoted or not, is refused with
  `WorkShapeError.YamlReference`, naming the anchored value or the merging
  mapping, such as `parts[0]`, as the lead refuses it; a `<<` key with a scalar
  value is refused the same way.
- `PieceAsset` compares a stem and a file name in Unicode normalization form C and
  reads only the ASCII digits `0` to `9` as the digits of a piece number.
- The lead's `Piece.passage` and `PieceStanding` are not ported yet. Both are built
  on ReadAloudKit types, `Passage` and `StageState`, which have no Kotlin port to
  take them from.
