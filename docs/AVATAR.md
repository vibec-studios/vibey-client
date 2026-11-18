# Cosmetics & emotes engine

How the client turns `.vibey` bundles (SPEC §5) into things on players. Modules: `cosmetics` (loadout, sync,
wardrobe), `emotes` (wheel, local plays), `devlink` (Blockbench live link). The engine itself lives in
`dev.henny.vibeyclient.avatar` and has no module dependencies.

> See also: [FORMAT.md](FORMAT.md) (the `.vibey` format for creators) · [SPEC §5–§8](SPEC.md#5-the-vibey-asset-format-v1)
> (format, sync, backend, live link) · [BLOCKBENCH.md](BLOCKBENCH.md) · [CONTENT.md](CONTENT.md) (starter set) ·
> [docs index](README.md)

## Packages

| Package | What |
|---|---|
| `avatar` | `AvatarItems` (registry, DEV > CATALOG > BUILTIN by id), `ItemLoader` (builtin bundles from the jar, backend catalog with ETag + SHA-256 asset cache in `<gameDir>/vibeyclient/cache`), `AvatarState`/`AvatarStates` (per player: loadout, emote, pet, movement weights), `AvatarTicker`, `Thumbnails`, `DevItemsImpl`, `EngineCompat`, `SelfTest` |
| `avatar.bundle` | `Bundle` (immutable model), `BundleParser` (validation per SPEC §5, warnings, forward compatible) |
| `avatar.anim` | `Sampler` (Blockbench interpolation incl. split keys and loop wrap), `EmotePlay` (fade 0.15 s, loop/hold/once), `StateWeights` (cosmetic state blending, unit tested) |
| `avatar.render` | `BundleMesh` (Blockbench → model space, vanilla box UV, per-face UV, cube rotation), `GpuBundle(s)` (textures, animated strips copied per frame), `BoneRenderer`, `AvatarDraw`, `MeshSink` (buffer source < 1.21.9, submit collector ≥ 1.21.9), `AvatarHooks`, `CosmeticLayer`, `WorldPass`, `SkinOverride` (capes via the vanilla cape model + elytra), `ShieldDraw` |
| `avatar.fx` | `Effects` (event runner), `SpriteParticles`, `EmbeddedSounds` (stb_vorbis + OpenAL, positional) |
| `avatar.preview` | `AvatarPreview` service: 3D player preview without a player entity (title screen too) |

## Services

| Service | Provider | Use |
|---|---|---|
| `avatar.Wardrobe` | cosmetics module (while enabled) | equip/unequip, `canUse`, emote wheel slots, play/stop emotes |
| `avatar.preview.AvatarPreview` | cosmetics module | `create()` a `View`, call `draw(g, x, y, w, h, delta)` every frame, forward mouse with `mouse(...)`; `playEmote`, `override(slot, id)` for try-on |
| `avatar.DevItems` | cosmetics module | devlink pushes (`push` with an optional thumbnail PNG, `remove`, `ids`) |

`AvatarItems.version()` changes on every registry change (poll it in UIs). `Thumbnails.draw(g, item, x, y, size)`
(dev items: the thumbnail of their last push that had one, `ThumbnailCache.putDev`; else the letter tile).

## Cosmetic states

`AvatarTicker` derives per avatar (smoothed 0..1): `walk` (walk animation speed), `fly` (creative or elytra), `sneak`,
`sprint` (sprinting and moving), `glide` (elytra, or falling > 14 ticks), `idle_long` (no movement, on the ground, no
emote for 200 ticks; eases in over ~1.5 s) and `happy` (an emote plays). `AvatarDraw.weights` hands them to
`StateWeights.compute`: priority glide > fly > sprint > walk > sneak > happy > idle_long > idle with a budget of 1;
missing or empty animations weigh 0 (fallbacks: no `glide` → elytra plays `fly`, no `sprint` → `walk`). The
optional states start at `AvatarState.stateStart` (per avatar), the others use the global `Clock`. Pets use their own
movement (`petMove`) instead of `walk`. Ground pets keep `AvatarTicker.petSide(bundle)` to the player's right (player
radius 0.3 + the pet's widest body cube × `pet.scale` + 0.3, at least 0.95) and are pushed out of the player's
footprint so a big pet never walks through its owner.

`cosmetic.offsetWhen` (`Bundle.offsetWhen`): `AvatarDraw.offsets` eases a weight per (item slot, other slot) towards
1 while the other slot shows an item (smoothstep, ~0.3 s) and adds the blended offset/rotation to every root bone
(`BoneRenderer.Mix.rootOffset`).

Render types (`Tex`): cutout (opaque), translucent and emissive as before; emissive + translucent bones use `glow()`.
On 1.21.11 and 26.1 translucent and glow are the engine's own pipelines (`Tex.SoftPipelines`, created through the
`RenderTypeInvoker` mixin): vanilla entity shader with an alpha cutout of 0.004 instead of 0.1; glow is full-bright,
without per-face shading and depth writes. Other versions (older, and 26.2+ with the new bind-group pipeline API) keep vanilla's entity render types.

## Over-the-air updates (ItemLoader)

