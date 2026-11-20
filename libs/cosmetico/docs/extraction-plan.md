# cosmetico – extraction plan

## Status (2026-11-20)

| Step | State |
|---|---|
| 0 baseline | **done**: golden dumps of the old engine in `libs/cosmetico/src/test/resources/golden` (126 files, 1.8 MB), written by `src/junit/.../avatar/golden/OldEngineGoldenTest` (`:fabric-1.21.11:test`, compares once the files exist). No change to the old engine; the only seams are test-side (reflection on `BoneRenderer`'s scratch arrays and `AvatarDraw.cosmeticMix`, a `GpuBundle` built without textures). Format: [testing.md](testing.md). Step 0b: screenshot baseline of the old engine taken with `CosmeticoParityGameTest` (31 shots, `build/review/cosmetico/baseline`, harness in [testing.md](testing.md)). |
| 1 model + parser | **done**: `model`, `parse`, `version`, host port (`language`, `contentVersion`). |
| 2 rest of the core | **done**: `registry`, `load`, `config`, `anim`, `state`, `sim`, `draw`, `skeleton`, `mesh`, `pose`, `fx`, `texture`, `thumbs`, `dev`, `preview`, `Engine` + `Cosmetico` facade, ports. 166 files, longest 118 lines, no comments. See [core.md](core.md). |
| 3 MC backend | **done**: `src/main/java/dev/henny/cosmetico/mc/**`, see [backend.md](backend.md). Compiles on fabric 1.21.1 / 1.21.4 / 1.21.5 / 1.21.11 / 26.1 / 26.3, neoforge 1.21.11 / 26.3. |
| 4 switch | **done**: the client implements `CosmeticoHost` (`module/cosmetics/host`), loads builtin items through `IndexLoader` and the catalog through `module/cosmetics/catalog/*` (split from `ItemLoader`), UI/LoadoutSync/devlink/DevDriver/gametests use the cosmetico API; `cosmetico.mixins.json` is active, the client's `vibeyclient.cosmetics/emotes.mixins.json` and their mixin classes are gone. Screenshot parity vs. the baseline: see below. Standalone: `CosmeticoStandaloneGameTest`. API for other mods: [api.md](api.md). |
| 5 delete old engine | open |

Tests: 228 JUnit tests in `libs/cosmetico/src/test` (126 golden parity + 102 behaviour), all green with JOML 1.10.5
and 1.10.8, ~0.8 s. Golden parity: parse, mesh, animation, state and registry dumps are **exactly** equal to the old
engine; bone matrices are bit-equal too (compared with ε 1e-5 to survive other JOML versions).

Parity rows (§5) with a new core test now: P1, P2, P3, P4 (unknown slot), P5, P6, P7, P8, P9, P10, P11, P13, P14,
P15, P16, P17, P18 (core part, recording `FxSink`), P19 (core part), P21 (core part: `StripFrame`, `StripPixels`), P26,
P28 (core part: `ShownBundle`, `SlotVisibility`), P30 (core part: `PreviewState`), P31 (core part: `ThumbnailStore`,
`ThumbnailCheck`), P32, P33, P37. Still GT-only (step 3/4): P12, P20, P22–P25, P27, P29, P38–P40; client-side: P34–P36.

### Deviations from the design below

- `ItemRegistry` is a class (instance owned by `Engine`), not an interface. `Engine` is new: all core objects wired
  together; `Cosmetico` is the static facade over one `Engine`, tests build their own `Engine` with a fake clock.
- `ItemSource` is a record `(id, priority)` as planned; the old `CATALOG` is `REMOTE`. Other mods can add sources.
- Registry change events: `listen` (Runnable) and `listenItems` (`ItemListener.replaced/removed`). `Lifecycle`
  evicts replaced/removed bundles through `BundleResources` and restarts replaced emotes (R4, P37). `putAll` reports
  replaced and, with `replace=true`, dropped items.
- `CosmeticoHost` has the plan's methods plus `log()`, `workers()`, `effects()`. `BundleResources` (prepare / evict /
  prefetchSounds) is the backend port the plan called `Backend`.
- `FxSink.sprite(owner, bundle, SpriteSpawn)`: one call with `count`; the backend spawns into a `SpritePools`.
  Vanilla particle spread randomisation stays in the backend (as in the old `Effects`).
- `DevItems.push` always takes a thumbnail argument (null = none); no two-argument variants.
- Previews: `Cosmetico.preview()` returns a `PreviewView`; the backend calls `PreviewState.frame(x, y, w, h, partial,
  now, selfAvatar)` each frame (returns the entity size) and then draws `avatar`.
- `Emotes` has no `playLocal/stopLocal` yet: camera switch and stop-on-move need the game → step 3 (`LocalEmotes`).
- `MotionModel` keeps the old quirk of using `FrameSnapshot.x/y/z` as the previous position (the render extract writes
  the same fields), for exact parity.
- `Rarity` has ids only (colours → client `RarityColors`), `Slot.ruleKey()` is gone (→ client `SlotRules`), both as
  planned. `Text.get(language)` replaces `Text.get()`.
- `IndexLoader.read(...)` is synchronous and returns the items; running it on `host().workers()` and handing the result
  over through `MainQueue` is the backend's tick (step 3).
- Error messages are byte-identical to the old ones, including "needs Vibey Client x or newer" (golden parity).
- The infra branch's `checkCorePurity` bans `net.minecraft`, `com.mojang`, `net.fabricmc`, `net.neoforged`; this plan
  also wants `org.lwjgl` and `dev.henny.vibeyclient` banned (the core has neither today).

### Step 3/4 notes (2026-11-20)

- Old `avatar/**` classes are still in the client (unused, compiled). Only `avatar/preview/PreviewLegacy` changed (its
  accessor now comes from cosmetico, so it still compiles on 1.21.2–1.21.5).
- Not split yet although touched: `LoadoutSync` (561 lines), `WardrobeScreen` (497), `CosmeticsModule` (~320): only
  the API calls changed. Do the §1.3 splits before more features land there.
- Self-test (P39) ported to the client: `module/cosmetics/selftest/*` on the cosmetico API, same env var, shot names
  and log prefix (it needs the wardrobe and builtin items, so it stays a Vibey Client tool).
- `WardrobeUiGameTest` failed after the merge because the production catalog gained `vibey:dev_cape` (sort 0); the
  pre-switch commit fails the same way. The test now runs with online features off.
- `CosmeticoConfig.thirdPersonEmotes` / `emoteVolume` are now set by the emotes module; the module broadcasts its own
  emotes through an `EmoteListener` (start/stop of the local player), so stop-on-move inside cosmetico is broadcast too.
- `Locators` got `names()` / `clear()` (used by the showcase gametest), `Clock` got `pin()`.
- Screenshot parity (31 shots, `build/review/cosmetico/report.md`): 15 shots bit-identical to the old engine, the
  others within the old-vs-old noise of the harness (vanilla particles, vanilla arm bob, sprite particles); a first
  new-engine run (`after_run1`) showed tick-timing noise on 5 shots that a second run did not, so the harness still
  has run-to-run timing noise around world joins. R1 checked in the test: exactly one `CosmeticLayer`.
- Client build: `gradle/client-libs.gradle` adds each lib's core project as `compileOnly` (fabric 26.x and NeoForge
  compile against the lib target's class dirs, which do not contain the core; their runtime uses the jar, which does).

### What step 3 had to do (kept for reference)

1. `mc` backend per the tables in §1 and §4: `BoneDraw` (walk `BonePose`, pick render type per `MeshMode`, submit
   through `MeshSink`), `ModelPartFrames implements PartFrames`, `GpuBundle` / `GpuCache` (built from `Rig`, textures
   via `StripFrame`), `Tex`, `EntityRenderTypes`, `SoftPipelines` (`cosmetico:` namespace, R3), `VanillaFx implements
   FxSink` (+ `SpritePools` / `SpriteQuads` render, embedded sounds `OggDecoder` / `AlSources`), `McPreview` around
   `PreviewState`, `ThumbnailTextures` for `ThumbnailStore`, `PartPose` / `RootPose` appliers, `PlayerSnapshot`
   (fills `FrameSnapshot`, `SlotVisibility`), `McTicker` (`MotionInput`, `PetInput`, tick order of §2), `LocalEmotes`.
