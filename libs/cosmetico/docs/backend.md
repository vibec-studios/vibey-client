# cosmetico Minecraft backend

The version-specific part of cosmetico: `libs/cosmetico/src/main/java/dev/henny/cosmetico/mc/**`. It turns game state
into core inputs and core outputs into draw calls, sounds and particles. Everything that differs between Minecraft
versions lives here, in small classes with Stonecutter `//?` blocks; the core (`src/core`, see [core.md](core.md)) has
none.

## Packages

| Package | Job | Classes |
|---|---|---|
| `mc` | Mod id, logger, `init` from the loader entrypoints | `CosmeticoMod` |
| `mc.fabric` / `mc.neoforge` | Loader entrypoints: init + own client tick (`END_CLIENT_TICK` / `ClientTickEvent.Post`) | `CosmeticoFabric`, `CosmeticoNeoForge` |
| `mc.boot` | Ports and the default host | `CosmeticoBoot`, `McResources`, `McDefaultHost`, `Slf4jLog` |
| `mc.tick` | Everything that runs once per client tick | `CosmeticoTick`, `McTicker`, `LocalEmotes`, `MoveCheck` |
| `mc.render` | Bones, meshes, textures, render types, the two render passes | see below |
| `mc.player` | Hooks between the vanilla player renderer and the engine | `PlayerSnapshot`, `EmotePoseHook`, `RootPoseHook`, `CapeShieldTextures`, `SkinOverride`, `ShieldDraw` |
| `mc.fx` | Effect sink for animation events | `VanillaFx`, `VanillaIds`, `Sprites`, `SpriteRender`, `EmbeddedSounds`, `OggDecoder`, `AlSources` |
| `mc.preview` | GUI preview of the local player | `McPreview`, `PreviewRenderer`, `PreviewSkin`, `PreviewLegacy` (< 1.21.6) |
| `mc.thumbs` | Item thumbnails as GUI textures | `McThumbnails`, `TexThumbnails`, `ResourceAnchors`, `GuiBlit` |
| `mc.compat` | Small version shims | `McIds`, `McSelf`, `McCamera`, `PoseCompat`, `Errors` |
| `mc.mixin` | The render hooks (config `cosmetico.mixins.json`) | see "Mixins" |

## Start-up and init order

Fabric does not run library entrypoints in dependency order, and NeoForge constructs mods before
`Minecraft.getInstance()` exists. So nothing depends on order:

- `CosmeticoBoot.ensure()` is idempotent. It installs the backend ports into the engine once
  (`Engine.resources(McResources)`, `Engine.backendFx(VanillaFx)`) and, if no host called `Cosmetico.init` yet, activates
  the engine with `McDefaultHost` (language from the game options, content version `0.3.1`, SLF4J logger `cosmetico`).
- It runs from both loader entrypoints, from every client tick and from `McPreview.create()`.
- A host mod (Vibey Client) calls `Cosmetico.init(host)` whenever it likes; a later call replaces the default host. The
  host only hands out suppliers, so a late swap is safe.
- Nothing at start-up touches textures, OpenAL or stb_vorbis. Textures upload on first draw (at most 3 bundles per
  frame), sounds decode on the worker pool when a bundle is first drawn or an emote starts.

## Tick (`CosmeticoTick`, client thread, once per client tick)

1. `CosmeticoBoot.ensure()`; nothing else runs until the engine is active.
2. Drain `Engine.main` (work posted from worker threads: index loads, evictions from other threads).
3. `McTicker`: `GpuCache.tick` (animated texture strips, release after 30 s unused), sprite pools, finished OpenAL
   sources; then per player in the level that has an avatar: `MotionModel.step`, `PetFollower.step`,
   `EmoteControl.tick`, `EventRunner.tick` (events go to `VanillaFx`). Without a level only emote timing runs.
   A 25 ms gate keeps it at one step per tick even if two tick events fire.
4. `LocalEmotes.tick`: stop-on-move for the local player's emote.

## Rendering

Two passes per player, both reached from mixins and both never throwing into vanilla (exceptions are logged three
times, the pose stack is unwound with `PoseCompat.unwind`):

- **World pass** (`WorldPass`, from `LivingEntityRendererMixin` at the start of the living-entity render/submit):
  world-attached bones (pets with their own smoothed position, auras that stay level) through `WorldDraw`, then the
  avatar's sprite particles (`SpriteRender`). Skipped inside vanilla's GUI entity rendering (`GuiDepth`, set by
  `GuiEntityMixin`) except for previews.
- **Attached pass** (`CosmeticLayer`, a render layer added to the player renderer): bones attached to the posed vanilla
  parts through `AttachedPass`; shoulder pets sit on the body part.

Both passes use `BoneDraw`: the core `BonePose` computes every bone matrix from the pose stack's current matrix,
`ModelPartFrames` (vanilla `ModelPart.translateAndRotate` on a scratch pose stack, so the math is bit-equal to the old
engine) and the `Mix` from `CosmeticMix`; `BoneDraw` then submits each visible bone's `MeshPart`s with the render type
for its `MeshMode`:

| Mode | Render type | Light |
|---|---|---|
| `CUTOUT` | `entityCutoutNoCull` (26.1+: `entityCutout`) | world |
| `TRANSLUCENT` | 1.21.11–26.1: own soft-alpha pipeline (`ALPHA_CUTOUT 0.004`); else `entityTranslucent` | world |
| `EMISSIVE` | `entityTranslucentEmissive` (1.21.5: flushed right away, lightmap bound) | full bright |
| `GLOW` | 1.21.11–26.1: own pipeline without depth write; else as `EMISSIVE` | full bright |

