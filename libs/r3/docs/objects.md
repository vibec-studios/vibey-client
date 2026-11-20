# World objects

Everything r3 shows in the world is a `WorldObject` in an `R3Scene`.

```java
WorldObject o = WorldObject.create("mymod:board")      // random UUID id, a kind string for your feature
        .content(new MyPanel())                        // what it shows
        .rules(InteractionRules.screen())              // how players may change it
        .dimension("minecraft:overworld")              // null = every dimension
        .anchoredTo(EntityAnchor.aboveHead(uuid, 0.3)) // where it lives (default: free)
        .fullBright(false)                             // lit by the world instead of glowing
        .layer(0);                                     // lower layers draw first
o.transform().size(2f, 1.125f).scale(1f);
o.visibility().maxDistance(64).fadeDistance(8).opacity(1f);
scene.add(o);
```

## Transform

| Field | Meaning |
|---|---|
| `position()` | center, world coordinates (double) |
| `orientation()` | quaternion; local **+X = right, +Y = up, +Z = normal** (the side that faces the viewer) |
| `width()`, `height()` | base size in blocks |
| `scale()` | uniform multiplier |
| `worldWidth()`, `worldHeight()` | `size × scale`, what you see |

Why two sizes: the **wheel** changes `scale` (the panel gets bigger, its layout stays the same, text grows), the
**edge drag** changes `size` (the panel gets wider, its layout gets more room). Panel pixels are
`size × pixelsPerBlock` (default 64), so a 2 × 1.125 panel is laid out at 128 × 72 px at any scale.

`Axes.facing(nx, ny, nz, q)` builds an orientation from a normal (right stays horizontal), `FaceSnap.orientation(face,
up, q)` from block faces, `Axes.fromAxes(right, up, normal, q)` from any orthonormal basis.

## Contents

| Content | Use |
|---|---|
| `PanelContent` | 2D UI drawn into the panel (`layout(w, h)`, `draw(PanelCanvas)`, pointer events, `controlAt(x, y)` → control id). vui documents implement it through an adapter. |
| `VideoSurface` | a texture per frame (`overlay(panelContent)`: controls drawn on top while you look at it, see below) (`texture(handle)`, crop UVs, `aspect()`), dark bezel/back plate, a status line when there is no picture; `drawnFrames()` tells the player whether the screen was drawn (upload frames only then) |
| `Text3D` | text in the world, see [text3d.md](text3d.md) |
| `Mesh` | quads (`Mesh.builder(mode, texture).quad(...).box(...).build()`), local coordinates in blocks |
| `Effect` | particles, beams, rings, trails, glow, see [effects.md](effects.md) |
| `CustomContent` | draws itself: `draw(WorldCanvas, object, light)` – for rich content none of the above covers (Vibey's lyric frames) |

Any `ObjectContent` gets `tick()` (20/s, from `scene.tick()`), `update(dt)` (per frame), `facing()` (applied after
the anchor, e.g. billboards), `translucent()` (draw order) and `boundingRadius()` (culling). Contents that implement
`PointerContent` receive pointer events in panel pixels (see [interaction.md](interaction.md)).

## Anchors

| Anchor | Behaviour |
|---|---|
| `FreeAnchor.INSTANCE` | the transform is the truth |
| `BlockFaceAnchor(face, up, bx, by, bz)` | stuck to a block face; keeps the orientation from `face` + `up`; floor/ceiling objects have a horizontal `up` |
| `EntityAnchor(uuid, offset, aboveHead, facing)` | follows an entity every frame; `aboveHead` adds the entity height (minus 0.25 when sneaking); hidden while the entity is unknown |
| `CameraAnchor(distance, yawOffset, pitchOffset, smoothing)` | in front of the camera, facing it; `smoothing` (1/s, 0 = glued) makes it float after your head |
| `ViewFollowAnchor(distance)` | "where you look": rests while you look within 20° of it, glides (critically damped, 0.5 s, max 170°/s) after 0.35 s outside or at once beyond 55°; 9° above the crosshair, between 9° and 58° above the horizon |

Own anchors: implement `Anchor` (`resolve(ctx, transform)` returns false to hide the object this frame).
`Facing` (`CAMERA`, `CAMERA_YAW`, `ENTITY_YAW`, `KEEP`) turns an object towards the viewer or with the entity.

| `SoftFollowAnchor` | around the camera at a fixed elevation: `soft` rests until you turn > 30° away, then glides back (2.6/s); `follow` always eases after the view (4.5/s); `fixed` stays at one yaw |
| `LowersWhenLookingDown(inner)` | wraps any anchor around the camera: looking down (smoothed pitch) lowers the elevation by that much, never below 2° (or the set elevation if lower) |

### Video overlay (controls on a screen)

`new VideoSurface().overlay(content)` adds a `PanelContent` (usually a vui document) laid out at the object's panel
size and drawn 6 mm in front of the picture. It shows while the pointer is on the object, `linger` (1.5 s) after it
left, while `pinned(true)` (an open menu) and while it has the keyboard. While it is hidden it has no controls, so the
whole screen is body (grab, wheel). Pointer and key events are forwarded to it.

### Snapping to block faces

`FaceSnap` does what placing a video screen did: `upFor(face, viewerYaw, quarterTurns)` (walls: up; floor: the way
you face; ceiling: towards you; each quarter turn rotates clockwise), `pose(point, face, up, offset, grid, transform)`
snaps the two in-plane axes to `grid` (default 1/8 block) and lifts the object `offset` (default 0.02) off the face
so it never z-fights.

## Per-frame state

`R3Scene.update` writes `object.state()`: `visible()` (right dimension, anchored, alpha > 0, in the frustum),
`distance()`, `alpha()` (`Visibility.alphaAt`: opacity, linear fade over the last `fadeDistance` blocks, 0 past
`maxDistance` or when hidden), `light()` (packed lightmap value; world light 0.3 blocks in front of the object, or full
bright).

## Lock and remote

`locked(true)` (the player's "lock" menu entry) and `remote(true)` (someone else's shared object) make
`locked()` true: no grab, no resize, no wheel, no border zone. Controls and the context menu still work.

## Scene

`R3Scene.create(worldQuery, heldKeys, hintStore)` builds a scene with default bindings and config.
`add`, `remove(id)`, `get(id)`, `objects()`, `clear()`; `update(camera, frustum, dt)` (crosshair pointer, limited by
the first block in the way) or `update(camera, frustum, ray, maxDistance, dt)` (mouse pointer on a screen);
`tick()`; `render(canvas)` (layer, then opaque near→far, then translucent far→near; finished one-shot effects are
removed); `press/release/wheel` (input routing: a held object first, then the nearest hit); `blocksVanillaMouse()`,
`blocksHotbarScroll()`.
