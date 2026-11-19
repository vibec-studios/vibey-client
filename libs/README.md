# Libraries

Three Minecraft libraries that live next to Vibey Client. Each is its own mod with its own jar per target, usable
by any mod; the client depends on all three and ships them inside its jar, so players still install one file.

| Lib | Mod id | Package | Needs | Gradle project |
|---|---|---|---|---|
| [vui](vui/) | `vui` | `dev.henny.vui` | – | `:vui` |
| [r3](r3/) | `r3` | `dev.henny.r3` | vui | `:r3` |
| [cosmetico](cosmetico/) | `cosmetico` | `dev.henny.cosmetico` | r3 | `:cosmetico` |

What each one does: [docs/REBUILD.md](../docs/REBUILD.md) and each lib's own README/docs.

## Layout

```
libs/<id>/
  build.gradle              ext.libDeps = [...]  (other libs it needs), applies gradle/lib/core.gradle
  src/core/java             pure Java: no net.minecraft / com.mojang / net.fabricmc / net.neoforged imports
  src/core/resources        optional
  src/test/java             JUnit 5 tests of the core
  src/main/java             MC backend, Stonecutter-processed (//? comments) like the client's src/main
    dev/henny/<id>/mc/fabric/**     Fabric only (left out of NeoForge jars)
    dev/henny/<id>/mc/neoforge/**   NeoForge only (left out of Fabric jars)
  src/main/resources        fabric.mod.json, META-INF/neoforge.mods.toml, assets/<id>/...
  versions/<target>/        created by Stonecutter, build output only (not in git)
libs/NOTICE                 NOTICE of all libs (inside each jar as NOTICE_<id>, LICENSE as LICENSE_<id>)
```

Two kinds of Gradle projects per lib:

- `:<id>` (`libs/<id>`): plain `java-library`, compiles `src/core` once (Java 21, JOML + Gson compile-only, the
  versions every Minecraft target ships) and runs `src/test`. `check` also runs `checkCorePurity`, which fails on
  any Minecraft or loader import in `src/core`.
- `:<id>:<target>` (`libs/<id>/versions/<target>`, one per client target, e.g. `:vui:fabric-1.21.11`): the mod jar.
  It compiles `src/main` against Minecraft and the loader of that target and packs the core classes into the same
  jar. Minecraft/loader versions come from the client's `versions/<target>/gradle.properties`.

Each lib is a Stonecutter branch of the client's tree (`settings.gradle`), so `stonecutter.active` switches the
client and all libs together, and `//? if fabric` / `//? if >=1.21.9` work the same in lib sources.

Scripts: `gradle/lib/core.gradle`, `core-purity.gradle` (`:<id>`); `gradle/lib/common.gradle`, `props.gradle`,
`fabric.gradle`, `fabric-26.gradle`, `neoforge.gradle`, `fabric-resources.gradle`, `publish.gradle`
(`:<id>:<target>`). Lib versions: `vui_version`, `r3_version`, `cosmetico_version` in the root `gradle.properties`.

## Build and test one lib

```
./gradlew :vui:test                          core tests only, no Minecraft (seconds)
./gradlew :vui:check                         core tests + checkCorePurity
./gradlew :vui:fabric-1.21.11:build          mod jar -> libs/vui/versions/fabric-1.21.11/build/libs/vui-fabric-1.21.11-0.1.0.jar
./gradlew :vui:neoforge-1.21.11:compileJava  another target
```

Add `--no-daemon --max-workers=2` and the slot script on the shared PC (docs/REBUILD.md, "PC load").
`:fabric-1.21.11:build` (the client, source version) also runs every lib's `check` and `checkModuleBoundaries`.
Lib core tests run only through `:<id>:test`, never per target (same reason the client's JUnit is in `src/junit`:
Stonecutter would hand `src/test` to every target).

Pure core without Gradle: `javac` the files of `src/core/java` (+ JOML/Gson jars) and run the tests with the JUnit
console launcher.

## How the client ships them

`gradle/client-libs.gradle` adds, for every lib, the target project of the same name: `implementation` (dev
classpath, so `runClient` / `runClientGameTest` load all four mods) and `include` (Fabric) / `jarJar` (NeoForge).
The client jar then carries `META-INF/jars/vui-<target>-<version>.jar` etc., and its `fabric.mod.json` /
`neoforge.mods.toml` require `vui`, `r3`, `cosmetico` at least at their current versions. Loaders pick the newest
copy if another mod ships a lib too.

## Add a lib

1. Add the id to `libs` in `settings.gradle` (order: dependencies first).
2. `libs/<id>/build.gradle` with `ext.libDeps = [...]` and `apply from: rootProject.file('gradle/lib/core.gradle')`.
3. `<id>_version=0.1.0` in `gradle.properties`.
4. `src/main/resources/fabric.mod.json` and `META-INF/neoforge.mods.toml` (copy from vui; placeholders
   `${mod_id}`, `${version}`, `${minecraft_range}`, `${java}`, `${neo_range}`, `${minecraft_neo_range}`,
   `${<lib>_version}`), plus `src/main/java/dev/henny/<id>/mc/...` entrypoints for both loaders.
5. Nothing else: the client picks it up from `gradle.vibeyLibs` (dependency, nesting); add the `depends` lines to
   the client's `fabric.mod.json` and `neoforge.mods.toml`.

## Use a lib from another mod

The jars are not on a public Maven yet. Publish them into this repo's `build/libs-repo`:

```
./gradlew :vui:fabric-1.21.11:publishAllPublicationsToLibsRepository
```

Coordinates: `dev.henny.<id>:<target>:<version>`, e.g. `dev.henny.vui:fabric-1.21.11:0.1.0` (the same identity
NeoForge's jarJar records for the copy inside the client, so loaders see one library). In the other mod:

```groovy
repositories { maven { url = uri('<path to vibey-client>/build/libs-repo') } }
dependencies {
    // Fabric (Loom)
    modImplementation 'dev.henny.vui:fabric-1.21.11:0.1.0'
    include 'dev.henny.vui:fabric-1.21.11:0.1.0'        // optional: ship it inside your jar
    // NeoForge (ModDev)
    implementation 'dev.henny.vui:neoforge-1.21.11:0.1.0'
    jarJar 'dev.henny.vui:neoforge-1.21.11:0.1.0'
}
```

and declare it: `"depends": { "vui": ">=0.1.0" }` (Fabric) or a `[[dependencies.<your mod>]]` entry with
`modId = "vui"` (NeoForge). r3 needs vui and cosmetico needs r3: depend on (and ship) those too.
