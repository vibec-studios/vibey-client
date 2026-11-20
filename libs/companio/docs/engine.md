# companio core

Everything that is not drawing, input or a game hook: plain Java 21 + Gson + the pure cores of cosmetico/r3/vui, no
Minecraft. Tested with JUnit in `src/test/java` (packs, expressions, brain, physics, pathfinding, animation, sync).

## Packages

| Package | Job | Main types |
|---|---|---|
| `pack` | Files of a pack (folder, zip, memory), limits, problems, hash, the loaded `Pack`, the `PackRegistry` (active packs, last good companions) | `PackFiles`, `DirFiles`, `ZipFiles`, `MapFiles`, `Problem(s)`, `PackHash`, `PackRegistry` |
| `parse` | JSON → definitions with validation (`file: path → message`, suggestions for typos) | `PackReader`, `CompanionReader`, `BehaviorReader`, `ActionReader`, `ModelReader`, … |
| `def` | Immutable definitions | `CompanionDef`, `BehaviorDef`, `Step`, `action/*`, `MovementDef`, `PropDef`, … |
| `expr` | The safe expression language: lexer, parser (depth ≤ 24, length ≤ 400), evaluator | `Expr`, `Scope`, `Names`, `Functions`, `Suggest` |
| `text` | Texts: literal / per language / `@key`, lang tables, sanitizing | `TextRef`, `Lang`, `Sanitize` |
| `sense` | What the backend reports each tick, and events derived from changes | `Senses`, `Owner`, `EventDetector`, `Variables` |
| `brain` | The behavior state machine: choosing, timers, actions, needs, memory | `Brain`, `BehaviorRun`, `BrainScope`, `ActionRunner`, `Needs`, `Memory` |
| `move` | Physics on a `BlockView`, A* pathfinding, movement modes | `Body`, `Physics`, `PathFinder`, `Terrain`, `Mover`, `Goals`, `Steering` |
| `anim` | Base animation cross-fade + layers into a cosmetico `Mix`; head look | `Animator`, `LookControl` |
| `world` | One live companion: brain + body + animator + props + bubble | `Companion`, `CompanionView`, `Output`, `Command` |
| `sync` | Snapshot codec, remote puppets | `Snapshot`, `SnapshotCodec`, `Puppet`, `RemoteCompanions` |
| `config` | Player settings (selected companion, active packs, needs per companion) | `CompanioConfig`, `ConfigCodec` |
| `tool` | `CheckPack` command line validator | `CheckPack` |

## Loading

`PackReader.read(files, gate, builtin)` reads `companion.json`, the lang files, every `.ogg` (checked), then each
companion: needs/vars first (they define the expression names), variants, the model (`ModelReader`: pack sounds are
added to the bundle as embedded sounds, every variant is the bundle JSON with replaced texture PNGs, parsed by
cosmetico's `Bundles.parse`), props (bone indices), sounds, particles (locators checked), behaviors (all expressions
compiled now) and tricks. A companion with any error is left out; the pack still loads the others.
`PackRegistry.replace` keeps the **last good** definition of a companion whose pack now has errors, so a typo while
editing never removes the companion – it shows the error and keeps the old version.

## A tick (20/s)

1. Backend fills `Senses` (owner state, weather, time, biome, light, `near.*` with line-of-sight rules).
2. `Companion.tick`: `EventDetector` turns changes into events (`rain_start`, `owner_hurt`, …) → `Brain.tick`:
   stamp events, end the running behavior if its duration/`while` says so, choose the best candidate (priority, ties
   by weight, `interrupt`, forced tricks), run delayed (`at`) and repeated actions, drift needs.
3. The movement mode comes from the behavior (or the command), filtered by Stay/Sit (`Modes`). `Mover` picks a goal
   (`Goals`), steers along an A* path (`Steering`, re-planned every 1.5 s or when the goal moves), jumps up steps,
   teleports when too far / stuck / chunk unloaded. `Physics` does gravity, water buoyancy, step-up and collisions on
   collision tops of the `BlockView`.
4. Body turn, head look (`LookControl`), locomotion state → base animation (`Locomotion`), `Animator` ticks layers.

Actions never run code: each `do` is a fixed record (`def/action`), executed by `ActionRunner`; `random` nests at most
3 deep, a behavior switch chain stops after 4 per tick, every expression is bounded.

## Animation mix

`Animator.fill(mix)`: current base (weight = 1 − fading previous base) × (1 − strongest `replace` layer) + each layer
with its own weight. cosmetico adds keyframe offsets weighted, so `add` layers stack on top of the base. Props hide
bones through `Mix.hidden`, the head look adds a rotation through `Mix.rotate(bone, …)` (both small additions to
cosmetico's `Mix`). Visibility events come from the strongest animation that has some.

## Sync

`Snapshots.of(companion, packHash, dimension)` → `SnapshotCodec.encode` (compact JSON, < 400 bytes). Sent at most
4×/s when something changed, else every 3 s; `GONE` when the companion disappears. Receivers decode (strict ids,
finite bounded numbers, sanitized texts) and create a `Puppet` only if they have a companion with the same key **and**
pack hash; puppets interpolate position, play the named base/layers and show the listed props. They time out after
10 s without data.
