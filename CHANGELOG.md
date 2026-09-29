# Changelog

## 0.5.0

Follows [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift) 0.5.0.

### Added

- `Work.language`: the language the work names itself as written in. It is read
  from the `language` key of a work file or an assembled book, and a work that
  does not name one, or names a blank one, is refused.

### Changed

- `WorkCorpus.work` takes the work's language first, since a held work has no
  file to read it from:

  ```kotlin
  // 0.4
  WorkCorpus.work(pieces, reading)
  // 0.5
  WorkCorpus.work(language, pieces, reading)
  ```
- An assembled book needs a `language` key; one without it no longer decodes.
- A work's `cuts` table is validated. A cut of zero or fewer lines, cuts whose
  sizes add up to more lines than the piece has, cuts for a stage that does not
  exist, and cuts for the `line` stage are refused, naming the piece and the
  stage. Cuts that add up to fewer lines than the piece has are still accepted,
  and the lines after them are read as one more cut.

## 0.4.0

- First release of the Kotlin/JVM port of
  [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift) 0.4.0, with
  the same names and behaviour: a work and its pieces, parts, cuts, reading
  stages and progress bands, read from the work file or the assembled book, or
  assembled from values a caller already holds.
- The Swift `Piece.passage` and `PieceStanding` are not ported. Both are built on
  ReadAloudKit types, `Passage` and `StageState`, which have no Kotlin port to
  take them from.
