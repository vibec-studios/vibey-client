# cosmetico core

The pure part of cosmetico: everything the cosmetics engine does that is not drawing, input or a game hook. Plain
Java 21 + JOML + Gson, no Minecraft, no mod loader, no Vibey Client classes. It lives in `src/core/java` and is tested
in milliseconds with JUnit (`src/test/java`, see [testing.md](testing.md)). The Minecraft backend (`src/main`, the `mc`
packages) only turns game state into core inputs and core outputs into draw calls.

The `.vibey` format itself is described in the client's [`docs/FORMAT.md`](../../../docs/FORMAT.md). It is unchanged.

## Entry points

| Type | What it is |
|---|---|
| `Cosmetico` | Static facade for mods: `init(host)`, `items()`, `loadouts()`, `emotes()`, `config()`, `preview()`, `devItems()`, `versionGate()`, `engine()`. |
| `Engine` | All engine objects wired together. `Cosmetico` holds one; tests build their own with a test host and a fake clock. |
| `Lifecycle` | Registry listener inside the engine: a replaced or removed bundle is evicted through `BundleResources` (GPU textures, sounds), an emote that was replaced restarts with the new bundle. |

`Cosmetico.init(host)` may be called once by the host mod (Vibey Client does it early in client init). Until then the
`DEFAULT` host is used (English, content version `0.3.1`). The host only hands out suppliers, so a late `init` is safe.
`Cosmetico.active()` tells the backend whether a host enabled the engine.

## Host interfaces (`host`)

The host is the mod that embeds cosmetico. It implements `CosmeticoHost`:

| Method | Default | Meaning |
|---|---|---|
| `language()` | — | `"en"`, `"de"`, … Item names and descriptions follow it (`Text.get(lang)`). |
| `contentVersion()` | — | Compared with a bundle's `minClientVersion`. Vibey Client passes its own client version, so the meaning of `minClientVersion` stays exactly as in FORMAT.md. |
| `log()` | `Log.SYSTEM` (`System.Logger` "cosmetico") | Where the engine logs. |
| `workers()` | `Workers.shared()` (2 daemon threads) | Pool for parsing and loading. cosmetico never uses the host's own task pool, so a busy host cannot starve it. |
| `effects()` | `FxSink.NONE` | Effect sink. `NONE` means "use the backend's" (vanilla particles and sounds); a host may return its own, e.g. r3 effects later. |
| `localActions()` | `LocalActions.NONE` | What "equip" and "play" mean for the local player (dev pushes use it). Vibey passes its wardrobe. |
| `thumbnails()` | `ThumbnailFetcher.NONE` | Downloads catalog thumbnails by URL. Vibey passes its disk-cached downloader. |

Other ports, set by the Minecraft backend, not by the host:

| Port | Meaning |
|---|---|
| `BundleResources` (`Engine.resources(...)`) | `prepare` (compile the mesh off-thread), `evict` (free GPU textures and decoded sounds), `prefetchSounds` (decode embedded sounds before an emote plays). |
| `FxSink` (`Engine.backendFx(...)`) | Vanilla particles and sounds, sprite particles, embedded OGG sounds. |
| `MainQueue` | Work posted from worker threads, drained only by cosmetico's own client tick. |
| `ThumbnailTextures<T>` | Upload/release of thumbnail textures, given to `ThumbnailStore<T>`. |

### Content version

`minClientVersion` keeps its meaning: "the Vibey Client version this item needs". The host reports the version it
satisfies. A standalone cosmetico reports `0.3.1` (the content level it implements). A separate engine version
(`minEngineVersion` in FORMAT.md) may come later; until then cosmetico releases follow the client's content version.

## Packages