2. Install the ports at boot: `Cosmetico.engine().resources(...)`, `.backendFx(...)`; drain `MainQueue` in the own tick.
3. Keep every scratch object (`BonePose`, `CosmeticMix`, `EventRunner`) per pass, not global (R7).
4. Mixins stay inactive until step 4 (R1). Build fabric-1.21.11 and one target per `//?` axis.
5. Take the gametest screenshot baseline with the old engine before step 4 (part of step 0 that needs Minecraft).

---

Design only. Source: branch `henny/client-aenderungen` @ `062d694`. Contract: `docs/REBUILD.md` § cosmetico, `docs/AVATAR.md`,
`docs/FORMAT.md` (format unchanged).

Rules this plan follows (on top of REBUILD.md):

- **Short files.** One class/record/enum per file, one clear job, target < 200 lines, hard cap ~300. Every class over
  that is split below (§1.3). Nested types become top-level files.
- **No comments or javadoc in the new code.** Every explanation that today sits in javadoc moves into
  `libs/cosmetico/docs/*.md` (§7). The old engine keeps its comments until it is deleted.
- **Pure core.** `libs/cosmetico/src/core/java` = Java 21 + JOML + Gson, no `net.minecraft`, `com.mojang`,
  `org.lwjgl`, loader or `dev.henny.vibeyclient` imports (the check task also has to ban `dev.henny.vibeyclient`).
- **Same API on every version.** The public API exposes no MC types, except small `mc` entry helpers
  (`GuiGraphics` for preview/thumbnail drawing).
- r3 / vui: no API dependency yet. cosmetico declares two internal ports (`FxSink`, preview host) that get an r3/vui
  implementation later (§2.3).

---

## 0. What the engine is today (for orientation)

- 66 files, ~10.1k lines across `avatar/**` (engine), `module/cosmetics/**` (loadout, sync, wardrobe UI, mixins),
  `module/emotes/**` and `module/nametags/**`.
- Already pure (no MC imports): `bundle/*`, `anim/*`, `AvatarItem(s)`, `AvatarState(s)`, `Slot`, `Rarity`,
  `ItemSource`, `ClientVersion`, `DevItems(Impl)`, `render/BundleMesh`. **But** they still use client classes:
  `api.Lang` (Bundle.Text), `BuildInfo.VERSION` (ClientVersion), `Vibey.LOG` (AvatarItems), `GpuBundles` /
  `EmbeddedSounds` (DevItemsImpl, AvatarStates.play).
- Mixed: `BoneRenderer` and `AvatarDraw` are JOML matrix math plus `PoseStack` and `RenderType` → split into a core
  part (math) and a thin mc part (submission).
- `module/nametags` does **not** use the engine at all (only `VibeyPlayers` and `Compat`) → stays in the client
  entirely, out of scope.
- Signed loadouts (`LoadoutSync` + `net/LoadoutToken` + `backend/LoadoutKeys`) are Vibe backend features → stay in
  the client. cosmetico only gets "set the loadout of UUID X".

---

## 1. Inventory

Legend: **core** = `libs/cosmetico/src/core/java/dev/henny/cosmetico/...`, **mc** =
`libs/cosmetico/src/main/java/dev/henny/cosmetico/mc/...`, **client** = stays in `src/main/java/dev/henny/vibeyclient`,
**delete** = removed once the replacement has parity and a test.

### 1.1 `avatar/**`

