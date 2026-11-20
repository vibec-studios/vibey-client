# Parity with today's code

What the current video screens (`module/media/video`), world lyrics (`module/media/lyrics/render/WorldLyrics`,
`WorldSink`) and cosmetic effects (`avatar/fx`) do, and how r3 covers it. "Feature" = stays in the feature that uses
r3 (media, cosmetico), "backend" = the r3 MC backend.

## Video screens

| Today | r3 |
|---|---|
| Item-free placing: sticks to the block face under the crosshair (wall, floor, ceiling) | `Interaction.beginPlacement` (surface first, any face up to 24 blocks) |
| Snapped to 1/8 block, 0.02 off the face | `FaceSnap` grid 0.125, offset 0.02 (`InteractionConfig.grid/faceOffset`) |
| Floor screens' top points the way you face, ceiling towards you; **R** turns them | `FaceSnap.upFor`, `ROTATE` gesture while holding |
| Wheel = size while placing (0.25, Shift = ¼) | wheel while holding, 0.25; fine = Alt while holding, Shift or Alt otherwise (Shift is now distance while holding) |
| Right click places, left click / key cancels | **changed:** left click places, right click / Esc cancels (one rule for every gesture) |
| *Move* picks a placed screen up, cancel puts it back | `beginMove` (click to place) or hold middle mouse; cancel restores transform + anchor |
| Width 0.5–16 m, saved sizes clamped 0.25–32 | `SizeLimits` per object (default 0.25–16), clamped on load |
| Shapes 16:9, 4:3, 1:1, 21:9, 9:16 | `transform.size(w, h)` + `InteractionRules.screen()` (keep aspect) |
| Size ± in the controls | `UniformResize.step(transform, ±0.25, limits)` or the wheel |
| — | **new:** edge/corner resize, Shift+wheel distance, free-floating screens, undo, lock |
| Look at a screen + key → its controls | `CONTEXT_MENU` event (right click) or `interaction.hover().target()` for the key |
| Hit only from the front, nearer than the block in the way, ≤ 64 | `R3Scene.update` (block-limited pointer, reach 64), single-sided by default |
| Bezel/back plate (both windings), picture on top (lit, no shading), glowing = full bright, else light 0.3 in front | `VideoDrawer` + `WorldObject.fullBright`, light probe 0.3 along the normal |
| Status line without a picture: ~7 lines per height, wrap, shrink to ≤ 3 lines | `VideoSurface.status`, same rules |
| Ghost (translucent plate) while placing | the real object is drawn while held, plus a blue outline (`HandleDrawer`) |
| Upload frames only while in the frustum; distance 160 / render 128 | `state().visible()`, `VideoSurface.drawnFrames()`, `Visibility.maxDistance` |
| Screens per world (`sp:<level>`, `mp:<ip>`), overlays not saved, come back paused | `PlacementStore` + `WorldKeys`; paused/overlay = feature data |
| Sharing: UPSERT/STATE/REMOVE/REQUEST, answer after 0.3–1 s, STATE every 10 s | `SharedObjects` (same ops and delay); periodic STATE = feature calls `publishState` |
| Max 8 screens and ~3 msg/s (burst 6) per player, owner checked, leavers dropped | `SyncPolicy` defaults, conflict rule, `forgetOwner` (backend calls it when a player leaves) |
| URL policy, never share files, "Ask first" | feature (`SyncPolicy.accepting(...)`, status line via `VideoSurface.status`) |
| Coordinates finite/bounded, normal ⟂ up, sizes clamped | `PlacementCodec.valid`, `BlockFaceAnchor` check, `AnchorCodec` fallback |
| Binary `FriendlyByteBuf` codec | **changed:** JSON string ≤ 8192 chars (`SyncCodec`); the backend channel carries it |
| Timeline, drift correction, ≤ 6 decoders, positional audio, volume falloff | feature (uses `state().distance()`, `transform().position()`) |
| HUD overlay (picture-in-picture) | HUD / vui; the same interaction works in 2D (`InteractionConfig.hud()`) |
| *Use page* (mouse to the web page) | `PointerContent` pointer events in panel pixels; keyboard forwarding is the feature's |
| Clicks swallowed at tick start, wheel via hotbar slot | backend, driven by `press/wheel` return values and `blocksVanillaMouse/HotbarScroll` |

## World lyrics

