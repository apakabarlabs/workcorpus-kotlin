# Changelog

## 0.5.0

- First release: the Kotlin/JVM port of
  [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift) 0.5.0, with
  the same names and behaviour. It reads a work from its work file or assembled
  book, or assembles it from values a caller already holds, and gives its pieces,
  parts, cuts, reading stages and progress bands, and the language the work names.
- A work is refused, with a `WorkCorpus.WorkShapeError` or `WorkCorpus.CorpusError`
  that names what is wrong and where, when its pieces are not numbered from one in
  order, its parts do not cover it exactly once, its free pieces or thresholds are
  out of shape, its language is not a language tag such as `en`, `eng` or `en-GB`,
  or a piece's cuts do not add up to exactly the lines of that piece. A stage a
  work says nothing about is read line by line.
- Piece numbers and cut sizes are `Long`, as wide as the lead's `Int`. A cut size
  too large to count is taken as `Long.MAX_VALUE`, so it is refused naming the
  piece and the stage.
- `PieceAsset` compares a stem and a file name in Unicode normalization form C and
  reads only the ASCII digits `0` to `9` as the digits of a piece number.
- The lead's `Piece.passage` and `PieceStanding` are not ported yet. Both are built
  on ReadAloudKit types, `Passage` and `StageState`, which have no Kotlin port to
  take them from.
