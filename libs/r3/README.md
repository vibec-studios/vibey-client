# r3 – 3D objects for Minecraft mods

r3 puts things into the world that players can **look at, use, grab, move and resize where they are**: UI panels,
video screens, 3D text, meshes and effects (particles, beams, rings, trails, glow). Every object gets the same
interaction model for free: crosshair = pointer, left click = use, hold middle mouse = move, wheel = size, border =
resize handle, right click = menu, Esc = cancel, Ctrl+Z = undo.

- **Pure core** (`libs/r3/src/core/java`, package `dev.henny.r3`): plain Java 21 + JOML + Gson. No Minecraft
  classes, unit tested in milliseconds.
- **Thin backend per Minecraft version** (`libs/r3/src/main/java`): draws through `WorldCanvas`, answers
  `WorldQuery`, feeds camera and input. See [docs/backend.md](docs/backend.md).
- Same API on every version. Mod id `r3`, Apache-2.0.

## 30-second example: a panel on a wall

```java
R3Scene scene = R3Scene.create(worldQuery, heldKeys, hintStore);

final class Counter implements PanelContent {
    int clicks;
    int width;
    public void layout(int w, int h) { width = w; }
    public void draw(PanelCanvas c) {
        c.fill(0, 0, width, 40, 0xE0101014);
        c.text("Clicks: " + clicks, 8, 8, 0xFFFFFFFF);
    }
    public String controlAt(float x, float y) { return y < 40 ? "count" : null; }
}

Counter counter = new Counter();
WorldObject panel = WorldObject.create("mymod:counter")
        .content(counter)
        .rules(InteractionRules.screen());
panel.transform().size(2f, 1f);
scene.add(panel);
scene.interaction().beginPlacement(panel);

scene.interaction().listen(e -> {
    if (e.kind() == InteractionEventKind.ACTION && "count".equals(e.controlId())) counter.clicks++;
    if (e.kind() == InteractionEventKind.PLACEMENT_CANCELLED) scene.remove(panel.id());
    if (e.changedPose()) save(panel);
});
```

`beginPlacement` makes the panel follow the crosshair and stick to the wall, floor or ceiling you look at
(snapped to 1/8 block, R turns floor/ceiling panels). Left click places it. After that the player can:

| Do | Gesture |
|---|---|
| press the button on the panel | look at it, left click |
| move it | hold middle mouse on it, look somewhere else, release |
| make it bigger/smaller | wheel while looking at it |
| drag one edge or corner | look at the border, hold middle mouse (or wheel) |
| change the distance while holding | Shift + wheel |
| cancel a move | Esc or right click while holding |
| open your menu (close, lock, settings) | right click → `CONTEXT_MENU` event |
| undo the last move/resize | Ctrl+Z |

The backend calls, every frame and on input:

```java
scene.update(camera, frustum, dt);
scene.render(canvas);
if (scene.press(code)) consumeVanillaInput();
```

This exact flow runs as a unit test: `ReadmeExampleTest`.

## Packages

| Package | What |
|---|---|
| `math` | rays, planes, oriented rectangles/boxes, picking, faces, angles, springs, colors |
| `view` | camera state, frustum interface, pointer rays (crosshair, mouse unproject) |
| `object` | `WorldObject`, `Transform`, `Visibility`, contents `VideoSurface` (+ `VideoOverlay` controls), `Mesh`, `CustomContent` |
| `panel` | `PanelContent` / `PointerContent` / `KeyContent` (UI-library neutral) |
| `text` | `Text3D`, styles, per-glyph effects, layout |
| `anchor` | block face, free, entity, camera, view-follow, soft-follow, lowers-when-looking-down anchors, `WorldQuery`, snapping |
| `input` | abstract input codes, gestures, rebindable bindings |
| `interact` | the interaction state machine (hover, grab, resize, wheel, undo, events) |
| `hint` | first-time hints, persisted; where the hint line goes (`HintPlacement`) |
| `fx` | pooled particles, beams, rings, trails, glow, budget/LOD |
| `render` | `WorldCanvas`, `PanelCanvas`, render modes, light, quad helpers |
| `draw` | turns objects into canvas calls |
| `persist` | Gson placement records per world/server |
| `sync` | shared objects over a `SyncChannel`, conflict rule |
| `scene` | `R3Scene`: registry, update, culling, input routing, render order |
| `vui` | `VuiPanelContent`: a vui document as panel content (controls = elements with an id or `@click`) |
| `mc` (backend jar) | the Minecraft side for every version, facade `R3Mc`, `/r3 demo` – see [minecraft.md](docs/minecraft.md) |

## Guides

- [objects.md](docs/objects.md) – world objects, transforms, contents, anchors
- [interaction.md](docs/interaction.md) – every gesture, zones, snapping, resize math, hints, undo, HUD reuse
- [text3d.md](docs/text3d.md) – 3D text, facing, sizes, glow/depth, glyph effects
- [effects.md](docs/effects.md) – particles and other effects, budget, no-allocation rules
- [persistence.md](docs/persistence.md) – saving placements, sharing with other players, conflict rule
- [minecraft.md](docs/minecraft.md) – using r3 from a mod: facade, vui panels, input, storage, rendering
- [backend.md](docs/backend.md) – what a Minecraft version backend implements (and its status)
- [parity.md](docs/parity.md) – how r3 covers today's video screens, world lyrics and cosmetic effects

## Building the core without Gradle

```
javac -d out/core -cp "joml.jar;gson.jar" $(find libs/r3/src/core/java -name '*.java')
javac -d out/test -cp "out/core;junit-console.jar;joml.jar;gson.jar" $(find libs/r3/src/test/java -name '*.java')
java -jar junit-console.jar execute -cp "out/core;out/test;joml.jar;gson.jar" --scan-classpath out/test
```

Code style: small files with one job each, no comments in code; everything that needs explaining is in `docs/`.
