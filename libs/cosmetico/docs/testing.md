# Testing cosmetico

## Core tests (JUnit, no Gradle needed)

`src/test/java` holds two kinds of tests:

- **Golden parity** (`golden/`): the new core must reproduce what the old Vibey Client engine (`avatar/**`) computed.
- **Behaviour tests** (one package per core package): rules the golden data cannot show (validation errors, emote
  timing with a fake clock, event firing, registry priority, dev pushes, thumbnails, preview drag, …).

Without Gradle (JDK 21 + the jars in `C:/Users/henny/.t3/build-slots/tools`, `;` is the Windows classpath separator):

```sh
javac -d out/core -cp "joml.jar;gson.jar" $(find libs/cosmetico/src/core/java -name '*.java')
javac -d out/test -cp "out/core;junit-console.jar;joml.jar;gson.jar" $(find libs/cosmetico/src/test/java -name '*.java')
java -jar junit-console.jar execute -cp "out/core;out/test;joml.jar;gson.jar" --scan-classpath out/test
```

Run it from the repository root (or anywhere below it): the golden tests find `content/` and the golden files by
walking up from the working directory. With Gradle it is `:cosmetico:test`.

## Golden files

`src/test/resources/golden/` was written by the **old** engine, by the JUnit test
`src/junit/java/dev/henny/vibeyclient/avatar/golden/OldEngineGoldenTest` (`:fabric-1.21.11:test`). That test writes a
file when it is missing (or with `-Dcosmetico.golden.write=true`) and otherwise compares, so it also guards the old
engine while it still exists. After the old engine is deleted the files stay the reference.

Inputs: every bundle in `content/dist`, `content/dist/polar-pals` and `content/audit` (42 files, also the ones that
must fail). The content version of the dump is pinned to `0.3.1` (first line of `versions.txt`, constant
`PINNED_CONTENT_VERSION` in the old engine's `MiscDump`), so a client version bump never changes the golden files.
Only the parse of the audit bundle `needs_update` (requires 9.0.0) still follows the real client version; it
changes from 9.0.0 on, long after the old engine is deleted.

| File | Content | Compared |
|---|---|---|
| `parse/<dir>/<name>.txt` | Every parsed field, warnings, errors / needs-update with message | exact |
| `mesh/<dir>/<name>.txt` | Per bone: model-space pivot and rest rotation; per part: texture, mode, quad count, SHA-256 of the raw float bits, first quad | exact (byte-equal vertices) |
| `pose/<dir>/<name>.txt` | Bone matrices (3×4 pose + 3×3 normal, visibility) for the rest pose, every animation at 37 % and 81 % of its length, and the blended state mix at two times (weights, offsetWhen); both passes; locator positions | numbers within 1e-5 (other MC targets ship other JOML versions; today they are bit-equal) |
| `anim/<dir>/<name>.txt` | `AnimTime`, `localTime`, 11 samples per channel; emotes: `EmotePlay` timeline, stop/fade, part poses and root matrix | exact |
| `states.txt` | `StateWeights` table, state start times, pet half width / side, armour hiding, emote visibility rules | exact |
| `versions.txt` | Version compare / satisfies table, dev thumbnail checks | exact |
| `registry.txt` | Builtin index load: item order, thumbnails, slot counts, DEV-over-BUILTIN merge | exact |

The fixed inputs (base frame, part rotations, state values, times) are written down twice, once per side:
`golden/Frames`, `StateSetup`, `Tables`, `PoseDump` in both test trees. Change them on both sides or not at all.

Format: one record per line, `key value value …`, floats as Java `Float.toString` (round-trip exact), arrays as
`[a b c]`. Files are LF; the comparison ignores CRLF.

## Gametests

| Test | Where | Run |
|---|---|---|
| `CosmeticoParityGameTest` | client `src/gametest` (`parity/`) | `./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=CosmeticoParityGameTest -Dvibeyclient.test.relay=off` |
| `CosmeticoStandaloneGameTest` | `libs/cosmetico/src/gametest` (only cosmetico, r3, vui, Fabric API) | `./gradlew :cosmetico:fabric-1.21.11:runClientGameTest` |
| `CosmeticsGameTest`, `AuditBundlesGameTest`, `ShowcaseGameTest`, `WardrobeUiGameTest` | client `src/gametest` | `-Dvibeyclient.test.only=<name>` |

### Screenshot parity

`CosmeticoParityGameTest` takes 31 shots (title previews, thumbnails, wardrobe, a mannequin with every slot from fixed
angles, six emotes, the local player in third person, inventory, first-person shield, local emote) through a
`ParityEngine` adapter. Step 0b took them with the old engine (`build/review/cosmetico/baseline`), step 4 with
cosmetico (`build/review/cosmetico/after`); `COSMETICO_PARITY_DIR=<folder>` writes elsewhere.

To make two runs comparable the test pins what it can: animation time is pinned to client ticks (gametest mixins on
`Minecraft.tick`, `GameRenderer.render` and `DeltaTracker` partial tick; `Clock.pin` in cosmetico), the ticker's
tick counter and 25 ms gate are reset every tick, sprite and particle randoms are seeded, the player stands on a fixed
block. What stays random: vanilla particles (every vanilla particle has its own random), a few vanilla animations. Two
runs of the *same* engine therefore differ in a few shots; that noise floor is part of the report
(`build/review/cosmetico/noise-floor.md`). Compare with

```
python libs/cosmetico/tools/shotdiff.py build/review/cosmetico/baseline build/review/cosmetico/after build/review/cosmetico/after-mask
```

(max channel difference, changed pixels, pixels off by more than 16; masks of changed pixels in the third folder).

## Self-test (other versions, Vibey Client)

For targets without a client gametest: `VIBEYCLIENT_AVATAR_TEST=true|world|devlink` (or `-Dvibeyclient.avatarTest=true`)
plus `runClient`, dev environment only. It lives in the client (`module/cosmetics/selftest`, it uses the wardrobe and
the builtin items) and runs on cosmetico: three previews on the title screen (front/side/back, then a wave), with
`world` the local player in third person (front, guitar emote, back), with `devlink` a front/back shot of every item
pushed from Blockbench. Screenshots in the run dir, log prefix `[vibeyclient-avatartest]`, the game quits at the end.

## Gametests and the production catalog

Dev runs fetch the real catalog (`GlobalSettings.backend()`). Tests that depend on the exact item order switch online
features off for their duration (`WardrobeUiGameTest`); catalog items such as `vibey:dev_cape` (sort 0) would
otherwise sort in front of the test's own items.
