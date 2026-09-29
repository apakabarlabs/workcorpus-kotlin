# workcorpus-kotlin

[![Tests](https://github.com/apakabarlabs/workcorpus-kotlin/actions/workflows/tests.yml/badge.svg)](https://github.com/apakabarlabs/workcorpus-kotlin/actions/workflows/tests.yml)
[![Documentation](https://github.com/apakabarlabs/workcorpus-kotlin/actions/workflows/documentation.yml/badge.svg)](https://apakabarlabs.github.io/workcorpus-kotlin/)

Reads the work a reading exercise is built on: its text, how it is divided, and
what a reading of it is held to.

A work is written once, as one file, and read by everything that touches it —
the tool that records it, the tool that measures the recording, and the app a
reader holds. One reading of that file, in one place, is what keeps the three
from disagreeing about what is written.

This is a Kotlin/JVM port of [workcorpus-swift](https://github.com/apakabarlabs/workcorpus-swift),
with its behaviour and, where the languages allow, its names. One difference is
deliberate: where the lead lets Swift's own `DecodingError` report a document
that is not YAML, a missing field or a value of the wrong kind, this port reports
it as `WorkCorpus.DocumentError`, so that no error of its YAML parser reaches a
caller. The cases every port is held to live in the lead's
`work-cases.yaml`; they are synced from there with `make sync-yaml`, and a test
holds the copies against that repository, so the ports cannot quietly drift
apart.

## What it holds

- **A piece**, what a reader takes in one sitting: a sonnet, a stanza, a scene.
  It carries its own title, because only the work knows what to call it.
- **How a piece is cut** for a single attempt. The work says it; nothing here
  computes it, since how a sonnet falls into quatrains or a scene into speeches
  is the work's own shape. A stage the work says nothing about is read line by
  line.
- **A part**, a run of pieces the work is divided into.
- **What a reading is held to**: the score a word counts as difficult at and the
  bands a stage is coloured by.

What it does not hold is how long a piece should be, or how many pieces a work
has. Those belong to the work file, and a library that knew them could serve
only one book.

## Use

A work file names the work, its language and what a reading of it is held to,
and files its pieces under the sections they belong to:

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
        lines: [The first line., The second line., The third line.]
        cuts:
          block: [2, 1]
```

```kotlin
import fm.apakabar.workcorpus.ReadingStage
import fm.apakabar.workcorpus.WorkCorpus

val work = WorkCorpus.decodeWork(yaml)
val piece = work.pieces[0]
val firstBlock = piece.cuts(ReadingStage.BLOCK)[0]

check(work.language == "eng")
check(firstBlock == 0..1)
```

`decodeWork` reads a work file, `decodeWorkFromBook` an assembled book, and
`WorkCorpus.work(language, pieces, reading)` assembles values a caller already
holds. `Work.language` is the language tag the work gives, such as `en`, `eng`
or `en-GB`; nothing here assumes one.

Every number a work carries — piece numbers and identifiers, cut sizes, part
bounds, free pieces, the difficult-word threshold — is a whole number that fits
in 32 bits. In YAML it is written as plain decimal digits: `0`, or digits that
do not start with `0` after an optional `-`. In JSON it is a JSON number whose
value is whole, so `5.0` reads as 5, read from its literal and of fewer than 38
significant digits. The stage field bounds are fractions: in YAML plain decimal
digits with an optional `-` and fractional part, such as `0.001` or `1`; in
JSON a JSON number of fewer than 38 significant digits that a `Double` holds
without rounding it to zero or infinity.

`decodeWork` and `decodeWorkFromBook` refuse a malformed work with an error
that says what is wrong and where:

- a document that is not YAML, a missing field or a value of the wrong kind
  (`DocumentError`);
- a number not written or not valued as above (`InvalidNumber`), a piece or
  free-piece identifier that is not such a number (`PieceIsNotNumbered`), or a
  bound not written as above (`InvalidFraction`);
- a text the work needs left null (`NullText`), where YAML null is `null`,
  `Null`, `NULL`, `~` or nothing, and empty text is written `""`;
- a YAML anchor, alias or `<<` merge key (`YamlReference`), an explicit YAML tag
  such as `!!str 3` (`ExplicitTag`), or a key named twice in one mapping
  (`RepeatedKey`); of several of these, the one first in the document, even
  before a syntax error further on, and of several keys repeated on one line, as
  a flow mapping can, the first written, where the lead names the first in
  code-point order; a key that is a list or a mapping is a `DocumentError`;
- pieces not numbered from one in order (`OutOfOrder`), parts that do not cover
  the work exactly once or run past it, free pieces that are empty, repeated or
  outside the work, stage field bounds out of order, a threshold below one, a
  language that is not a language tag, or cuts that do not add up to the lines
  of their piece (`WorkShapeError`).

`WorkCorpus.work` holds values a caller already has to the rules of the last
item. `Work.serializer()` also reads a work from JSON with kotlinx.serialization,
holding its numbers, fractions and texts to the rules above, as the shared cases
test with the default `Json`; it checks no relationships between the fields, a
JSON syntax error reaches the caller as kotlinx.serialization's own error, and a
key repeated in one JSON object is not refused, the value kept possibly differing
from the lead's. The YAML rules hold only through `decodeWork` and
`decodeWorkFromBook`. Rare differences from the lead no case pins: keys equal
only under Unicode canonical equivalence count as one key there and as two here;
`!!str` written on a quoted value, or `!!str` or `!` on a key, is refused here
while the lead, which cannot see it, reads it as untagged; and within one flow
collection the lead may name a later problem before an earlier tag or anchor.

## Install

The library is published to Maven Central:

```kotlin
repositories {
    mavenCentral()
}

dependencies {
    implementation("fm.apakabar:workcorpus-kotlin:0.5.0")
}
```

The API is not settled before 1.0 and may change between minor versions.

The library reads YAML with [kotaml](https://github.com/Heapy/kotaml)
(`io.heapy.kotaml:kotaml`), a fork of kaml that keeps kaml's package,
`com.charleskorn.kaml`. A project that also depends on kaml itself
(`com.charleskorn.kaml:kaml`) gets two sets of the same classes on its classpath;
keep one of the two, or exclude one from the other's dependency. Neither appears
in this library's public API.

The library also depends directly on
[snakeyaml-engine-kmp](https://github.com/krzema12/snakeyaml-engine-kmp)
(`it.krzeminski:snakeyaml-engine-kmp`), whose parser events it reads to find the
first YAML problem in a work. kotaml brings the same library, and the version
declared here has to be the one the declared kotaml version requires; raising
one means raising the other to match.

## Documentation

The [Dokka API reference](https://apakabarlabs.github.io/workcorpus-kotlin/) is generated from the public Kotlin API and deployed by GitHub Actions.

## Develop

```bash
make test
make lint
make docs
make build
make sync-yaml   # after the work files change in workcorpus-swift
```

Releases are published by the [Release workflow](https://github.com/apakabarlabs/workcorpus-kotlin/actions/workflows/release.yml).

## Lines of Code

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="https://raw.githubusercontent.com/apakabarlabs/workcorpus-kotlin/main/.github/loc-history-dark.svg">
  <source media="(prefers-color-scheme: light)" srcset="https://raw.githubusercontent.com/apakabarlabs/workcorpus-kotlin/main/.github/loc-history-light.svg">
  <img alt="Lines of Code graph" src="https://raw.githubusercontent.com/apakabarlabs/workcorpus-kotlin/main/.github/loc-history-light.svg">
</picture>
