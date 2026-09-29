# Changelog

## 0.4.0

- First release of the Kotlin/JVM port of
  [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift) 0.4.0, with
  the same names and behaviour: a work and its pieces, parts, cuts, reading
  stages and progress bands, read from the work file or the assembled book, or
  assembled from values a caller already holds.
- The Swift `Piece.passage` and `PieceStanding` are not ported. Both are built on
  ReadAloudKit types, `Passage` and `StageState`, which have no Kotlin port to
  take them from.