| Old class (lines) | Target | New file(s) | Reason / notes |
|---|---|---|---|
| `bundle/Bundle` (379) | core `model` | split, see §1.3 | Immutable format model. `Text.get()` uses `api.Lang` → `Text.get(String lang)` + `Cosmetico.host().language()` |
| `bundle/BundleParser` (616) | core `parse` | split, see §1.3 | Validation per SPEC §5. `ClientVersion.satisfies` → `VersionGate` (host version) |
| `bundle/BundleException` | core `parse` | `BundleException` | as is |
| `bundle/NeedsUpdateException` | core `parse` | `NeedsUpdateException` | as is |
| `Slot` | core `model` | `Slot` | `ruleKey()` (PLAY-LINKS server rule) is Vibey-specific → client `SlotRules.key(slot)` |
| `Rarity` | core `model` | `Rarity` (ids only) | ARGB "Vibe palette" colours → client `RarityColors` |
| `ItemSource` | core `registry` | `ItemSource` record `(id, priority)` + constants `BUILTIN=0, REMOTE=100, DEV=1000` | A library needs more than 3 sources (other mods). Client maps `CATALOG` → `REMOTE` |
| `AvatarItem` (105) | core `registry` | `Item` (~90) + `ItemMeta` record (creator, minVersion, availability, thumbnail, sort) | `needsUpdate()` → `VersionGate`. `DELETED_CREATOR` stays (client shows it localised) |
| `AvatarItems` (115) | core `registry` | `ItemRegistry` (~90), `ItemOrder` (comparator, ~20), `RegistrySnapshot` record | `Vibey.LOG` → `Log` port. Static → instance owned by `Cosmetico` |
| `ItemLoader` (608) | **split**: builtin part → core `load`; catalog part → **client** | core: `IndexLoader` (~80, reads `index.json` + bundles from a classpath root, generic). client: see §1.3 | Catalog = Vibe backend protocol (ETag, `catalog.updated`, `ready.catalogVersion`, sha cache) → client. Touches `GlobalSettings`, `Services/VibeBackend`, `Net`, `HttpCache`, `Limiter`, `SingleFlight`, `Tasks`, `Vibey.platform().gameDir()` |
| `ClientVersion` | core `version` | `SemVer` (compare, ~40) + `VersionGate` (~20, `satisfies(required)` against `host().contentVersion()`) | `BuildInfo.VERSION` → host. Vibey passes its own version, so `minClientVersion` semantics stay as they are |
| `AvatarState` (93) | core `state` | `Avatar` (uuid, preview, loadout + overrides, emote, ~60), `Motion` (walk/fly/…/targets/ticks, ~40), `PetMotion` (~25), `FrameSnapshot` (x/y/z, yaw, armour flags, hidden, firstPerson, crouching…, ~30), `EventClocks` (~30), `OffsetWeights` (~25), `Locators` (~25) | One job per file. `stateStart` goes into `Motion` |
| `AvatarStates` (96) | core `state` | `AvatarRegistry` (get/peek/remove/prune/clear, ~60), `Loadouts` (setLoadout + layers, ~50), `EmoteControl` (play/stop/reload/tickEmote, ~60) | `AvatarStates.play` calls `EmbeddedSounds.prefetch` → `Backend.prefetchSounds(bundle)` port |
| `AvatarConfig` | core `config` | `CosmeticoConfig` (instance; `cosmetics, showOwn, showOthers, emotes, showOthersEmotes, slotShown[], slotServerOff[], emotesServerOff, hideWithArmor, emoteVolume, thirdPersonEmotes`) | `AvatarDraw.hideWithArmor` and `Effects.volume` move here too. `self` → `Cosmetico.self()` |
| `AvatarTicker` (203) | core `sim` + mc `tick` | core: `MotionModel` (move math, input record `MotionInput`, ~90), `StateStarts` (`states()`, ~25), `PetFollower` (pet math, input `PetInput`, ~100), `PetGeometry` (halfWidth/side, ~40). mc: `McTicker` (reads `Player` → inputs, ~70) | Only `Minecraft`/`Player` reads are MC; the math is pure and becomes unit-testable |
| `EngineCompat` (54) | mc `compat` | `McSelf` (selfId/selfName/onRenderThread), `McCamera` (get/set camera type) | `isPhysicallyDown(KeyMapping)` is a key-input helper used only by the emote wheel → **client** `Keys` |
| `EmoteBridge` | **delete** | – | Replaced by `Cosmetico.emotes()` + `EmoteListener` (§2) |
| `Wardrobe` | **client** | `module/cosmetics/Wardrobe` | Vibey UI contract (`canUse`, `lockedReason`, wheel slots) |
| `DevItems` (interface) | core `dev` | `DevItems`, `PushResult` record, `PushAction` enum, `ThumbnailCheck` (checkThumbnail, ~30) | Used by devlink. Client keeps registering it as a service |
| `DevItemsImpl` (93) | core `dev` | `DevItemService` (~80) | `Wardrobe` → `LocalActions` port (equip / play), `GpuBundles` → `Backend.prepare/evict` port, `ThumbnailCache` → `ThumbnailStore` |
| `Thumbnails` / `ThumbnailCache` (108) | core `thumbs` + mc + client | core: `ThumbnailStore` (state machine LOADING/FAILED/READY, dev PNG keys, ~90), `ThumbnailFetcher` port. mc: `ThumbnailTextures` (upload + `draw(GuiGraphics,…)`, ~50). client: `CatalogThumbnails implements ThumbnailFetcher` (disk cache + download, ~60) | `ui.Gfx` → `GuiGraphics` (client passes `g.g`). Own daemon executor stays |
| `SelfTest` (229) | mc `selftest` (dev only) | `SelfTest` (steps, ~120), `SelfTestShots` (screenshot per version, ~35), `SelfTestScreen` (plain vanilla `Screen` + `GuiGraphics.fill`, ~45), `SelfTestWorld` (pressLoad, loading, ~35) | Uses `BaseScreen/Gfx/Theme`, `ClientCompat`, `Services`, `Tasks` → vanilla `Screen`, own `McScreens`, `Cosmetico` API. Equips through the **test override layer** (see risk R5). Keeps `VIBEYCLIENT_AVATAR_TEST` as alias of `COSMETICO_SELFTEST` |
| `anim/Clock` | core `anim` | `Clock` (instance, `TimeSource` injectable) | Tests need a fake clock |
| `anim/Sampler` | core `anim` | `Sampler` | as is |
| `anim/EmotePlay` | core `anim` | `EmotePlay` (~90) | as is |
| `anim/StateWeights` | core `anim` | `StateWeights` | as is |
| `render/BundleMesh` (260) | core `mesh` | split, see §1.3 | Already pure |
| `render/BoneRenderer` (283) | core `skeleton` + mc `render` | split, see §1.3 | Matrix math pure; `PoseStack`/`RenderType` only in mc |
| `render/AvatarDraw` (301) | core `draw` + mc `render` | split, see §1.3 | Mix/weights/offsets/time are pure |
| `render/AvatarHooks` (163) | core `pose` + mc `render` | core: `EmotePartPose` (blend values per part → `PartPose` struct, ~60), `RootPose` (translate/rot/scale values, ~40), `HeldItems` (hideHeldItems rule, ~15). mc: `PlayerSnapshot` (extract from `Player`, ~60), `PartPoseApplier` (writes into `ModelPart`, ~40), `RootPoseApplier` (into `PoseStack`, ~25), `CapeShieldTextures` (~40) | |
| `render/AvatarCarrier` | mc `render` | `AvatarCarrier` (method names `cosmetico$avatar`) | mixin interface |
| `render/PlayerParts` | core `skeleton` | `PartFrames` (Matrix4f/Matrix3f per `PlayerBone`) | JOML instead of `PoseStack` |
| `render/ModelParts` | mc `render` | `ModelPartFrames implements PartFrames` | `//?` 1.21.2 root |
| `render/MeshSink` (141) | mc `render` | `MeshSink` (abstract + `emit`, ~50), `BufferSink` (<1.21.9, ~50), `SubmitSink` (≥1.21.9, ~70) | 1.21.5 emissive flush stays in `BufferSink` |
| `render/Tex` (212) | mc `render` | `Tex` (upload/register/release, ~80), `EntityRenderTypes` (cutout/translucent/emissive/glow factories, ~80), `SoftPipelines` (~75) | `Compat.id` → `McIds`. Namespace becomes `cosmetico:` (R3) |
| `render/TexPixels` | mc `render` | `TexPixels` | |
| `render/GpuBundle` (118) | mc `render` + core | mc: `GpuBundle` (~50), `AnimatedStrip` (frame copy + interpolate, ~70). core: `BoneTracks` (animation → track per bone, ~30), `StripFrame` (frame/blend key math from `animate`, ~35) | Frame selection is pure and testable |
| `render/GpuBundles` (94) | mc `render` | `GpuCache` (~90) | `Vibey.LOG` → `Log`; `UPLOADS_PER_FRAME` = 3 kept |
| `render/CosmeticLayer` (84) | mc `render` | `CosmeticLayer` | `//?` per version, see §4 |
| `render/WorldPass` (77) | mc `render` | `WorldPass` | `gui` counter → `GuiDepth` (~15) |
| `render/SkinOverride` (42) | mc `render` | `SkinOverride` | |
| `render/ShieldDraw` (69) | mc `render` | `ShieldDraw` | |
| `render/PoseCompat` | mc `compat` | `PoseCompat` | |
| `fx/Effects` (196) | core `fx` + mc `fx` | core: `EventRunner` (crossed-time logic incl. loop wrap, which animation fires, ~110), `FxMath` (toWorld, locator position fallback, ~40), `FxSink` port (`particle(id,…)`, `sprite(…)`, `sound(id,…)`, `embeddedSound(…)`). mc: `VanillaFx implements FxSink` (registries, `SimpleSoundInstance`, volume × MASTER × PLAYERS, ~90), `McIds` | r3 later implements `FxSink` for sprites/beams |
| `fx/SpriteParticles` (185) | core `fx` + mc `fx` | core: `SpritePool` (spawn/tick/move, ~100), `SpriteQuads` (billboard quads, ~60). mc: `SpriteRender` (~40) | |
| `fx/EmbeddedSounds` (166) | mc `fx` | `OggDecoder` (stb_vorbis, ~55), `AlSources` (play/cleanup/evict/stopAll, ~100), `SoundPrefetch` (~30) | LWJGL = mc side. `Tasks.run` → cosmetico's own executor. Never touch LWJGL during startup (known crash) |
| `preview/AvatarPreview` | core `api` + mc | core: `PreviewView` interface (no `Gfx`) + `MouseAction`. mc: `McPreview.draw(GuiGraphics, view, x,y,w,h,delta)` | Only `draw` needs an MC type |
| `preview/PreviewService` | core `api` | folded into `Cosmetico.previews()` | |
| `preview/PreviewView` (247) | core + mc | core: `PreviewState` (yaw/auto/drag/overrides/emote/pet placement/span, ~110). mc: `PreviewRenderer` (render state + PiP per version, ~110), `PreviewSkin` (skin lookup + cape patch + `isOurs`, ~55) | Drag and pet placement become unit-testable |
| `preview/PreviewLegacy` (120) | mc `preview` | `PreviewLegacy` | <1.21.6 only |

### 1.2 Modules

| Old | Target | Notes |
|---|---|---|
| `module/cosmetics/CosmeticsModule` (337) | client, split | `CosmeticsModule` (settings + lifecycle, ~140), `LocalWardrobe implements Wardrobe` (~120), `NewItemsToast` (~50), `CosmeticoSettingsBridge` (applyConfig + server rules → `CosmeticoConfig`, ~50), `SelfLoadout` (applySelf/sharedSlots/adoptRemote, ~80) |
| `module/cosmetics/Loadout` | client | as is |
| `module/cosmetics/LoadoutSync` (561) | client, split | §1.3 |
| `module/cosmetics/ui/*` | client | Use `Item`, `Cosmetico.items()`, `McPreview`, `ThumbnailTextures`. `WardrobeScreen` (497) split §1.3. The rest is < 300; `WheelEditorScreen` (266) and `EmoteWheelScreen` (232) can stay as they are until vui replaces them |
| `module/cosmetics/mixin/*` | cosmetico mc, see §3 | |
| `module/emotes/EmotesModule` (202) | client, slimmer | Keeps: settings, wheel key, server rule gate + toast, `RemoteEmote` filter (`allowServerEmote`), broadcast. Moves to cosmetico `LocalEmotes`: play/stop on the local player, third-person camera switch + restore, stop-on-move, `onEmoteEnded`. Emotes are a cosmetico feature and work in standalone use too |
| `module/emotes/mixin/PlayerModelMixin` | cosmetico mc | |
| `module/nametags/**` | client | No engine use. Ranks / animated names later on vui/r3 |
| `dev/DevDriver`, `DevTour`, `DevTourB` | client | Switch imports to the API (`Cosmetico.items()`, `Slot`, `McCamera`) |
| `module/devlink/*` | client | `DevItems` from core; `BundleParser.parse` → `Bundles.parse` |
| `mixin/client/RenderTypeInvoker` | cosmetico mc (own copy as `RenderTypeInvoker` in `cosmetico.mixins.json`) | Only used by `Tex`. Delete the client copy in the final step |

