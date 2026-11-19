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
must fail). The content version is the client version of the dump (first line of `versions.txt`).

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

## Gametests and the self-test

Rendering, textures, sounds and mixins are checked in the Minecraft gametests of the client (`CosmeticsGameTest`,
`AuditBundlesGameTest`, `ShowcaseGameTest`, `WardrobeUiGameTest`) and the self-test. See the parity table in
[extraction-plan.md](extraction-plan.md) §5 for what still needs a gametest once the backend switches over.