| Today | r3 |
|---|---|
| Sky lyrics: billboard, far away (render distance × 0.35, 32–96), size × distance | `Text3D` + `constantScreenSize(0.013 × size)` |
| "In front of hills" (shrink towards the eye to 3.5 blocks) | `Text3D.drawInFront(3.5)` |
| *Where you look* (20° comfort zone, 350 ms, 55°, 0.5 s critically damped glide ≤ 170°/s, 9° above crosshair, 9–58°) | `ViewFollowAnchor` (same constants, dt based) |
| *Follow* / *in front* (smoothed yaw, 4.5/s; front at pitch + 6°) | `CameraAnchor(distance, 0, 6, 4.5f)` |
| *Fixed* at a spot (billboard or fixed yaw) | `FreeAnchor` + `Facing.CAMERA` / `KEEP` |
| Glow rings, 3D depth layers, caption boxes | `TextStyle` glow/depth (same offsets and alphas), background quad |
| Opacity setting | `Visibility.opacity` |
| Sky drawn first (order −1000) | `WorldObject.layer(-1000)` |
| Additive untextured quads (glow, flashes, aurora) without alpha cut-off | `RenderMode.ADDITIVE`, `beginQuads(ADDITIVE, null)` |
| `resetView` after a pause/teleport | `ViewFollowAnchor.reset()`, `CameraAnchor.reset()` |
| Text through polygon offset vs normal | `TextStyle.seeThrough` |

## Cosmetic effects (`avatar/fx`)

| Today | r3 |
|---|---|
| Sprite particles, pool of 256 per avatar, no allocation once warm | `ParticleSystem(256, …)` per object, SoA pool, tested 0 bytes/frame |
| Spread box ± xyz, velocity jitter, lifetime × 0.85–1.15 | `EmitterShape.BOX` + `box()`, `spread()`, `lifetime(min, max)` |
| Gravity per tick, drag 0.98/tick, shrink in the last 20 % | per second (×400, ≈ 0.4/s), `FloatCurve.popAndShrink` |
| Camera-facing quads, sprite UVs, emissive | `Quads.billboard`, `ParticleStyle.withTexture`, `EMISSIVE/ADDITIVE` |
| Directions relative to the body yaw | `Emitter.localVelocity(true)` + object orientation (`Facing.ENTITY_YAW` or bone pose) |
| Partial-tick interpolation | simulated per frame with real dt (not needed) |

## Minecraft backend (done)

| Today | r3 backend |
|---|---|
| Video: `WorldRenderEvents.BEFORE_ENTITIES` + submit collector (1.21.11 only) | every target: Fabric `AFTER_ENTITIES` < 1.21.9, `submitEntities` mixin ≥ 1.21.9 (Fabric and NeoForge) |
| Lyrics: `WorldSink` per version (buffer source / submit collector) | `McWorldCanvas` + `DrawSink`, same render types (`text`, `lightning`) plus `eyes` for textured additive |
| Bezel/back plate drawn with both windings | every quad is turned to the camera; no culling issues for mirrored panels |
| Clicks swallowed at `START_CLIENT_TICK`, wheel undone via the hotbar slot | mouse/scroll/key mixins cancel at the source; tick guard drops queued clicks while hovering |
| Placement R key read with `isKeyDown` | key events routed to the scene (`ROTATE`), letters follow the keyboard layout |
| `LevelRenderer.getLightColor` 0.3 in front | `McWorldQuery.lightAt` (block + sky light at that block) |
| `sp:<level>` / `mp:<ip>` keys, own JSON per feature | `WorldKeyResolver` + `config/r3/worlds/<key>.json` (`PlacementStore`) |

## Gaps (not in r3, on purpose or for later)

- **Lyrics "follow softly" (30° recenter) and "looking down lowers the text"**: not built-in; small custom
  `Anchor`s if the modes stay.
- **Lyrics rich frames** (`LyricLayout` runs with per-word yaw and z for the 3D flight, chroma neon edges): `Text3D`
  is one string with per-glyph offset/scale/roll/color; per-glyph **yaw** and chroma edges are not there yet – use
  several `Text3D` objects or a custom content.
- **Vanilla particle types and sounds** of animation events: cosmetico calls Minecraft directly; r3 only has its own
  particles.
- **Bone locators** (particles at a bone): cosmetico sets the object transform each frame (custom `Anchor`).
- **Block removed under a screen**: anchors do not check that the block still exists (today neither).
- **Keyboard input to panels** (text fields, *Use page*): r3 routes pointer events only; keys stay with the UI lib.
- **Frustum**: the backend culls with a camera cone (`CameraFrustum`, options fov + 12°), not the game's frustum –
  slightly more objects are updated than needed, nothing visible is culled.
- **Panel layering**: panels are direct quads (no render-to-texture); draw calls are separated by tiny z steps and
  scissors clip on the CPU (text glyph by glyph), so very long documents (> 160 draw calls) share the last layer.
- **Pause screen**: the scene keeps updating hover while a screen is open but gets no presses; a grab in progress
  ends on the next release.
- **Ghost look while placing**: the outline replaces the old translucent plate.