### 1.3 How the big classes get split

**`Bundle` (379)** → core `model/`, one file per type: `Bundle` (top-level fields + `animation()`, `emoteAnimation()`,
`isEmote()`, ~70), `Kind`, `Attach`, `PlayerBone`, `Armor`, `PetFollow`, `Interp`, `Text`, `Texture` (~45),
`Face`, `Cube` (~35), `Bone` (~40), `Locator`, `Model` (~35), `Channel` (~30), `Track`, `EventType`, `Event`
(~45), `Animation` (~40), `Sprite`, `Sound`, `EmoteOptions`, `PetOptions`, `OffsetRule`. That's 23 files, all < 70
lines. Keep the field names exactly the same so moving is search-and-replace on the package name.

**`BundleParser` (616)** → core `parse/`:
`Bundles` (public facade: `parse(byte[])`, `parse(JsonObject)`, `sha256`, ~40), `BundleReader` (size/format/version
gates, metadata, orchestration, ~130), `Limits` (constants + `ID` pattern, ~20), `Json` (str/num/bool/vec/floats/
base64/text, ~90), `Warnings` (~20), `CosmeticReader` (slot, hideWithArmor, pet, cape/shield, ~70),
`OffsetWhenReader` (~55), `EmoteReader` (~35), `TextureReader` (incl. PNG header readInt, ~70), `ModelReader`
(bones, parent order, cycle `visit`, locators, ~120), `CubeReader` (~75), `AnimationReader` (~70),
`ChannelReader` (~45), `EventReader` (~70), `PriceGuard` (rejects price/cost/prices, ~15), `Sha256` (~15).

**`ItemLoader` (608)**: the builtin part → core `load/IndexLoader` (~80). The catalog part stays in the client,
`module/cosmetics/catalog/`: `CatalogPoller` (schedule, jitter, force, `requestCatalog`, `readyVersion`, ~120),
`CatalogLoad` (disk-first then network, generation, `Applied`, ~110), `CatalogApply` (entries → items, reuse by
hash, downloads, ~140), `CatalogEntry` (record + parse from JSON, ~40), `CatalogItems` (item/stub/creator/text,
~100), `CatalogSwap` (client-thread swap, evict, new-ids diff, ~70), `AssetCache` (disk read/write atomic, sha
check, `catalog.base`, ~80), `AssetDownloads` (Limiter + SingleFlight, ~45), `OnceLog` (~20). The
`GpuBundles.evict` and `EmbeddedSounds.evict` calls become `Cosmetico.items().replace(...)`; cosmetico itself frees
GPU/sound resources of replaced bundles (listener inside the registry, see R4).

**`AvatarDraw` (301)** → core `draw/`: `CosmeticMix` (cosmeticMix + visibility pick, ~70), `StateWeightsInput`
(`weights()` incl. "empty optional state = missing", ~35), `OffsetBlend` (offsetWhen easing, ~50), `AnimTime`
(`time(a, now)`, `time(st,k,a,now)`, ~30), `ShownBundle` (`bundle(st,slot)`, `hiddenByArmor`, `hasWorldBones`,
`shoulder`, ~55). mc `render/`: `AttachedPass` (~90), `WorldDraw` (~90), `LocatorCollector` (~35).

**`BoneRenderer` (283)** → core `skeleton/`: `Mix` (~45), `BonePose` (computes `Matrix4f`/`Matrix3f`/visible/drawn
for every bone from `PartFrames` + Mix, no allocation, ~140), `BoneTransform` (`transform()` blend, ~60),
`Visibility` (~20), `LocatorMath` (`locatorLocal`, part pivots, ~30), `Pass` enum. mc: `BoneDraw` (walks
`BonePose`, picks render type per `MeshMode`, hands parts to `MeshSink`, ~70).

**`BundleMesh` (260)** → core `mesh/`: `BundleMesh` (~60), `MeshPart` (~20), `MeshMode`, `ModelSpace`
(Blockbench → model coordinates, ~30), `BoxUv` (~70), `FaceUv` (~60), `CubeQuads` (~90).

**`LoadoutSync` (561, client)** → `module/cosmetics/sync/`: `LoadoutSync` (coordinator + tick, ~110),
`OwnedItems` (owned set, `canUse`, `isDefault`, `reloadOwned`, ~90), `LoadoutPush` (~80), `OwnToken`
(fetch/adopt/due, ~100), `RemoteVerifier` (verify, reverifyAll, lastValid, effective, allowServerEmote, ~110),
`PresenceSubscriptions` (subscriptions, badges, lookups, ~100), `RemoteLoadouts` (fromBackend/fromServer,
applyRemotes → `Cosmetico.loadouts().set`, slot/uuid parsing, ~90), `EmoteRelay` (~40).

**`WardrobeScreen` (497, client)** → `WardrobeScreen` (layout + input routing, ~150), `SearchBar` (~65),
`CategoryRail` (~60), `ItemGrid` (~120), `EmptyState` (~40), `PreviewColumn` (~100). This is a mechanical split; vui
replaces it in phase 3, so keep the effort small (do it only if the screen has to be touched for the API switch
anyway).

**Gametests** over 300 lines (`ShowcaseGameTest` 468, `WardrobeUiGameTest` 431, `AuditBundlesGameTest` 416): split
when they move or change (e.g. `AuditBundlesGameTest` → `AuditRenderSteps` + `AuditOtaSteps` + `MockCatalog`), not
as a goal of their own.

### 1.4 Dependencies on client classes and how to cut them

| Client dependency | Used by | Cut |
|---|---|---|
| `Vibey.LOG` | AvatarItems, GpuBundle(s), CosmeticLayer, WorldPass, ShieldDraw, Effects, EmbeddedSounds, PreviewView, SelfTest | core `Log` port (`info/warn/error/debug`); mc sets an SLF4J logger named `cosmetico` |
| `Vibey.platform()` (`gameDir`, `isDevelopment`) | ItemLoader, SelfTest | mc `McPlatform` (Fabric: `FabricLoader`; NeoForge: `FMLLoader`), loader folders like the client's `fabric/` / `neoforge/` |
| `BuildInfo.VERSION` / `MINECRAFT` | ClientVersion, ItemLoader, SelfTest | `CosmeticoHost.contentVersion()`; MC version from `SharedConstants` in mc |
| `api.Lang` | Bundle.Text, (UI) | `CosmeticoHost.language()` (`"en"`/`"de"`); the client returns `Lang.german() ? "de" : "en"` |
| `api.Tasks` (`run`, `onMain`, `later`, `debounce`) | ItemLoader, EmbeddedSounds, SelfTest | core `Workers` (own small daemon pool; do **not** share the client pool, see the Tasks-pool starvation note) + `MainQueue` drained in cosmetico's tick |
| `api.Services` | CosmeticsModule, SelfTest, DevItems, Wardrobe | cosmetico uses its static `Cosmetico` facade. The client still registers `DevItems` / `Wardrobe` / `AvatarPreview` services that delegate to it |
| `api.GlobalSettings` (online, backend URL) | ItemLoader | client only (catalog) |
| `backend.net.*` (`Net`, `HttpCache`, `Limiter`, `SingleFlight`), `VibeBackend` | ItemLoader | client only (catalog, thumbnails through `ThumbnailFetcher`) |
| `Compat.id` | Tex, Effects | mc `McIds` (`Identifier` ≥1.21.11 / `ResourceLocation` before) |
| `mixin.client.RenderTypeInvoker` | Tex | own copy in the cosmetico mixin config |
| `ui.Gfx`, `ui.BaseScreen`, `ui.Theme` | Thumbnails, AvatarPreview, PreviewView, SelfTest | API takes `GuiGraphics` (`g.g`); SelfTest uses a vanilla `Screen` |
| `client.ClientCompat` (`screen`, `openScreen`) | SelfTest, EmotesModule | mc `McScreens` (2 methods) |
| `client.Keys` / `EngineCompat.isPhysicallyDown` | EmotesModule | stays in the client |
| `api.ServerRules` | CosmeticsModule, EmotesModule | client writes into `CosmeticoConfig.slotServerOff` / `emotesServerOff` |
| `ModConflicts` via `VibeyMixinPlugin` | cosmetics/emotes mixin configs | cosmetico gets its own `CosmeticoMixinPlugin` with a conflict list (other cosmetics/emote mods); the client stops gating these mixins |
| `Wardrobe` (DevItemsImpl → equip/play) | DevItems | core `LocalActions` port; the client passes `LocalWardrobe`; the standalone default sets the self loadout or plays locally |