`MeshSink` hides the submit model: `BufferSink` (< 1.21.9, straight into a `MultiBufferSource`) and `SubmitSink`
(≥ 1.21.9, custom geometry from a ring of reusable callbacks). Vertices go through `VertexEmit` (normals renormalised
like `PoseStack` does).

Scratch objects (`BonePose`, `Mix`, `LocatorCollector`) live in the pass objects (`RenderPasses`), never in the core.

### Textures

`Tex` registers a `DynamicTexture` under `cosmetico:dyn/<bundle>_<texture>_<n>`. `GpuBundle` holds a bundle's textures
and its `Rig` (mesh + tracks, compiled on a worker by `GpuCache.prepare`). Animated strips (`AnimatedStrip`) keep the
whole strip on the CPU and copy the current frame (`StripFrame` from the core) into a frame-sized texture, so capes and
shields drawn by vanilla models animate too. Replaced or removed bundles are evicted through the registry listener
(`McResources.evict`, on the client thread).

### Player hooks

- `PlayerSnapshot` fills the avatar's `FrameSnapshot` at extract time (position, body yaw, armour, crouch, hidden).
- `EmotePoseHook` blends the emote over vanilla's pose after `setupAnim`; `RootPoseHook` applies the emote root bone in
  `setupRotations` and decides `hideHeldItems`.
- Capes: `SkinOverride` puts the cape texture into the player's skin (vanilla cape model with physics, elytra).
- Shields: `ShieldDraw` replaces vanilla's shield draw while a player with a shield cosmetic is drawn (`owner` set by
  the item-in-hand hooks, third and first person).

## Mixins (`cosmetico.mixins.json`, plugin `CosmeticoMixinPlugin`)

| Mixin | Target | Job |
|---|---|---|
| `PlayerRendererMixin` | `AvatarRenderer` (1.21.9+) / `PlayerRenderer` | add `CosmeticLayer`, snapshot into the render state, emote root, 1.21.1 world pass |
| `LivingEntityRendererMixin` | `LivingEntityRenderer` | world pass |
| `RenderStateMixin` | player render state | carries the `Avatar` (`AvatarCarrier`) |
| `PlayerModelMixin` | `PlayerModel` | emote pose |
| `AbstractClientPlayerMixin` | `AbstractClientPlayer.getSkin` | cape texture |
| `ItemInHandLayerMixin`, `FirstPersonItemMixin`, `ShieldRendererMixin` | held items | shield cosmetic |
| `GuiEntityMixin` | GUI entity renderer | `GuiDepth` |
| `LivingEntityRendererAccessor`, `EntityRenderDispatcherAccessor`, `RenderTypeInvoker` | – | accessors |

`defaultRequire` is 0 (targets differ per version). The plugin skips the hooks (accessors stay) when a conflicting mod
is installed (`CosmeticoMixinPlugin.CONFLICTS`, empty today) or with `-Dcosmetico.disableMixins=true`.

## Effects

`VanillaFx` is the backend `FxSink`: vanilla particles by id (spread randomised here, like the old engine), vanilla
sounds as `SimpleSoundInstance` on the players source, sprite particles into `Sprites.pools()` (one pool per avatar
UUID, drawn by `SpriteRender` in the world pass) and embedded OGG sounds (`EmbeddedSounds`: stb_vorbis decode on the
worker pool, at most 16 OpenAL sources, mono = positional; volume × master × players). A host can replace the whole
sink through `CosmeticoHost.effects()`.

## Previews and thumbnails

- `McPreview.create()` returns a core `PreviewView`; `McPreview.draw(graphics, view, x, y, w, h, delta)` runs
  `PreviewState.frame` and draws through vanilla's player renderer with a synthetic render state (1.21.6+: entity
  picture-in-picture; older: `PreviewLegacy`). The skin comes from the player (in a world) or a skin lookup (title
  screen); a cosmetico cape in the skin is stripped when the preview shows no cape (`McIds.isOurs`).
- `McThumbnails.draw(graphics, item, x, y, size)` draws an item's thumbnail once loaded (classpath paths through
  `ResourceAnchors`, URLs through the host's `ThumbnailFetcher`, dev pushes from `DevThumbnails`). Mods that ship their
  own index call `McThumbnails.anchor(SomeClassOfTheirJar.class)` so NeoForge finds resources in their module.

## Versions

Every `//?` axis in the backend: `Identifier`/`ResourceLocation` (1.21.11), render state (1.21.2), submit API (1.21.9),
`AvatarRenderer` (1.21.9), GUI picture-in-picture (1.21.6), `DynamicTexture` ctor (1.21.5), pixel API (1.21.3),
`RenderTypes` package (1.21.11), soft pipelines (1.21.11–26.1), `GuiGraphicsExtractor` (26.1), `EntityTypes` (26.2),
`gui.screen()` (26.2), first-person hands renderer (26.3), `mulPose(Matrix4f)` (26.3), loader (`fabric`/`neoforge`).
Compiled on fabric 1.21.1 / 1.21.4 / 1.21.5 / 1.21.11 / 26.1 / 26.3 and neoforge 1.21.11 / 26.3; run on fabric 1.21.11.

## Tests

- `libs/cosmetico/src/gametest`: `CosmeticoStandaloneGameTest`, cosmetico without Vibey Client
  (`./gradlew :cosmetico:fabric-1.21.11:runClientGameTest`, shots in `build/review/cosmetico/standalone`).
- The client's `CosmeticoParityGameTest` (`-Dvibeyclient.test.only=CosmeticoParityGameTest`) takes the same 31 shots as
  the old engine's baseline; `libs/cosmetico/tools/shotdiff.py <a> <b> [maskdir]` compares two shot folders. Time is
  pinned with `Clock.pin` and the sprite/particle randoms are seeded, see [testing.md](testing.md).
