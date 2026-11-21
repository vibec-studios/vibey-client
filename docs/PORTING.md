# Porting modules to every target

The client builds 20 jars (Fabric and NeoForge × 1.21.1 … 1.21.11, 26.1 … 26.3, see `settings.gradle`). A module
runs on a target when `gradle/modules.gradle` lists it there. This page says which modules run where, what still
differs per version, and which shared helpers a port should use instead of calling the version-specific API.

## Where modules run

| Module | Targets | Not everywhere |
|---|---|---|
| `hud` (HUD editor, widgets, share codes, templates, vanilla boss bar / title / scoreboard / tab list) | all 20 | – |
| `info` (light level, chunk borders, block/mob card, food gain, tool warning, TNT timer) | all 20 | – |
| `pvp` (zoom, toggle sprint/sneak, crosshair editor) | all 20 | block outline, hit colour, low fire and lower shield only on 1.21.11 (`PvpExtras.RENDER_TWEAKS`); elsewhere the settings are hidden |
| `camera` (freelook, orbit view) | all 20 | motion blur only on 1.21.11 (`MotionBlur.SUPPORTED`); elsewhere the setting is hidden |
| `waypoints`, `chat`, `inventory`, `servers`, `social.streamer`, `visuals`, `clips`, `cinematic`, `screens`, `companion` | Fabric 1.21.11 | still to port |

"Only on 1.21.11" means the render code those features hook into (block outline render state, submit node
collector, post chains) was rebuilt by Mojang several times between 1.21.1 and 26.3. They are gated with Stonecutter
(`//? if >=1.21.11 && <26.1`) so the rest of the module runs everywhere; widen the condition once a version's hook is
written and tested.

## Shared helpers (use these, not the per-version API)

| Need | Helper | Why |
|---|---|---|
| Replace or hide a vanilla HUD part (effects, boss bar, title, scoreboard, tab list) | `client/hud/VanillaLayers.replace(Layer, (gfx, vanilla) -> handled)` | Fabric's `HudElementRegistry` only exists from 1.21.6 and not on NeoForge. Two core mixins (`mixin/client/GuiLayersMixin`, `BossOverlayLayerMixin`) hook `Gui` (`Hud` from 26.2, `extract*` from 26.1) on every target. Return `true` when you drew it yourself; call `vanilla.run()` to draw vanilla's part (for example moved by `Gfx.translate`). |
| The object that owns titles, boss bar, tab list, overlay message | `client/hud/HudAccess` | `Minecraft.gui` up to 26.1, `Minecraft.gui.hud` from 26.2 |
| Clicks and wheel on any screen | `client/ScreenInput.onClick(...)`, `onScroll(...)` | Fabric screen events vs NeoForge `ScreenEvent`; button numbers come as GLFW numbers on 26.3 too |
| Items, sprites, effect icons, player faces, text with shadow | `Gfx.item`, `Gfx.sprite(Object id, …)`, `Gfx.effectIcon`, `Gfx.face`, `Gfx.text(Component, x, y, color, shadow)` | `GuiGraphics` / `GuiGraphicsExtractor`, `RenderType` / `RenderPipeline`, `MobEffectTextureManager` / `Gui.getMobEffectSprite` / `Hud.getMobEffectSprite` |
| Sprite and texture ids | `Compat.id(namespace, path)` stored as `Object` | `ResourceLocation` up to 1.21.10, `Identifier` from 1.21.11 |
| Profile name / id, window handle | `client/GameCompat` | authlib records from 1.21.9, `Window.handle()` from 1.21.9 |
| The open screen | `ClientCompat.screen()` | `Minecraft.screen` up to 26.1, `Minecraft.gui.screen()` from 26.2 |

## Things that changed between versions (seen while porting)

- 1.21.2: `Input` record and `ClientInput.keyPresses` (before: fields on `Input`, sprint read from the key).
  Toggle sprint on 1.21.1 reports the key as held through `KeyMapping.isDown` (`pvp/mixin/KeyMappingToggleMixin`).
- 1.21.2: `GameRenderer.getFov` returns `float` (before `double`); 26.1 moved the FOV into `Camera.calculateFov`.
- 1.21.2 / 1.21.6: blits take a `RenderType` factory, then a `RenderPipeline`. Inverted fills: `RenderType.guiTextHighlight`
  (up to 1.21.5) and `RenderPipelines.GUI_TEXT_HIGHLIGHT` (1.21.6–1.21.8) with black, `GUI_INVERT` from 1.21.9.
- 1.21.4: `BlockState.getCloneItemStack(level, pos, includeData)` (before: on `Block` with the state).
- 1.21.5: `MobEffects.SPEED` / `STRENGTH` (before `MOVEMENT_SPEED` / `DAMAGE_BOOST`).
- 1.21.9: `DebugScreenEntries` (before `DebugRenderer.switchRenderChunkborder()`), `MouseHandler.onButton`
  (before `onPress`), `PlayerSkin` moved to `world.entity.player`.
- 1.21.11: `ResourceKey.identifier()` (before `location()`), `EnderDragonPart` moved to `boss.enderdragon`.
- 26.1: `Camera.alignWithEntity` replaces `Camera.setup`; `Level.getOverworldClockTime()` replaces `getDayTime()`;
  `IntProvider.maxInclusive()`; `SpawnEggItem.byId` returns an `Optional<Holder<Item>>`.
- 26.2: HUD code lives in `net.minecraft.client.gui.Hud`; team colours are `Optional<TeamColor>`.
- 26.3: native mouse buttons are SDL numbers (`Keys.mouseFromNative` / `mouseToNative`).

## How to port the next module

1. Widen its entry in `gradle/modules.gradle` (`'all'` or a list).
2. Compile a spread of targets, a few at a time (`--max-workers=2`):
   `./gradlew --max-workers=2 --continue :fabric-1.21.1:compileJava :fabric-1.21.5:compileJava :fabric-1.21.8:compileJava
   :fabric-26.1:compileJava :fabric-26.3:compileJava :neoforge-1.21.1:compileJava :neoforge-26.3:compileJava`.
   These seven cover every API break listed above; then all 20.
3. Replace Fabric API calls with the helpers above (or add a helper there when two modules need the same thing).
4. Mixins: gate the body, not the class (`//? if …` inside the class; keep `@Mixin` on a class that exists on every
   version), because the mixin config lists the class on every target. Put block comments outside gated code:
   Stonecutter comments a gated block out with `/* … */`.
5. Features whose hook is not written for a version: a constant (`PvpExtras.RENDER_TWEAKS`, `MotionBlur.SUPPORTED`),
   `visibleWhen(...)` on their settings and the `…Active()` check, so the page never shows a switch that does nothing.
