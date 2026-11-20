# PvP & View module (`pvp`)

Small PvP basics and view tweaks, **Fabric 1.21.11 only for now** (`gradle/modules.gradle` leaves
`module/pvp/**`, `vibeyclient.pvp.mixins.json`, `i18n/*/pvp.json` and its `fabric.mod.json` line out of every other
target; `BuiltinModules` registers the module by reflection when the class is there). Code:
`src/main/java/dev/henny/vibeyclient/module/pvp`.

> See also: [MODULES.md](MODULES.md) · [UI.md](UI.md) · server rules: `api/ServerRules` (vibe-launcher
> `docs/PLAY-LINKS.md` §5)

**No advantage beyond vanilla.** Zoom is a spyglass-like view, toggle sprint/sneak only hold the vanilla keys
for you (vanilla has the same option in Controls), everything else is cosmetic. There is no freelook, auto-clicker,
reach/hitbox change or fullbright, and none should be added without a server rule that allows it.

## Features

| Feature | Settings (section) | How | Server rule |
|---|---|---|---|
| Zoom | `zoom`, `zoomLevel` (2–10×), `zoomSmooth`, `zoomScroll`, `zoomCinematic`, key `zoomKey` (C) · advanced `zoomTime`, `zoomSensitivity`, `zoomScrollStep` | `GameRendererMixin` divides the world FOV (`getFov(..., useFovSetting = true)`, not the hand) by `Zoom.factor()`; `MouseHandlerMixin` scales the turn by `1/factor`, ORs `smoothCamera` for cinematic, takes the wheel while the key is held. Options are never written, so FOV and mouse speed come back exactly. The factor eases in log space; a wheel-changed level only lasts for that hold. | `zoom` |
| Toggle sprint / sneak | `toggleSprint`, `toggleSneak`, `showToggleState` | `ToggleMovement` consumes clicks of the vanilla sprint/sneak key; `KeyboardInputMixin` reports the key as held. Vanilla's own *Toggle* mode wins when it is set. Toggled sneak pauses while flying or riding. | `togglesprint`, `togglesneak` |
| Toggle indicator | HUD widget `pvp.togglestate` ("Sprinting (toggled)"), a `TextWidget` drawn and styled by the core (docs/HUD.md) | `PvpHud` (the only HUD-API class of the module) | `hud.togglestate` |
| Crosshair | `crosshair` (Vanilla/Cross/T/Circle/Square/Dot), `chSize`, `chGap`, `chThickness`, `chColor`, `chOpacity`, `chOutline`, `chDot`, `chDynamic`, `chInvert` · advanced `chDynamicAmount` | `GuiMixin` wraps (`@WrapOperation`) only the crosshair `blitSprite` inside vanilla `renderCrosshair`, so F1, third person, spectator, the debug crosshair and the attack indicator behave like vanilla. `Crosshair` draws GUI-pixel rects (outline = the shape 1 px bigger first, invert = `RenderPipelines.GUI_INVERT`). | `crosshair` |
| Crosshair editor | header button *Edit crosshair…* | `CrosshairEditorScreen`: panel on the right, no background and the game's GUI scale, so the live HUD crosshair in the middle is the real thing; 4× preview over sky/grass/stone/snow, shape tiles, sliders, Reset/Done. On the title screen a sample scene is drawn behind. | – |
| Block outline | `outline`, `outlineColor`, `outlineWidth`, `outlineOpacity`, `outlineFill`, `outlineFillOpacity` | `LevelRendererMixin` replaces `renderBlockOutline`: lines through vanilla's `renderHitOutline` with our colour/width, fill = `RenderTypes.debugFilledBox()` quads 0.002 bigger than each box of the shape. | `blockoutline` |
| Hit colour | `hitColor`, `hitTint`, `hitStrength` | `HitColor` repaints the upper 8 rows of vanilla's 16×16 overlay texture (vanilla `0xB3FF0000`; strength = 1 − alpha) and paints vanilla back when off. | `hitcolor` |
| Low fire | `lowFire`, `fireHeight` | `ScreenEffectRendererMixin` wraps `renderFire` (`@WrapMethod`) in a push/translate/pop. | `lowfire` |
| Lower shield | `lowShield`, `shieldHeight` | `ItemInHandRendererMixin` translates down right before `renderItem` for a `ShieldItem` (idle and blocking). | `lowshield` |

Every effect checks `PvpModule.<x>Active()` = module on + setting on + `!ServerRules.isDisabled(key)`; all keys
are declared with `ServerRules.supports` in `onLoad`, and a `ServerRules.listen` listener resets zoom/toggles and
repaints the hit colour immediately. Settings carry `.serverRule(key)`: the module page then shows the row dimmed with
an amber **Disabled by this server** chip (on the card title when the whole card is off; the server's message is
the tooltip). That is a core feature of `Setting`/`SettingsView`, any module can use it.

**Other mods (0.3.1):** every pvp hook is `require = 0` (`vibeyclient.pvp.mixins.json` `defaultRequire: 0`) and
stacks where it can (`@WrapOperation`/`@ModifyExpressionValue`/`@WrapMethod`, no `@Redirect`), so another mod that
changes the same vanilla code (Centered Crosshair redirects the same `blitSprite`) never crashes the game.
`MixinHealth` (called from `VibeyMixinPlugin.postApply`) finds handlers nothing calls after the transform, logs one
warning each and `PvpModule.notices()` shows *"Crosshair: not available – another mod (Centered Crosshair) already
changes the crosshair."* (known mods per hook only name the culprit). Zoom and the toggles also switch themselves off
then. Test: `PvpGameTest` with `-Dvibeyclient.test.addMods=<centered-crosshair.jar>;<crosshairindicator.jar>`.

**Key conflict:** zoom defaults to **C** like most clients. In vanilla C is also *Save Toolbar Activator*
(creative only: C + a number key saves the hotbar); both still work and the key card shows the conflict in amber.
Rebind in the Keys card if that matters.

## Testing

`PvpGameTest` (client gametest, Fabric 1.21.11) builds a platform with a target wall at y 150 and checks/shoots:
module page en/de (+ the badge), the editor (title and world), seven crosshair styles, F1 and spectator (no
crosshair), zoom 1×/4×/wheel/10× with eased factor, unchanged FOV/sensitivity and hotbar, block outline styles (cube
and fence), the hit tint on an iron golem (vanilla, blue, lime strong; texture value checked), low fire, lower shield
idle/blocking, toggle sprint/sneak with the HUD indicator, and `ServerRules.apply(["zoom","hud.*"])` (zoom key does
nothing, page badge) and clearing it.

```
JAVA_TOOL_OPTIONS=-Dvibeyclient.test.relay=off ./gradlew :fabric-1.21.11:runClientGameTest
python tools/pvp-review.py      # → build/review/pvp/ (prefix stripped, 5× crosshair crops, contact sheet)
```

Porting to the other versions later: the targets that differ most are `Gui.renderCrosshair` (GuiGraphicsExtractor on
26.x), `LevelRenderer.renderBlockOutline`/`renderHitOutline` (line width per vertex since 1.21.9), the overlay texture
and `ItemInHandRenderer` (SubmitNodeCollector since 1.21.9). Then drop the exclusions in `gradle/modules.gradle`.
