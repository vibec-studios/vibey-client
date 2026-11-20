# Cinematic module (`cinematic`)

AFK cinematics: after a few minutes without input (or on a key) the camera leaves the player and flies slow shots
through the surroundings; the HUD is replaced by the **AFK screen**, a second HUD layout the player edits like the
HUD. **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `cinematic: ['fabric-1.21.11']`, registered by
reflection in `BuiltinModules`). Code: `src/main/java/dev/henny/vibeyclient/module/cinematic`.

> See also: [MODULES.md](MODULES.md) · [HUD.md](HUD.md) (layout profiles, editor) · [CAMERA.md](CAMERA.md) ·
> [SERVER-RULES.md](SERVER-RULES.md)

## Using it

| Setting | Default | What |
|---|---|---|
| *Start now* (page button) | – | closes the menu and starts |
| `mode` (icon picker) | Mix | Builds & world · Players · Showcase · Panorama · Waypoints · Sky & stars · Dronie · Mix |
| `auto`, `afkMinutes` | on, 3 min | start after this long without any key, click, wheel or mouse move (only with no screen open, standing still, not hurt) |
| `speed` | 1.0 (0.5–2) | shot durations are divided by it |
| `radius` | 32 (12–48) | the camera never leaves this sphere around the eyes; also capped to (render distance − 1) × 16 |
| `mixSeconds` | 40 s | Mix: switch the mode after this long (at the next cut) |
| *Edit AFK screen* | – | the HUD editor on the AFK layout, with a live camera preview behind it |
| `letterbox`, `vignette`, `grain` (section *Look*) | on, on, off | bars (11.5 % top and bottom), soft edge darkening, animated film grain |
| `title` | empty | text of the *AFK title* widget (empty = "Be right back") |
| key `startKey` | **F9** | start / stop (F6 and V are vanilla debug keys since 1.21.9) |

Any key press, mouse click, wheel or mouse move (> 4 px) ends it; that first input is swallowed (no accidental
attack, no pause menu). The first 0.6 s after starting ignore input (the start key's own release). It also ends when
a screen opens, the player is hurt or dies, the world is left, the module is switched off or the server rule arrives.

## Modes

| Mode | Shots |
|---|---|
| Builds & world | fly-by, crane reveal and arc orbit around the best scored spots within ~32 blocks |
| Players | orbit, fly-past and push-in around players that are already rendered (not invisible, not spectators), visible from your eyes; takes turns |
| Showcase | slow wide orbit, low "hero" arc and a push-in on your own player (cosmetics, emotes) |
| Panorama | high slow pan (up to 22 blocks above you), facing the sun at sunrise/sunset |
| Waypoints | fly toward or orbit waypoints within range (from the `WaypointPoints` service) |
| Sky & stars | tilt from the horizon up to the sky, or a slow turn looking up (needs open sky); Mix takes it mostly at night |
| Dronie | pulls back and up away from you, always looking at you |
| Mix | cycles through the modes that are possible here, with a fade through black between shots |

A mode that has nothing to show (no players, no waypoints, roofed) falls back to Showcase, then to a slow pan from
the eyes (always valid: it is the vanilla view).

## Fairness

Nothing is shown that the player could not see from where they stand:

- Every camera position is checked before a shot is used (samples every 0.2 s, 0.25-block steps between them):
  inside the radius, in loaded chunks, a 0.6-block box around it free of solid blocks and water, and **a clear line
  of sight from the player's eyes** (glass counts as see-through, like in vanilla).
- While a shot runs, every frame is checked again (line of sight every 4th frame); if a block appeared or a player
  walked behind a wall, the camera holds the last good pose and cuts through black.
- Players are only those the client already renders; targets must be visible from the camera too.
- The player entity never moves or turns and sends nothing extra: only the camera is moved (`Camera.setup` TAIL).
- Server rule **`cinematic.afk`** (allowed by default) stops a running cinematic at once and blocks every start.

## AFK screen (layout profile)

The AFK screen is the HUD profile `afk` (`ui/hud`: `HudLayout.registerProfile(id, shownByDefault)`, file
`config/vibeyclient/hud-layout-afk.json`). While the cinematic runs the profile is active; the vanilla HUD (hotbar,
crosshair, chat, …) is not drawn (`Gui.render` HEAD), the overlay (fade, vignette, grain, bars) and then the normal
Vibey HUD hook draw instead, so every module's widgets appear where the AFK layout places them.

- Own widgets (only listed while the AFK layout is active): `cinematic.title` (option *Shows*: title / your name /
  world or server), `cinematic.afktime`, `cinematic.mode` (mode + subject), `cinematic.friends`, `cinematic.hint`.
- Reused by id from the shared registry: `hud.clock`, `hud.server`, `media.lyrics.text` (lyrics), `media.lyrics.bar`
  (now playing). In a non-main profile the lyrics are always a HUD element, whatever their position setting.
- Presets (dock → *Presets*): **Cinema** (default; title in the bottom bar, clock top right, lyrics low),
  **Music** (big lyrics in the middle), **Minimal** (clock, AFK time, server).
- Editing: *Edit AFK screen* opens `HudLayout.openEditor(parent, "afk", onClose)`: same editor (drag, wheel scale,
  snap, style, share codes); the profile switches back when it closes.

## Code map

| Package | Job |
|---|---|
| `cinematic` | `CinematicModule` (settings), `CinematicRules`, `ModeIcons` |
| `cinematic.core` | pure (JUnit): `V3`, `Ease`, `Look`, `Pose`, `Spline` (centripetal Catmull-Rom, arc-length), `WorldView` (block tags), `Clearance` (free box, segments, voxel line of sight, open sky) |
| `cinematic.core.shot` | `Shot`, `PathShot` (spline + look spline, optional live origin), `OrbitShot`, `PanShot` |
| `cinematic.core.plan` | `Scene`, `PoiScanner` + `Poi` (spot scoring), one `Planner` per mode, `Tries` (budget + quick reject), `ShotCheck`, `Director` (shots, fades, cuts, mix), `Session` (intro, run, fly back / fade back) |
| `cinematic.run` | `Cinematic` (state), `CameraDriver`, `SceneSource` + `McWorld` + `BlockClass` + `Skies` (scene from the client), `AfkWatch`, `InputGate`, `AfkHud` + `Overlay`, `HudSwap`, `Reach` |
| `cinematic.afk` | `AfkLayout` (profile + widgets), `AfkPresets`, widgets, `AfkEditor` |
| `cinematic.mixin` | camera TAIL, FOV + hand, `Gui.render` takeover, mouse + keyboard input |

Spot scoring (`PoiScanner`): 4 × 4 column cells within the radius, the top 7 blocks of each column: built blocks
(solid, not natural) +1 each, lights +2.5, block variety +1.5 per kind above 2, water edge +4, relief +0.6 per
block; the best 12 spots at least 7 blocks apart.

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.cinematic.*"` (splines, easing, look,
  clearance, line of sight, spot scoring, every planner in a test village / closed room, players behind walls,
  turn taking, waypoints, long director runs in every mode stay fair, mix, fades, session intro/stop/fly back).
- Game: `CinematicGameTest` (+ `CinematicScene`, `CinematicChecks`, `CinematicAfkChecks`), shots in
  `build/review/cinematic/`: module page EN/DE, every mode mid-shot (camera in range and in air, player unchanged),
  Mix fairness watch, AFK screen presets with lyrics + clock, sunset, the AFK editor, return to the player, auto start,
  key start, input stop, server rule.

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=CinematicGameTest -Dvibeyclient.test.relay=off
```