---

## 2. Public API proposal

Everything under `dev.henny.cosmetico` (core) unless marked `mc`. Static facade, client thread unless noted.

```java
public final class Cosmetico {
    static void init(CosmeticoHost host);                       // once; mc bootstrap calls init(CosmeticoHost.DEFAULT) if no host did
    static CosmeticoHost host();
    static ItemRegistry items();
    static Loadouts loadouts();
    static Emotes emotes();
    static CosmeticoConfig config();
    static Previews previews();
    static DevItems devItems();
    static UUID self();
}
public interface CosmeticoHost {                                // the client implements this; DEFAULT for standalone use
    String language();                                          // "en" | "de" | …
    String contentVersion();                                    // compared with minClientVersion
    default LocalActions localActions() { return LocalActions.DEFAULT; }
    default ThumbnailFetcher thumbnails() { return ThumbnailFetcher.NONE; }
}
public record ItemSource(String id, int priority) { BUILTIN, REMOTE, DEV }
public interface ItemRegistry {
    List<Item> all(); Item get(String id); Item get(String id, ItemSource src);
    List<Item> bySlot(Slot slot); List<Item> emotes(); long version();
    void put(Item item); void putAll(ItemSource src, Map<String, Item> items, boolean replace);
    Item remove(String id, ItemSource src); long nextRevision();
    AutoCloseable listen(Runnable onChange);
    void loadIndex(Class<?> anchor, String classpathRoot, ItemSource src);   // index.json + *.vibey, off-thread
}
public final class Bundles { static Bundle parse(byte[] b); static Bundle parse(JsonObject o); static String sha256(byte[] b); }
public interface Loadouts {
    void set(UUID player, Map<Slot, String> slots);             // base layer (from the host: own, backend, server)
    Map<Slot, String> get(UUID player); void clear(UUID player); void clearAll();
    void override(UUID player, Slot slot, String itemId);       // try-on / self-test layer, null id = shown empty
    void clearOverrides(UUID player);
}
public interface Emotes {
    boolean play(UUID player, String emoteId); void stop(UUID player); String playing(UUID player);
    boolean playLocal(String emoteId); void stopLocal();        // camera switch, stop on move
    AutoCloseable listen(EmoteListener l);                      // started/stopped/ended (local + remote)
}
public interface FxSink {                                       // mc: VanillaFx; later r3
    void particle(String id, double x, double y, double z, float vx, float vy, float vz, int count, float sx, float sy, float sz);
    void sound(String id, double x, double y, double z, float volume, float pitch);
    void embedded(Bundle.Sound s, double x, double y, double z, float volume, float pitch);
    void sprite(UUID owner, Bundle b, int sprite, double x, double y, double z, float vx, float vy, float vz, float life, float size, float gravity, boolean emissive);
}
public interface Previews { PreviewView create(); }
public interface PreviewView {                                  // logic only; drawing in mc
    float yaw(); void setYaw(float deg); void setAutoRotate(boolean on);
    boolean mouse(MouseAction a, double x, double y);
    void playEmote(String id); void stopEmote(); String emote();
    void override(Slot slot, String itemId); void clearOverrides();
}
// mc helpers (only place with MC types)
public final class McPreview { static void draw(GuiGraphics g, PreviewView v, int x, int y, int w, int h, float delta); }
public final class McThumbnails { static boolean draw(GuiGraphics g, Item item, int x, int y, int size); }
public final class McSelfTest { static boolean enabled(); }     // dev only, -Dcosmetico.selftest=title|world|devlink
```

Notes:

- **Render hooks are automatic**: cosmetico's own mixins (§3) draw every player that has a state in `Loadouts`. The
  host never calls render code.
- **Tick**: cosmetico registers its own client tick (Fabric `ClientTickEvents.END_CLIENT_TICK`, NeoForge
  `ClientTickEvent.Post`): `MainQueue`, `IndexLoader` handover, `McTicker`, `GpuCache.tick`, sprites, sound cleanup,
  self-test. The client stops calling `AvatarTicker.tick()` / `ItemLoader.tick()` from two modules.
- **Config** is a plain mutable object (`config().showOwn = …`, `config().slotShown[slot.ordinal()]`). Settings UI
  stays with the host. Standalone cosmetico later gets a vui settings page.
- **Preview widget as a vui component** (contract item): phase 3. `PreviewView` stays the model, and a
  `<cosmetico-preview>` vui component wraps `McPreview.draw`. Until vui is there, `McPreview` is the entry point.
- **Thumbnails**: `ThumbnailStore` in core (dev PNGs from pushes, classpath `/assets/...`, everything else through
  `host().thumbnails()`), upload + draw in mc.
- **Self-test**: `McSelfTest`, dev environment only, same steps and screenshot names as today (with a
  `cosmetico-selftest` log prefix plus the old `[vibeyclient-avatartest]` prefix while the client's tooling still
  greps for it).

### 2.3 Ports that r3/vui will fill later

- `FxSink` (particles, sounds, sprites): `VanillaFx` + `SpriteRender` now, r3 effects later. The core never calls MC.
- `McPreview` → vui component. Thumbnails → vui `image`.
- Text (names on items): core returns plain strings. Rarity colours and animated names come from vui text effects
  in the client.

---

## 3. Mixins and entrypoints

New config `libs/cosmetico/src/main/resources/cosmetico.mixins.json`, package `dev.henny.cosmetico.mc.mixin`, plugin
`CosmeticoMixinPlugin` (conflicts), `defaultRequire: 0` as today (versions differ in targets).

| Mixin | Moves to cosmetico | Notes |
|---|---|---|
| `PlayerRendererMixin` (layer, extract, root, 1.21.1 world pass) | yes | Unique prefix `cosmetico$` |
| `LivingEntityRendererMixin` (world pass) | yes | |
| `LivingEntityRendererAccessor` | yes | |
| `RenderStateMixin` (carrier) | yes | |
| `AbstractClientPlayerMixin` (cape skin) | yes | |
| `ItemInHandLayerMixin` (shield owner; 1.21.1 hide items) | yes | |
| `FirstPersonItemMixin` (shield owner, first person) | yes | |
| `ShieldRendererMixin` | yes | |
| `GuiEntityMixin` (skip world things in GUI) | yes | |
| `EntityRenderDispatcherAccessor` (PreviewLegacy) | yes | |
| `emotes/PlayerModelMixin` (emote pose) | yes | merged into the same config |
| `mixin/client/RenderTypeInvoker` | yes (copy) | the client copy is deleted with the old engine |
| `nametags/*` | no | client |

**Transition:** while both engines exist only **one** set of mixins may be active, or players get two layers. So
the cosmetico mixins arrive in the same step that switches the client to them (step 4), and the old ones (and their
configs `vibeyclient.cosmetics/emotes.mixins.json`) are removed in that same step. Before that step the cosmetico
jar has no mixin config (or an empty one).

**Entrypoints:**

- Fabric `fabric.mod.json`: `"client": ["dev.henny.cosmetico.mc.fabric.CosmeticoFabric"]` → registers the tick and
  calls `CosmeticoBoot.start()`; `"mixins": ["cosmetico.mixins.json"]`; depends `fabric-api` (tick events).
- NeoForge `neoforge.mods.toml` + `@Mod("cosmetico")` class `mc.neoforge.CosmeticoNeoForge` (client dist only),
  `[[mixins]] config = "cosmetico.mixins.json"`, `ClientTickEvent.Post` on `NeoForge.EVENT_BUS`.
