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
| `avatar.anim` | `Sampler` (Blockbench interpolation incl. split keys and loop wrap), `EmotePlay` (fade 0.15 s, loop/hold/once) |
| `avatar.render` | `BundleMesh` (Blockbench → model space, vanilla box UV, per-face UV, cube rotation), `GpuBundle(s)` (textures, animated strips copied per frame), `BoneRenderer`, `AvatarDraw`, `MeshSink` (buffer source < 1.21.9, submit collector ≥ 1.21.9), `AvatarHooks`, `CosmeticLayer`, `WorldPass`, `SkinOverride` (capes via the vanilla cape model + elytra), `ShieldDraw` |
| `avatar.fx` | `Effects` (event runner), `SpriteParticles`, `EmbeddedSounds` (stb_vorbis + OpenAL, positional) |
| `avatar.preview` | `AvatarPreview` service: 3D player preview without a player entity (title screen too) |

## Services

| Service | Provider | Use |
|---|---|---|
| `avatar.Wardrobe` | cosmetics module (while enabled) | equip/unequip, `canUse`, emote wheel slots, play/stop emotes |
| `avatar.preview.AvatarPreview` | cosmetics module | `create()` a `View`, call `draw(g, x, y, w, h, delta)` every frame, forward mouse with `mouse(...)`; `playEmote`, `override(slot, id)` for try-on |
| `avatar.DevItems` | cosmetics module | devlink pushes (`push`, `remove`, `ids`) |

`AvatarItems.version()` changes on every registry change (poll it in UIs). `Thumbnails.draw(g, item, x, y, size)`.

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
- Other versions: `VIBEYCLIENT_AVATAR_TEST=true ./gradlew :fabric-1.21.4:runClient` (title-screen previews) or
  `VIBEYCLIENT_AVATAR_TEST=world ./gradlew :fabric-26.3:runClient --args="--quickPlaySingleplayer vibeytest"`
  (copy `versions/fabric-1.21.11/build/run/vibeytest-world` to `run/saves/vibeytest` first). Screenshots land in
  `versions/<target>/run/screenshots`, log lines start with `[vibeyclient-avatartest]`.
