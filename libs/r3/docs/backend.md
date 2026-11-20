# Writing a version backend

The core never touches Minecraft. A backend (one per Minecraft version family, `libs/r3/src/main/java`, Stonecutter
`//?` where versions differ) implements a handful of interfaces and calls the scene at the right moments.

## Status

Done for every target (`dev.henny.r3.mc`, see [minecraft.md](minecraft.md) for the package map), written and run on
Fabric 1.21.11, compiled for Fabric 1.21.1 / 1.21.5 / 1.21.8 / 26.1 / 26.3 and NeoForge 1.21.11 / 26.3:

| Item | Where |
|---|---|
| `WorldQuery` (dimension, block ray, entity pose, light) | `world.McWorldQuery` |
| camera → `CameraState`, `ViewFrustum` | `view.CameraReader` + core `view.CameraFrustum` (camera cone, not the game's frustum) |
| `WorldCanvas`, `PanelCanvas`, `TextMetrics` | `render.McWorldCanvas`, `McPanelCanvas`, `McFontMetrics` |
| `TextureHandle` wrappers | `texture.McTextures` (resources), `DynamicTextureHandle` (pixels) |
| render hook | Fabric `AFTER_ENTITIES` below 1.21.9, mixin at the end of `LevelRenderer.submitEntities` from 1.21.9 |
| input + vanilla suppression, `HeldKeys` | `mixin.*HandlerMixin` → `R3Hooks` → `input.InputRouter`, `HeldInput`, `VanillaGuard` |
| hint line | `hud.HintOverlay` (Fabric HUD element / NeoForge GUI layer) |
| hints, bindings, placements on disk | `store.R3Storage` + core `persist.ConfigStore` (`config/r3/`) |
| `SyncChannel` | interface for the host mod; core `sync.LoopbackChannel` for single player and tests |
| vui panels | core `vui.VuiPanelContent` + `mc.vui.WorldVui` |

Not done: MCEF textures, keyboard input to panels, the game's own frustum, video screens / lyrics moved over.

## Interfaces to implement

| Interface | Backend job |
|---|---|
| `anchor.WorldQuery` | `dimension()` (e.g. `minecraft:overworld`); `raycastBlocks(origin, dir, max, out)` – first solid block face (collider shape, no fluids), fill `BlockHit` with hit point, `Face` (the side that was hit, pointing out of the block), block position, distance; `entity(uuid, out)` – interpolated position (partial tick), height (bounding box), body yaw, sneaking; `lightAt(x, y, z)` – packed lightmap value at that block |
| `render.WorldCanvas` | drawing in the world pass, see below |
| `render.PanelCanvas` | 2D drawing into a panel (returned by `WorldCanvas.beginPanel`) – adapt vui's `Canvas` |
| `render.TextMetrics` | glyph advance and line height of the font (vanilla: `Font.width`, 9) |
| `render.TextureHandle` | wrap a texture (resource id, dynamic/video texture, MCEF view) with its pixel size |
| `view.ViewFrustum` | `isVisible(minX…maxZ)` – the game's frustum test on a world AABB |
| `input.HeldKeys` | is a key/mouse button down now (either Shift/Ctrl/Alt counts as `KEY_SHIFT`/`KEY_CONTROL`/`KEY_ALT`) |
| `sync.SyncChannel` | carry `SyncCodec` JSON strings to other players (broadcast / to one player) and hand received ones to the receiver with the sender's UUID |
| `hint.HintStore` | or use `SetHintStore` and save its JSON in the mod config |

## Per frame / tick

1. Fill a `CameraState` from the game camera: `set(x, y, z, yaw, pitch)` (Minecraft yaw/pitch in degrees; yaw 0 =
   +Z/south, pitch 90 = down).
2. `scene.update(camera, frustum, dt)` once per frame **before** drawing (real frame time in seconds). For a
   mouse pointer on a screen (an open GUI over the world): unproject with `PointerRays.fromScreen(mouseX, mouseY,
   width, height, projection, viewRotation, cameraPos, ray)` and call `scene.update(camera, frustum, ray, max, dt)`.
3. `scene.render(canvas)` in the world render hook (after the world, before/with translucent; 1.21.11: submit
   collector, `BEFORE_ENTITIES` worked for video screens).
4. `scene.tick()` 20/s; `sharedObjects.tick()` too.
5. HUD: draw `scene.interaction().hintText()` near the crosshair when not null.

## Input

Map GLFW codes 1:1 (`InputCode.key(glfwKey)`, `InputCode.mouse(button)`), then:

- mouse press/release → `scene.press/release(code)`; **true = cancel the vanilla action** (no attack/use/pick block).
- wheel → `scene.wheel(notches)` (positive = up); true = cancel the hotbar scroll.
- keys (Esc, R, Ctrl+Z) → `scene.press(code)` before vanilla handles them; true = swallow (Esc must not open the
  pause menu while holding).
- `scene.blocksVanillaMouse()` / `blocksHotbarScroll()` every tick: while true, drop queued attack/use clicks
  (the old video code did this at `START_CLIENT_TICK`) and keep the selected slot.

## WorldCanvas contract

- Coordinates are **camera relative** floats (`cameraPosition()` = the camera; the core subtracts it in double
  precision).
- `push/pop/translate/rotate/scale/multiply` act on a pose stack; every `push` is matched.
- `beginQuads(mode, texture or null)` … `vertex(x, y, z, u, v, argb, light)` × 4n … `endQuads()`; never nested.
  Vertex order per quad: top-left, bottom-left, bottom-right, top-right as seen from the front. Untextured quads
  (`texture == null`) use a white texture or a position-color type.
- `glyph(codepoint, x, y, argb, light, seeThrough)` and `text(...)`: font pixels, y down (the core already scaled by
  `(s, −s, s)`); `seeThrough` = polygon-offset/no-depth display mode.
- `beginPanel(w, h, light, alpha)` returns a `PanelCanvas` whose (0,0)–(w,h) is mapped onto the object by the matrix
  the core multiplied before (x right, **y down**, z = toward the viewer in pixel units). The mapping mirrors winding,
  so draw panel quads without back-face culling (or emit both windings). `endPanel()` flushes.

Suggested render types (1.21.11): `CUTOUT` → `RenderTypes.text(tex)` / cutout entity; `TRANSLUCENT` →
`textBackground()` or entity translucent; `ADDITIVE` → `lightning()` (position-color, additive) or an additive
textured type for sprites; `EMISSIVE` → the translucent type with `Light.FULL_BRIGHT`. Light values are packed like
the vanilla lightmap (`Light.pack(block, sky)`, `FULL_BRIGHT = 0xF000F0`).

## Video textures

Upload a new frame only when `videoSurface.drawnFrames()` advanced or `object.state().visible()` is true; set the
handle with `videoSurface.texture(handle)`; `status(line)` shows text when there is no picture.

## Checklist

`WorldQuery`, `WorldCanvas` + `PanelCanvas` + `TextMetrics`, `TextureHandle` wrappers, `ViewFrustum`, camera →
`CameraState`, input mapping + vanilla suppression, render/tick hooks, `SyncChannel` over the network, config
storage for hints, bindings (`GestureBindings.toMap/load`) and `PlacementStore` JSON.