- Init order: the client must call `Cosmetico.init(vibeyHost)` before the first tick. Use mod dependency ordering
  (`vibeyclient` depends on `cosmetico`), and make `init` replace the DEFAULT host if it already ran (the host only
  holds suppliers, so a late swap is safe).
- Builtin bundles: `content/dist` stays the source. It is copied into the **client** jar under
  `assets/vibeyclient/builtin` as today, and the client calls `items().loadIndex(..., "/assets/vibeyclient/builtin/",
  BUILTIN)`. cosmetico ships no content (it's a library); a demo bundle for its README only lives in
  `libs/cosmetico/src/test/resources`.

---

## 4. Version-specific code (`//?`) and where it lands

All `//?` blocks land in **mc**; core has none (check task). Count per file today:

| Old file (`//?`) | Version axes | New mc file |
|---|---|---|
| `Tex` (27) | Identifier/ResourceLocation 1.21.11; DynamicTexture ctor 1.21.5; RenderTypes 1.21.11/26.1; soft pipelines 1.21.11–<26.2, 26.1 ColorTarget/DepthStencil; 1.21.5 emissive set | `Tex`, `EntityRenderTypes`, `SoftPipelines`, `McIds` |
| `PreviewView` (22) | render state 1.21.2/1.21.9; PiP 1.21.6; `g.entity` 26.1; `EntityTypes` 26.2; skin lookup 1.21.9; cape strip 1.21.9 | `PreviewRenderer`, `PreviewSkin` |
| `PlayerRendererMixin` (16) | AvatarRenderer 1.21.9; hand item state 1.21.11 / 1.21.4 / 1.21.2; 1.21.1 render HEAD | mixin as is |
| `CosmeticLayer` (14) | submit 1.21.9 / render(state) 1.21.2 / entity 1.21.1 | `CosmeticLayer` |
| `WorldPass` (13) | submit + CameraRenderState (26.1 package) / render 1.21.2 / 1.21.1 | `WorldPass` |
| `MeshSink` (12) | buffer <1.21.9 (1.21.5 flush) / submit ≥1.21.9 | `BufferSink`, `SubmitSink`, `MeshSink` |
| `ShieldRendererMixin` (12), `ShieldDraw` (5) | ShieldSpecialRenderer 1.21.4; submit 1.21.9 / 26.1 | mixin, `ShieldDraw` |
| `GuiEntityMixin` (9) | GuiEntityRenderer 1.21.6; targets 26.1/26.2 | mixin |
| `LivingEntityRendererMixin` (8) | submit 1.21.9 + 26.1 camera state; render 1.21.2 | mixin |
| `SelfTest` (7) | pressLoad 1.21.9; overlay 26.2; Screenshot 26.2/1.21.6 | `SelfTestWorld`, `SelfTestShots` |
| `PoseCompat` (6) | isEmpty 1.21.5; mulPose matrix 26.3 | `PoseCompat` |
| `FirstPersonItemMixin` (6) | 26.3 class, 26.2 method | mixin |
| `PlayerModelMixin` (6) | setupAnim state 1.21.9/1.21.2, 1.21.1 entity + copyFrom | mixin |
| `EngineCompat` (5) | KEYSYM/KEYBOARD 26.3, isKeyDown 1.21.9 / 26.3 | the key part moves to client `Keys`; `McCamera` / `McSelf` have no `//?` |
| `ItemInHandLayerMixin` (5) | 1.21.9 / 1.21.4 / 1.21.2 / 1.21.1 cancel | mixin |
| `PreviewLegacy` (5) | <1.21.6, 1.21.2 split | `PreviewLegacy` |
| `TexPixels` (4), `SkinOverride` (4) | 1.21.3 pixel API; PlayerSkin 1.21.9 | same names |
| `RenderStateMixin` (3), `AbstractClientPlayerMixin` (2), `BoneRenderer` (2 imports), `ModelParts` (1) | RenderType package 1.21.11, root 1.21.2 | mixin; `BoneDraw`, `ModelPartFrames` |

`BoneRenderer`'s only `//?` is the `RenderType` import, so after the split `BonePose` (core) has none, and `BoneDraw`
(mc) keeps it.

Gradle: cosmetico needs the same Stonecutter target list (20 targets) as the client, plus the per-loader source
folders. New code is only *run* on fabric-1.21.11; every other target must compile (`--max-workers=2`, one target at
a time, through `slot.sh`).

---

## 5. Parity checklist

Existing tests:
- JUnit (`src/junit`): `StateWeightsTest` (5 tests), `OffsetWhenParseTest` (3 tests).
- Gametests (fabric-1.21.11): `CosmeticsGameTest`, `AuditBundlesGameTest` (+ `content/audit/*`, mock OTA catalog),
  `ShowcaseGameTest` (Polar Pals, states/offsetWhen/pet geometry), `WardrobeUiGameTest` (+ `PreviewTestScreen`).
- Runtime (other versions): `SelfTest` (`VIBEYCLIENT_AVATAR_TEST=true|world|devlink`), `DevTour`, `DevTourB`.

"New" = must exist (and pass) **before** the old code for that feature is deleted. Core JUnit tests run in
milliseconds through `javac` + the JUnit console, no Gradle.

| # | Feature / behaviour | Covered today by | New tests needed |
|---|---|---|---|
| P1 | `.vibey` parse: all fields, absolute pivots, defaults, warnings, forward compatibility (unknown fields/values) | Audit GT (expectWarning/expectThrows) | **core** `BundleReaderTest`: golden parse of every `content/dist/*.vibey` + `content/audit/*.vibey` (count bones/cubes/anims/events/warnings) — snapshot compared against the **old parser output** (dump once to JSON) |
| P2 | Validation errors: size 2 MiB, format, id regex/128, kind/slot/attach/rarity, references, cycles, unique names, texture/strip limits, 256 bones / 2048 cubes, sound 512 KiB, pet.scale, model required, emote rules, prices rejected | partly Audit GT | **core** `ValidationTest` (one minimal bundle per rule, table-driven) |
| P3 | `NeedsUpdate` (`minClientVersion` > host, `formatVersion` > 1) → metadata-only stub, greyed out, never rendered or equipped | Audit GT (needs_update, future_format, OTA stubs) | **core** `VersionGateTest` (+ `SemVerTest`: pre-release, build metadata, `v` prefix, junk) |
| P4 | Unknown kind/slot skipped and logged once | Audit GT (unknown_slot) | **core** in `ValidationTest`; client `CatalogItemsTest` (stub rules) |
| P5 | `offsetWhen` parse limits (8 rules, ±64, ±360, own slot) | `OffsetWhenParseTest` | move the test; **core** `OffsetBlendTest` (smoothstep easing ~0.3 s, armour-hidden counts as not worn, own slot ignored) |
| P6 | State weights glide>fly>sprint>walk>sneak>happy>idle_long>idle, budget 1, fallbacks | `StateWeightsTest` | move; **core** `StateWeightsInputTest` (empty optional animation = missing) |
| P7 | Motion: walk/fly/sneak/sprint/glide (fall > 14 ticks)/idle_long (200 ticks, slow in fast out)/happy; state start times | Showcase GT | **core** `MotionModelTest`, `StateStartsTest` |
| P8 | Pets: ground/shoulder/float follow, lag, teleport > 12 blocks, push out of the owner's footprint, petSide/halfWidth, petMove for walk | Showcase GT (petSide/halfWidth), Cosmetics GT | **core** `PetFollowerTest`, `PetGeometryTest` |
| P9 | Sampler: linear/catmullrom/step/bezier→linear, `pre` split keys, hold before/after, loop wrap | none (indirect via screenshots) | **core** `SamplerTest` (values from Blockbench 5.2 reference cases in `content/tools/render`) |
| P10 | Emote timing: 0.15 s fade in/out, loop/hold/once, stop, finished, reload on hot swap | Cosmetics GT (12 emotes mid-animation) | **core** `EmotePlayTest`, `EmoteControlTest` (fake clock) |
| P11 | Emote pose: six parts + root (Blockbench → model space, rotationZYX, model scale 0.9375), outer skin layer follows (1.21.1 copyFrom) | Cosmetics GT screenshots | **core** `EmotePartPoseTest`, `RootPoseTest` |
| P12 | Local emote: third person switch + restore, stopOnMove (keys, pushed > 0.5 blocks, after 4 ticks), hideHeldItems | Cosmetics GT ("emote switched camera") | **GT** `CosmeticoEmoteGameTest` (local emote camera + stop on move) |
| P13 | Others' emotes: `showOthersEmotes`, server rule `emotes` (previews still play) | none direct | **core** `EmoteVisibilityTest` (`emote(st)` rules) |
| P14 | Bone hierarchy: attach to six parts / root / world, root pivots relative to the part's rest pivot, child bones, rest rotation, normals with inverse scale | Cosmetics GT, real-Blockbench comparison shots | **core** `BonePoseTest` (matrix for a known bundle vs a matrix dump of the **old** BoneRenderer, ε 1e-5) |
| P15 | Mesh: Blockbench → model space, vanilla box UV, mirror, inflate, per-face UV (up/down swapped), omitted faces, cube rotation, texture lookup chain | Cosmetics GT, `content/dist/blockbench` shots | **core** `BundleMeshTest` (old vs new vertex arrays byte-equal for every content bundle) |
| P16 | Visibility events (last event ≤ t, default visible; idle keeps it unless another state dominates, else idle) | Audit GT (prop_pet idle+walk) | **core** `VisibilityTest`, `CosmeticMixTest` |
| P17 | Locators: on model bones and player bones, world positions per frame, fallback position before the first render | Audit GT (triple_prop_emote) | **core** `LocatorMathTest` |
| P18 | Events: particle (vanilla, sprite), sound (vanilla, embedded), crossed-time firing with loop wrap; idle events always, others while weight ≥ 0.5 | Audit GT (sparkle_hat), Cosmetics GT | **core** `EventRunnerTest` (recording `FxSink`), `FxMathTest` |
| P19 | Sprite particles (lifetime, size, gravity, emissive, billboard) | Audit GT screenshots | **core** `SpritePoolTest`, `SpriteQuadsTest` |
| P20 | Embedded sounds (stb_vorbis + OpenAL, mono positional, stereo not, max 16 sources, decoded on first use; no LWJGL during startup) | Audit GT (sparkle_hat sound, no crash) | GT check: log line "sound played" + no "Embedded sounds disabled" |
| P21 | Textures: HD, animated strips (frameTime, frames order, interpolate quantised ×8), lazy upload 3/frame, release after 30 s, evict on replace | Audit GT (faces_back strip + HD) | **core** `StripFrameTest`; GT: replaced bundle frees its textures (texture count before/after) |
| P22 | Render types: cutout no-cull, translucent, emissive, glow (emissive + translucent); soft-alpha pipelines (cutout 0.004) on 1.21.11 + 26.1 | Polar Pals / Showcase screenshots | GT screenshot comparison of the aurora/glow items old vs new (same `NNNN_sc_` names) |
| P23 | Additive / soft FX look (alpha cutoff, glow without depth write) | Showcase GT | as P22 |
| P24 | Capes: texture-only via the vanilla cape model + elytra (SkinOverride, showCape forced), cached per skin | Cosmetics GT (front/side/back) | GT: elytra shot with a cosmetico cape |
| P25 | Shields: 64×64 texture on the vanilla shield model, third person (ItemInHandLayer owner) + first person | Cosmetics GT "shields", DevTourB first-person shield | keep + **GT** first-person shield shot in `CosmeticoGameTest` |
| P26 | Armour hiding (`hideWithArmor` head/chest/legs/feet + global switch) | Cosmetics GT | **core** `ShownBundleTest` |
| P27 | First person: world renderer does not draw the local player, hands may (shield) | DevTourB | GT step (first person, shield, no cosmetics floating) |
| P28 | Hidden: invisible, spectator, `showOwn`/`showOthers`, slot toggles, server slot rules (`cosmetics.<plural>`) | none direct (ServerRulesTest covers parsing only) | **core** `SlotVisibilityTest`; client `SlotRulesTest` (ruleKey) |
| P29 | GUI: world things skipped inside vanilla GUI entity rendering (inventory) | none | GT: inventory screenshot with a pet equipped |
| P30 | Preview: no player entity, title screen + world, drag-rotate, auto-rotate, try-on overrides, preview emote, pet placement and span, happy while emoting, own cape choice in a world | `WardrobeUiGameTest`, `PreviewTestScreen`, Cosmetics GT | **core** `PreviewStateTest` (drag, span, pet placement); GT keeps the screenshots |
| P31 | Thumbnails: builtin classpath, catalog URL (disk cache, 4 parallel, single flight), dev push PNG (≤256², ≤256 KiB, re-push keeps), letter tile fallback | WardrobeUi GT | **core** `ThumbnailStoreTest`, `ThumbnailCheckTest` |
| P32 | DEV items: push (bytes/JSON/thumbnail), DEV > REMOTE > BUILTIN by id, hot replace keeps equip/emote, remove stops/unequips, ids, never sent anywhere | devlink manual + SelfTest devlink | **core** `DevItemServiceTest` (fake `LocalActions`, fake backend), `ItemRegistryTest` (priority merge, version bump, listeners, order) |
| P33 | Registry order (cosmetics by slot then sort then id, emotes last), lock-free reads | none | in `ItemRegistryTest` |
| P34 | OTA catalog (client): ETag/304 reuse, disk-first, same-hash reuse, 4 parallel downloads, sha check, swap on client thread, old version until swap, failed download keeps old, generation drop, new-items toast batch, needs-update stubs, creators incl. deleted | Audit GT OTA part, `HttpCacheTest`, `ConcurrencyTest` | client `CatalogApplyTest` (pure parts with a fake fetch), keep Audit GT |
| P35 | Signed loadouts (client): own token, verify server loadouts, wrong kid → refetch keys, lastValid, allowServerEmote, defaults | none in JUnit (relied on gametests + `LoadoutToken`) | client `RemoteVerifierTest` (keys from `backend/LoadoutKeys` test vector) — while LoadoutSync is split |
| P36 | Loadout persistence, adopt remote keeping local-only items, shared slots exclude DEV/unusable, wheel slots | Cosmetics GT, WardrobeUi GT | client `SelfLoadoutTest` |
| P37 | Hot reload of an equipped emote restarts it with the new bundle | Audit GT (OTA hot swap of an equipped item) | in `EmoteControlTest` |
| P38 | Errors never reach vanilla: layer/world pass catch + unwind the pose stack, 3 logs max | none | GT: a deliberately broken bundle (in test resources) equipped → no crash, "Pose stack not empty" never logged |
| P39 | Self-test (title previews, world front/guitar/back, devlink shots) | manual on other versions | keep; run once on fabric-1.21.11 and one older target after the switch |
| P40 | Standalone: cosmetico without Vibey Client renders a bundle | none | **GT** in `libs/cosmetico/src/gametest`: `CosmeticoStandaloneGameTest` (load a test bundle from test resources, set the loadout of a mannequin, screenshot) |

Screenshot parity: run `CosmeticsGameTest`, `AuditBundlesGameTest`, `ShowcaseGameTest` and `WardrobeUiGameTest` on the
old engine (baseline folder), again after step 4, and compare pairwise (`content/tools/montage.mjs` or a pixel diff
script). Remember: relay=off for gametests, the FPS cap, the gametest run dir reset and possible GPU TDR flakiness
(rerun before suspecting the code).

---

## 6. Step order (the client works after every step)

Each step = one commit (or a few) on its own branch, merged only when green. Pure steps use `javac` + JUnit console;
MC steps go through `slot.sh gradle -- :fabric-1.21.11:...`, one MC instance max.

**Step 0 – baseline (no code change).**
Dump golden data with the *old* engine: per-bundle parse summary JSON, mesh vertex arrays, bone matrices for fixed
inputs (a small dev-only dumper under `src/junit`, it reads the old classes). Take the gametest screenshot baseline.
Risk: none. It's needed for P1/P14/P15.

**Step 1 – pure model + parser into core.**
`libs/cosmetico` Gradle project (core source set only + JUnit; the infra agent already set up the libs skeleton),
split `model/*`, `parse/*`, `version/*` with the host port (`language`, `contentVersion`). New tests P1–P6, P9. The
client still uses the old `avatar.bundle`. Nothing switches yet.
Risk: low. Watch out: `Text.get()` now needs the host; the default host returns `"en"`.

**Step 2 – rest of the pure core.**
`registry/*`, `state/*`, `anim/*`, `sim/*`, `draw/*` (pure parts), `skeleton/*`, `mesh/*`, `fx/*` (EventRunner,
SpritePool), `thumbs/ThumbnailStore`, `dev/*`, `config`, `load/IndexLoader`, `Cosmetico` facade, ports
(`Log`, `Workers`, `MainQueue`, `FxSink`, `LocalActions`, `ThumbnailFetcher`). Tests P5–P19, P21 (core part),
P26, P28, P30–P33, P37 compared against the step-0 dumps. The client is still unchanged.
Risk: medium. `BonePose` must reproduce `PoseStack.Pose` semantics exactly (normal matrix with inverse scale and
renormalisation at emit) → the matrix dump comparison is mandatory.

**Step 3 – MC backend, dormant.**
`mc/*` (render, fx, preview, tick, compat, selftest), both loader entrypoints, `CosmeticoMixinPlugin`, but
**no mixin config active yet** and no tick work while no host enabled it (`Cosmetico.active == false`). The
cosmetico jar ships jar-in-jar in the client. Build fabric-1.21.11 and compile at least one target per `//?` axis
(1.21.1, 1.21.4, 1.21.8, 1.21.10, 26.1, 26.3, and one NeoForge).
Risk: medium. Two `RenderTypeInvoker` mixins on the same target class are fine (different names), but the soft
pipeline location id changes namespace → check `SoftPipelines` builds under `cosmetico:`.

**Step 4 – the switch (the big one, keep it mechanical).**
In one change:
1. The client implements `CosmeticoHost` (`VibeyCosmeticoHost`: Lang, BuildInfo.VERSION, `LocalWardrobe` as
   `LocalActions`, `CatalogThumbnails`) and calls `Cosmetico.init` early in client init.
2. `CosmeticsModule` / `EmotesModule` / `LoadoutSync` / UI / devlink / DevDriver/DevTour(B) / gametests move to the
   API (`avatar.*` imports → `dev.henny.cosmetico.*`). The catalog is split into `module/cosmetics/catalog/*`.
   Splits of `CosmeticsModule`, `LoadoutSync` and the catalog happen here (they are touched anyway).
3. `cosmetico.mixins.json` is activated, `vibeyclient.cosmetics.mixins.json` and `vibeyclient.emotes.mixins.json`
   are removed from `fabric.mod.json` / `neoforge.mods.toml`, and the old mixin classes are deleted (they reference
   the old engine).
4. Module enable/disable maps to `config().cosmetics` / `config().emotes` (cosmetico draws nothing while they are
   off, exactly as today).

Run all four cosmetics gametests + screenshot comparison + DevTourB on neoforge-26.3 (or the target the tour pass
uses) + `VIBEYCLIENT_AVATAR_TEST=true` on fabric-1.21.4.
Risk: **high** (see R1–R6). Rollback = revert the merge. The old `avatar/` package still exists, so the revert is
clean.

**Step 5 – delete the old engine.** (After the user's in-game OK.) Delete: `src/main/java/dev/henny/vibeyclient/avatar/**`
(66 files incl. `ItemLoader`, `SelfTest`, `DevItemsImpl`, `EmoteBridge`, `Wardrobe`, `preview/*`, `render/*`, `fx/*`),
`mixin/client/RenderTypeInvoker`, the old JUnit `src/junit/.../avatar/**` (golden writer `OldEngineGoldenTest` +
dumps, `StateWeightsTest`, `OffsetWhenParseTest`; the golden files stay as reference). Then: `docs/AVATAR.md` → pointer.
Remove `src/main/java/dev/henny/vibeyclient/avatar/**`, the client `RenderTypeInvoker`, `EmoteBridge`, and the old
JUnit tests (they moved). Update `docs/AVATAR.md` → short pointer to `libs/cosmetico/docs`, keep `FORMAT.md`.
Precondition: every row in §5 has its new test green.
Risk: low (the compiler finds leftovers). Grep the gametests, `content/tools` (log prefixes) and the Blockbench
plugin harness for `vibeyclient-avatartest` / `vibeyclient:dyn` before deleting.

**Step 6 – standalone + ports (later, needs r3/vui).**
`CosmeticoStandaloneGameTest` (P40), `FxSink` → r3, `McPreview` → vui component, a cosmetico settings page.

### Risks

- **R1 Double rendering / missing render** during the switch: old and new mixins must never be active together →
  one commit (step 4). Check: a gametest asserts the player renderer's layer list contains exactly one cosmetic
  layer.
- **R2 Two ticks**: today both `CosmeticsModule.tick` and `EmotesModule.tick` call `AvatarTicker.tick()`
  (deduplicated by a 25 ms nanoTime gate). cosmetico has its own single tick, so remove both calls in the client.
  Keep the 25 ms gate anyway (NeoForge + Fabric tick events can both fire in dev mixes).
- **R3 Namespace change** `vibeyclient:dyn/...` → `cosmetico:dyn/...`: `PreviewView.isOurs()` checks the
  `vibeyclient:` prefix to strip our own cape from the preview skin, so it must check `cosmetico:`. Any screenshot
  tooling or test that greps texture ids has to follow.
- **R4 Resource lifetime**: today `ItemLoader.register/swap` and `DevItemsImpl.remove` call
  `GpuBundles.evict` + `EmbeddedSounds.evict` by hand. In cosmetico the registry fires "bundle replaced/removed" and
  the mc backend evicts (one place). Test: P21 GT texture count.
- **R5 Self-test vs. host loadout**: the client rewrites the self loadout every tick (`applySelf`), which would
  undo whatever the self-test sets directly. So the self-test (and DevDriver) must use `Loadouts.override` (the
  layer the client never touches) or `LocalActions`.
- **R6 Thread model**: the old code mixes `Tasks.onMain` (client queue) with `pendingBuiltin` handover because
  NeoForge loads mods before the game exists. cosmetico's `MainQueue` must only be drained from its own tick, and
  `init` must not touch LWJGL or textures (startup crash loop known from the cosmetics engine notes).
- **R7 Shared static scratch state** (`BoneRenderer` arrays, `AvatarDraw.MIX`, `locatorTarget`): the code is
  render-thread only and not reentrant. GUI previews draw inside world frames (PiP). Keep the scratch objects in the
  mc pass objects, not in core singletons, and keep `WorldPass.gui` semantics.
- **R8 `minClientVersion` meaning**: it's the Vibey Client version in FORMAT.md/SPEC. With `contentVersion()` from
  the host nothing changes for Vibey. A standalone cosmetico has to report a compatible number (decision for the
  coordinator: cosmetico versions follow the client's content version, or FORMAT.md gets a `minEngineVersion`
  later).
- **R9 Mod conflicts**: the `VibeyMixinPlugin` conflict gating for the `cosmetics`/`emotes` packages has to move
  into `CosmeticoMixinPlugin`, or cosmetico mixins apply next to another cosmetics mod that Vibey used to step aside
  for.
- **R10 Build load**: 20 targets × a new lib. Only fabric-1.21.11 runs; the others compile one by one through the
  slot script. Never `buildAll`.

---

## 7. Docs (`libs/cosmetico/docs`, replaces the javadoc)

- `README.md` (lib root): what it is, a 30-second example (`Cosmetico.init`, `items().loadIndex`, `loadouts().set`).
- `docs/architecture.md`: core vs mc, ports, threads (client thread, workers, render thread), tick order.
- `docs/rendering.md`: coordinates (Blockbench → model space), attach points, passes (attached/world), render types
  and soft pipelines, per-version render path table (from AVATAR.md).
- `docs/animation.md`: sampler rules, cosmetic states + weights, offsetWhen, emotes (fade, loop/hold), pose
  mapping.
- `docs/effects.md`: event runner timing, particles/sprites/sounds, `FxSink`.
- `docs/items.md`: sources/priority, registry, dev items, thumbnails, needs-update.
- `docs/preview.md`, `docs/testing.md` (JUnit, gametests, self-test, golden dumps).
- `docs/versions.md`: the `//?` table from §4.

The format itself stays in the client's `docs/FORMAT.md` (creators' guide, shared with the backend and the
Blockbench plugin). The cosmetico docs link to it.
