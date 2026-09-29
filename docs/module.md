# WorkCorpus for Kotlin

Decode a portable reading work, validate its shape, and derive the pieces, stages
and assets a reading application needs.

Use `WorkCorpus.decodeWork` for the nested work-file format or
`WorkCorpus.decodeWorkFromBook` for the assembled book format. Both validate
numbering, parts, free pieces and progress thresholds before returning a `Work`.
`WorkCorpus.work` assembles a work from `HeldPiece` and `HeldReading` values a
caller already holds, and validates it the same way.

The nested work-file format groups pieces under sections and keeps reading settings
in one `reading` mapping:

```yaml
slug: poems
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