The catalog (`GET /api/v1/cosmetics/catalog`, ETag) is fetched on start, every 10 min + 0–3 min jitter, 3 s after a
world join, 0–5 s after the gateway event `catalog.updated` and after a reconnect whose `ready.catalogVersion`
differs (all through `ItemLoader.requestCatalog`, never more than every 15 s). A 304 does nothing. On a change:
entries whose asset hash is unchanged keep their parsed `Bundle` (no re-parse, no texture re-upload); new/changed
assets come from the disk cache or are downloaded on workers (4 in parallel, sha256-checked, cached by hash), parsed
and their meshes compiled, then everything is swapped in with one `AvatarItems.putAll` on the client thread
(replaced bundles release their GPU textures and sounds; the new textures upload lazily, 3 per frame). Until the
swap the old version keeps rendering; a failed download keeps it until the next poll. Loadouts store ids, so equipped
items stay equipped. `ItemLoader.onNewItems` (first load excluded) and `LoadoutSync.reloadOwned` (grants after
`catalog.updated`) feed one "New cosmetics available" toast per batch.

Items that need a newer client (`minClientVersion` on the catalog item or in the bundle, `formatVersion` > 1) become
metadata-only items (`bundle() == null`, `needsUpdate()`): listed greyed out with "Update Vibey Client", never
rendered or equipped. Unknown kinds/slots are skipped and logged once; parse failures are logged once per hash.

## Render path per version

| Versions | Player data | Cosmetics | World things (pets, auras, particles) | GUI preview |
|---|---|---|---|---|
| 1.21.1 | entity | `CosmeticLayer` (entity) | `PlayerRenderer.render` HEAD | own PlayerModel draw (`PreviewLegacy`) |
| 1.21.2 – 1.21.5 | `PlayerRenderState` + carrier | layer `render(state)` | `LivingEntityRenderer.render` HEAD | vanilla renderer with a synthetic render state, drawn into the GUI |
| 1.21.6 – 1.21.8 | same | same | same | vanilla entity picture-in-picture (`submitEntityRenderState`) |
| 1.21.9 – 26.3 | `AvatarRenderState` + carrier | layer `submit` → custom geometry | `LivingEntityRenderer.submit` HEAD | PiP (`GuiGraphicsExtractor.entity` from 26.1) |

Emote pose: `PlayerModel.setupAnim` TAIL (six parts, blended by the fade weight), `root` in `setupRotations` TAIL
(the pose stack is a Blockbench-like frame at the feet there). Held items are hidden by clearing the render state
(1.21.2+) or cancelling the item layer (1.21.1).

## Coordinates

Blockbench space (right arm +X) → model space `x' = -x, y' = 24 - y, z' = z`; rotations `(-x, -y, z)` as
`rotationZYX`; root bones on a part are placed relative to the part's rest pivot. Per-face UV: side faces map like
vanilla box UV regions, Blockbench's up/down rectangles are vanilla's with both corners swapped. Checked against the
real-Blockbench screenshots in `content/dist/blockbench/`.

## Testing

- `./gradlew :fabric-1.21.11:runClientGameTest` – `CosmeticsGameTest`: mannequin player with every slot (front/side/back),
  12 emotes mid-animation, shields, local player emote with camera switch, previews on the title screen and in a world.
- `AuditBundlesGameTest` (same run): the bundles in `content/audit/` (built by `content/tools/build-audit.mjs`) combine
  features in unusual ways – a hat with a catmull-rom orbit, a locator particle loop and an embedded sound, a pet with
  props and visibility toggles in `idle` *and* `walk`, per-face textures (animated interpolated strip + HD), wings with a
  future attach point, an emote with three props on different bones and locators on props and player bones, and
  bundles for a newer client / with an unknown slot. Then OTA against a local mock catalog: hot swap of an equipped
  item, new community items with creators, needs-update stubs, unknown kinds, 304 reuse, one "new items" batch.
- Other versions: `VIBEYCLIENT_AVATAR_TEST=true ./gradlew :fabric-1.21.4:runClient` (title-screen previews) or
  `VIBEYCLIENT_AVATAR_TEST=world ./gradlew :fabric-26.3:runClient --args="--quickPlaySingleplayer vibeytest"`
  (copy `versions/fabric-1.21.11/build/run/vibeytest-world` to `run/saves/vibeytest` first). Screenshots land in
  `versions/<target>/run/screenshots`, log lines start with `[vibeyclient-avatartest]`.
- Whole client on any version/loader: `./gradlew :neoforge-1.21.5:runClient "-Dvibeyclient.tour=<absolute outdir>"`
  (`dev/DevTour`, dev environment only; env `VIBEYCLIENT_TOUR` works too). Title, onboarding, Vibey menu, a fresh flat
  world, every slot on the local player (third person front/back), wave/guitar/seal_summon mid-animation, wardrobe,
  emote wheel, social sidebar, lyrics sky (Fabric), host screen, pause menu; one screenshot per step into the outdir,
  log lines `[vibeyclient-tour] PASS|FAIL <step>` (plus errors other code logged during the step) and a `SUMMARY`.
- Second tour, used for the 1.21.6+ / 26.x runtime pass (`dev/DevTourB`, dev workspace only, own switch so it never
  runs together with `DevTour`): `VIBEYCLIENT_TOUR_B=<outdir> ./gradlew :neoforge-26.3:runClient` (or `-Dvibeyclient.tourB=<outdir>` via
  `JAVA_TOOL_OPTIONS`; relative paths resolve against the game dir). Title/onboarding → Vibey menu → flat creative
  world `saves/vibeytour` (created on the first run) → one item per slot front/back/first person → wave, guitar,
  seal_summon → wardrobe, emote wheel, social sidebar, lyrics in the sky (Fabric), pause menu, host screen → quit.
  Also the first-person shield and NeoForge's mod-loading warning screen. Logs `[vibeyclient-tourb] PASS|FAIL <step>` (a step that logged an ERROR fails), writes `NN_<step>.png` and
  `tour.txt` to the out dir.
