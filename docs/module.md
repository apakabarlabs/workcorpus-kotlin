# WorkCorpus for Kotlin

Decode a portable reading work, validate its shape, and derive the pieces, stages
and assets a reading application needs.

Use `WorkCorpus.decodeWork` for the nested work-file format or
`WorkCorpus.decodeWorkFromBook` for the assembled book format. Both validate
numbering, parts, free pieces, progress thresholds, the language the work names
and the cuts each piece is taken in before returning a `Work`. The cuts of a stage
have to add up to exactly the lines of their piece: a cut of no lines, cuts longer
or shorter than their piece, and cuts for a stage that is not a cut reading stage
are refused with the piece and the stage named. A stage the work says nothing
about is read line by line. A work is written out in full: YAML anchors, aliases
and `<<` merge keys are refused, naming where they stand, and so is a key named
twice in one mapping. These YAML rules hold through `decodeWork` and
`decodeWorkFromBook`, which parse the YAML themselves; decoding `Work` with a `Yaml`
of the caller's leaves them to that `Yaml`. `WorkCorpus.work` assembles a work
from its language and the `HeldPiece` and `HeldReading` values a caller already
holds, and validates it the same way.

The nested work-file format groups pieces under sections and keeps reading settings
in one `reading` mapping:

```yaml
slug: poems
language: eng
title: Poems
reading:
  untouched_below: 0.001
  begun_below: 0.5
  most_below: 1.0
  difficult_word_score: 3
  free: ['1']
sections:
  - title: Opening poems
    summary: The first part.
    pieces:
      - id: '1'
        title: First poem
        lines: [The first line.]
        cuts:
          block: [1]
```

The assembled book format supplies the resulting pieces and parts directly:

```yaml
language: eng
pieces:
  - number: 1
    title: First poem
    lines: [The first line.]
parts:
  - title: Opening poems
    summary: The first part.
    first: 1
    last: 1
free: [1]
stage_field:
  untouched_below: 0.001
  begun_below: 0.5
  most_below: 1.0
difficult_words:
  score_threshold: 3
```

`Piece.cuts` gives the line ranges of one attempt at a `ReadingStage`, as the work
names them. `PieceAsset` gives recordings, alignments and shared attempts
deterministic names, and `NarrationVoice` identifies the voice those names carry.
