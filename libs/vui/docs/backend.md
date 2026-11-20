# Backend

Everything in `dev.henny.vui` is version independent. A backend for one Minecraft version implements four small
interfaces from `dev.henny.vui.render` and wires a host (screen, HUD, world panel) to a `Document`.

```java
Backend backend = new Backend(new McTextMetrics(font), new McInput(minecraft), new McTextures(textureManager));
Vui vui = new Vui(backend);
```

The headless backend (`dev.henny.vui.headless`: `ImageCanvas`, `PixelFontMetrics`, `HeadlessInput`,
`HeadlessTextures`) is the reference implementation and is what the tests and `Preview` use.

## Status

| Backend | Where | State |
|---|---|---|
| Headless (PNG, pixel font) | `src/core/java/dev/henny/vui/headless` | complete, used by tests, `Preview`, CI |
| Minecraft 1.21.1 – 26.3, Fabric + NeoForge | `src/main/java/dev/henny/vui/mc` ([minecraft](minecraft.md)) | complete; written and run on Fabric 1.21.11 (client gametest `VuiGameTest`), other targets compile |
| In-world panels | r3 (`libs/r3`) | planned: r3 renders a document into its own canvas |

The Minecraft canvas snaps rectangles and text origins to physical pixels (GUI scale × transform) so 1 px lines stay
1 px at every GUI scale, caches the `String`s it hands to the font (no allocation per `text` call in steady state)
and keeps its own copy of the transform to turn local scissor rectangles into screen space.

## Canvas

All coordinates are GUI pixels in the **current transform**. Colors are ARGB ints (`0xAARRGGBB`).

| Method | Contract |
|---|---|
| `fill(x, y, w, h, argb)` | solid rectangle |
| `gradient(x, y, w, h, from, to, horizontal)` | linear gradient, top→bottom or left→right |
| `line(x1, y1, x2, y2, width, argb)` | straight line |
| `text(chars, start, end, x, y, argb, shadow, bold)` | draw `chars[start, end)` on one line; `(x, y)` is the top-left of the line box (Minecraft `drawString` position). Must not allocate per call if possible (cache strings). |
| `glyph(codepoint, x, y, argb, shadow, bold)` | draw one character at the same baseline rules – used for per-glyph text effects |
| `texture(id, x, y, w, h, u0, v0, u1, v1, tint)` | draw a texture region (UVs 0..1) stretched to the rectangle, multiplied by `tint` |
| `nineSlice(id, x, y, w, h, slice, textureWidth, textureHeight, tint)` | corners keep `slice` texture px, edges/center stretch (`headless.NineSlice` shows the split) |
| `pushScissor(x, y, w, h)` / `popScissor()` | clip to a rectangle given in **local** coordinates; transform it to screen space and **intersect** with the outer scissor (`ScissorStack` does the intersection) |
| `push()` / `pop()` | save/restore the transform |
| `translate(x, y)`, `scale(sx, sy)`, `rotate(radians)` | modify the current transform |
| `pushAlpha(a)` / `popAlpha()` | global alpha; nested values **multiply**; applies to everything drawn until popped |
| `layer()` | start a new draw layer so later shapes cover earlier text. Needed on Minecraft ≥ 1.21.6, where text is drawn after shapes within a layer; vui calls it before every overlay and the error panel. No-op elsewhere. |

## TextMetrics

| Method | Contract |
|---|---|
| `glyphWidth(codepoint, bold)` | advance in GUI px, including the extra pixel for bold if the font adds one |
| `lineHeight()` | line pitch (Minecraft: 9) |
| `width(chars, start, end, bold)` | default sums glyph widths; override with a faster native call if you have one |

Layout and text wrapping only use these numbers, so they must match what `Canvas.text` draws.

## Input

| Method | Contract |
|---|---|
| `clipboard()` / `setClipboard(text)` | system clipboard (text field copy/paste) |
| `isKeyDown(key)` | GLFW key state |
| `keyName(key, scancode)` | **layout-correct** display name (`Z` on QWERTZ for GLFW `Y`), or `null` to use `Keys.fallbackName` |
| `setCursor(cursor)` | `Cursor` enum (`DEFAULT POINTER TEXT MOVE GRAB RESIZE_NS RESIZE_EW RESIZE_NWSE RESIZE_NESW NOT_ALLOWED`); called only on change |

## Textures

`info(id)` returns `TextureInfo(width, height)` or `null` if unknown. Ids are strings from markup
(`<icon src="mymod:icons/gear">`, `background-image: url(…)`); the backend maps them to its texture system.

## Wiring a host

```java
// on open
doc = vui.open("mymod:settings", controller);
doc.onEscape(this::close);              // Esc after overlays/focus were handled

// render (every frame, UI thread)
vui.tick();
doc.resize(width, height);              // cheap when unchanged
doc.frame(deltaSeconds);
doc.render(canvas);

// input – return true = consumed (don't pass on)
mouseMoved:    doc.pointerMove(x, y);
mouseClicked:  doc.pointerDown(x, y, button, mods);      // button: 0 left, 1 right, 2 middle (GLFW)
mouseReleased: doc.pointerUp(x, y, button, mods);
mouseScrolled: doc.scroll(x, y, dx, dy, mods);           // dy > 0 = wheel up
keyPressed:    doc.keyDown(key, scancode, mods);
keyReleased:   doc.keyUp(key, scancode, mods);
charTyped:     doc.charTyped(codepoint, mods);

// on close
vui.close(doc);
```

Key codes are GLFW values (`event.Keys`), modifier bits are GLFW's (`event.Mods`: `SHIFT=1 CTRL=2 ALT=4 SUPER=8`),
so Minecraft's values pass through unchanged. Mouse coordinates are GUI pixels relative to the document's origin
(translate them if the document is drawn at an offset or scale).

Order inside a frame: `tick` → input events (any time on the UI thread) → `frame(dt)` (bindings, styles,
animations, layout) → `render`. `render` lays out first if needed, so calling it without `frame` still shows a
correct (but unanimated) picture.

Other hosts: `HudLayer` ([hud](hud.md)) translates/scales the canvas per widget; `ToastStack`
([components](components.md#toasts)) is a full-size document; a world panel can render a document into its own
canvas implementation and feed ray-hit `(u, v)` coordinates as pointer events.

## Checklist for a new backend

- [ ] `text`/`glyph` positions match `TextMetrics` (compare with a `TreeDump` of the same document).
- [ ] Nested scissors intersect; scissors respect `translate`/`scale`.
- [ ] `pushAlpha` affects text and textures, not only fills.
- [ ] `layer()` implemented on versions with deferred text.
- [ ] No allocation per `fill`/`text` call in steady state.

## Limits

- One font: vui assumes the backend's single UI font (plus bold); no font families or sizes besides `font-scale`.
- Textures are referenced by string id only; no texture creation API in the core.