| Package | Job | Main types |
|---|---|---|
| `model` | The parsed bundle, immutable, field names as in the format. | `Bundle` (+ `BundleHeader`, `BundleLooks`, `BundleContent`), `Bone`, `Cube`, `Face`, `Texture`, `Animation`, `Track`, `Channel`, `Event`, `Locator`, `Slot`, `Rarity`, `Attach`, `PlayerBone`, `Text` |
| `parse` | Validation and parsing (FORMAT.md / SPEC §5). One reader per section. | `Bundles.parse(bytes, gate)`, `BundleException`, `NeedsUpdateException`, `Limits` |
| `version` | Version order and the gate for `minClientVersion`. | `SemVer`, `VersionGate` |
| `registry` | Every item by id, merged over sources by priority. Lock-free reads. | `ItemRegistry`, `Item`, `ItemInfo`, `ItemMeta`, `ItemSource`, `ItemListener`, `ItemOrder` |
| `load` | Reads an `index.json` + bundles from any resource root. | `IndexLoader`, `IndexEntry` |
| `config` | The switches the host's settings write. | `CosmeticoConfig` |
| `anim` | Keyframe sampling, state weights, emote timing, clocks. | `Sampler`, `StateWeights`, `EmotePlay`, `AnimTime`, `Clock`, `TimeSource` |
| `state` | Per-avatar state and the things that change it. | `Avatar` (+ `LoadoutLayers`, `Motion`, `PetMotion`, `FrameSnapshot`, `EventClocks`, `OffsetWeights`, `Locators`), `AvatarRegistry`, `Loadouts`, `Emotes`, `EmoteControl` |
| `sim` | Per-tick simulation from plain inputs. | `MotionModel` + `MotionInput`, `StateStarts`, `PetFollower` + `PetInput`, `PetGeometry` |
| `draw` | Which bundle is shown and how its animations are mixed. | `ShownBundle`, `CosmeticMix`, `StateWeightsInput`, `OffsetBlend`, `EmoteVisibility`, `SlotVisibility` |
| `skeleton` | Bone matrices for one bundle and one mix. | `Rig`, `Mix`, `BonePose`, `PartFrames`, `LocatorSink`, `LocatorMath`, `Visibility`, `Pass` |
| `mesh` | Geometry compiled once into model space. | `BundleMesh`, `MeshPart`, `MeshMode`, `ModelSpace` |
| `pose` | The emote's effect on the vanilla player model. | `PartPose`, `EmotePartPose`, `RootPose`, `HeldItems` |
| `fx` | Animation events and sprite particles. | `EventRunner`, `EventWindow`, `FxSink`, `SpriteSpawn`, `FxMath`, `SpritePool`, `SpritePools`, `SpriteQuads` |
| `texture` | Animated texture strips. | `StripFrame`, `StripPixels` |
| `thumbs` | Thumbnail loading state machine. | `ThumbnailStore`, `ThumbnailTextures`, `ThumbnailSource`, `DevThumbnails`, `ResourceReader` |
| `dev` | Items pushed from the Blockbench live link. | `DevItems`, `DevItemService`, `PushAction`, `PushResult`, `ThumbnailCheck` |
| `preview` | GUI preview logic (drawing is in `mc`). | `PreviewView`, `PreviewState`, `PreviewLayout`, `MouseAction` |

## How a frame flows

1. **Tick** (client thread, 20/s): the backend reads each player into `MotionInput` / `PetInput`, then
   `MotionModel.step`, `PetFollower.step`, `EmoteControl.tick`, `EventRunner.tick` (events → `FxSink`), and
   `SpritePools.tick`.
2. **Render**: the backend fills `FrameSnapshot` (position, yaw, armour, hidden), asks `ShownBundle` which bundle each
   slot shows, `CosmeticMix.of(...)` for the blended animations, then `BonePose.compute(rig, mix, base, normal, parts,
   pass, locators)`. It walks the bones that are `drawn` and `visible` and submits `rig.mesh.parts[i]` with
   `matrix(i)` / `normal(i)`. `PartFrames` gives the posed vanilla parts; `LocatorSink` receives locator matrices.
3. **Emote pose**: `EmoteVisibility.visible(avatar, config)` → `EmotePartPose.blend` per vanilla part and
   `RootPose.compute` for the root transform.

`BonePose`, `CosmeticMix` and `EventRunner` keep scratch arrays: one instance per render pass or thread, never shared
between a world pass and a GUI preview drawn inside it.

## Items and sources

`ItemSource(id, priority)`: `BUILTIN` (0), `REMOTE` (100, a backend catalog), `DEV` (1000, live link). Other mods may
add their own source with any priority. For the same id the highest priority wins. `ItemRegistry.all()` is sorted by
slot (wardrobe order), then `sort`, then id, emotes last. `listen` is called after every change, `listenItems` with the
replaced/removed item (used for resource lifetime).

## Coordinates

Bundles store Blockbench space. `ModelSpace` converts to Minecraft model space: `x' = -x, y' = 24 - y, z' = z`,
rotations `(-x, -y, z)` in radians, rotation order ZYX. Mesh vertices are in blocks relative to their bone pivot,
8 floats per vertex (`x y z u v nx ny nz`).
