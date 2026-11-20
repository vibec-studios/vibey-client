# HUD widgets and the HUD editor

Everything a module draws on the HUD that players may want to move, resize or style is a **HUD element**
(`dev.henny.vibeyclient.ui.hud`). The core keeps one layout for all of them (`config/vibeyclient/hud-layout.json`)
and offers the HUD editor to arrange it. The **HUD** module (`module/hud`, Fabric 1.21.11 for now) ships the built-in
widgets (FPS, CPS, keystrokes, …), the templates and the editor key / menu entries.

> See also: [MODULES.md](MODULES.md) (module API) · [UI.md](UI.md) (design rules) · [MEDIA.md](MEDIA.md) (media
> elements) · `api/ServerRules.java` (server-disabled features)

## Two kinds of elements

| | Widgets (`.widget(...)`) | Plain elements (`.render(...)`) |
|---|---|---|
| Drawn by | the core, every frame (`HudLayout.renderWidgets` from the HUD hook) | the owner, with `HudLayout.draw(g, e)` in `renderHud` |
| Style (background, colours, shadow, …) | yes, global + per widget | no |
| Free resize (Shift), options, duplicates, variants | yes | scale only |
| Library, templates, share codes | yes | yes (placement only) |
| Hidden when the server disables its rules key | yes (and greyed in the editor) | yes, if `.rules(key)` is set |

New HUD content should be a **widget**. Media (lyrics, now playing, video overlay) uses plain elements.

## Registering a widget

```java
// once, e.g. in your module's onLoad (client thread)
HudLayout.register(HudElement.builder("pvp.sprint")          // id: [a-z0-9_.], "<module>.<name>"
        .label("pvp.hud.sprint")                              // i18n key: name in the editor / library
        .description("pvp.hud.sprint.desc")                   // i18n key: one short line in the library
        .icon(Icons.WAVE)                                      // any Icons name (Icons.register(...) for your own)
        .category(HudElement.Category.MOVEMENT)                // INFO, COMBAT, MOVEMENT, WORLD, MEDIA, OTHER
        .rules("hud.togglestate")                             // ServerRules key (supports(...) is called for you)
        .defaults(Anchor.BOTTOM_LEFT, 4, -24)                  // its BOTTOM_LEFT point on the screen's, moved by (4, -24)
        .scale(1f, 0.5f, 3f)                                   // default, min, max
        .visibleWhen(() -> isEnabled() && toggleSprint.get())  // in the editor/HUD only while this holds (cheap!)
        .variants()                                            // offers Compact / Detailed (p.detailed())
        .options(HudOption.toggle("arrow", "pvp.hud.sprint.arrow", true))
        .widget(SprintIndicator::new)                          // factory: one instance per element (and copy)
        .build());
```

Nothing else is needed: no `renderHud` code, no saving. The player can move, resize, style, hide and reset it in the
editor; templates and share codes include it.

### Writing the widget

For one or more "label value" lines extend `TextWidget`:

```java
public final class SprintIndicator extends TextWidget {
    @Override protected int rate() { return 10; }              // updates per second (default 4)

    @Override protected void lines(HudPaint p) {               // runs `rate()` times a second: allocation is fine
        boolean on = p.preview || PvpModule.sprintToggled();   // p.preview = drawn by the editor: show a sample
        if (on) line(p.detailed() ? Lang.tr("pvp.hud.sprint.label") : null, Lang.tr("pvp.hud.sprinting"));
    }

    @Override protected String icon(HudPaint p) { return p.flag("arrow") ? HudIcons.SPEED : null; }
}
```

No lines = nothing drawn in game (`shown()` is false); the editor still shows the sample. Labels use the accent
colour, values the text colour; the background, corners, shadow, alignment and rainbow come from the style.

For anything else extend `HudWidget`:

```java
public final class MyBar extends HudWidget {
    private String text = "";

    @Override protected void update(HudPaint p) {              // rate() Hz + right after a style/option change
        text = compute();                                       // cache strings here
        width = 80; height = 12;                                // natural size (unscaled GUI px)
    }

    @Override protected void paint(HudPaint p) {               // every frame: NO allocation
        p.background();                                         // style background (+ border, bevel)
        p.text(text, p.alignX(p.g.width(text), 4), 2);          // style text colour / rainbow / shadow
        p.g.rect(...);                                          // anything else through Gfx
    }

    @Override protected boolean shown(HudPaint p) { return !text.isEmpty(); }
}
```

`HudPaint` (reused, never keep it): `g` (Gfx), `w`/`h` (box size, can be larger than the natural size after a free
resize), `preview`, `style` (resolved: `bg`, `text`, `accent`, `shadow`, `align`, `variant`, `corners`, `border`),
`now`, `align()` / `alignX(contentW, pad)`, `detailed()`, `option(key)` / `flag(key)`, `background()`,
`fillShape()`, `text()`, `accentText()`, `dimText()`, `plain()`.

**Hot path rules:** `paint` runs every frame. Do not build strings, lists or lambdas there; do it in `update`
(or `lines`). Keep `visibleWhen` cheap. Override `wheel(self, dir)` (return true) to use Shift+wheel in the editor for
your own value (the timer's countdown length). `HudWidget.invalidate()` asks for an update before the next frame (e.g. when
your module's state changes).

### Builder reference

| Method | Meaning |
|---|---|
| `label(key)`, `description(key)`, `icon(name)`, `category(c)` | how it appears in the editor and library |
| `rules(key)` | `ServerRules` key: hidden in game while disabled, greyed with *Disabled by server* in the editor |
| `defaults(anchor, dx, dy)`, `scale(def, min, max)` | default placement |
| `freeSize(minW, minH)` | Shift-resize may make the box smaller than the content (e.g. a compass bar's width) |
| `options(HudOption...)` | per-widget options in the style popover (`toggle`, `choice`, `.display(...)`, `.capture(...)`) |
| `duplicable()` | the player can add copies (`<id>.2`, `<id>.3`, … each with own placement/options) |
| `startHidden()` | not on screen until added from the library or a template |
| `variants()` | Compact / Detailed in the popover |
| `visibleWhen(cond)` | listed and drawn only while it holds (module on, feature on) |

`HudLayout.style(e)`, `HudLayout.serverDisabled(e)`, `HudLayout.placement(e)`, `HudLayout.hidden(e)` and
`HudLayout.hideWidgetsWhen(cond)` (the HUD module hides widgets while F3 is open) are there if you need them.

## Plain elements (owner-drawn)

```java
HudElement bar = HudLayout.register(HudElement.builder("mymodule.bar")
        .label("mymodule.hud.bar").size(200, 16).defaults(Anchor.TOP, 0, 4)
        .render((g, w, h, preview) -> drawBar(g, w, h, preview))
        .build());

@Override public void renderHud(Gfx g) { HudLayout.draw(g, bar); }   // or begin(g, bar) ... end(g)
```

## Placement model

Each element stores an **anchor** (one of 9 screen points, picked automatically from where it was dropped – the
nearest third of the screen – unless the player fixed it in the popover), its **offset** from that anchor, its
**scale** and, for widgets resized freely, its **box size**. So a widget in the top right corner stays in the top right
corner at 1280×720 and 1920×1080 and at every GUI scale. Styles are stored as overrides on top of the global style
(`null` = inherit). Entries of elements that are not registered in a run (a module that is off) stay in the file.

## How it runs (vui)

The HUD is a vui `HudLayer` (`libs/vui`, [hud](../libs/vui/docs/hud.md)). `ui/hud/render` keeps one vui widget per
registered element: a tiny document `<vibey-hud-paint :view="self"/>` whose behavior measures the element's box and
calls the element's `HudWidget.paint` (or the plain renderer) with a `Gfx` on the same `GuiGraphics`. So the widget
API above did not change: modules keep writing `HudWidget`/`TextWidget`; vui does placement, scale, layout and
drawing order.

Every frame `HudRenderer` syncs each element (shown? box size? where?) into the vui layer and renders it with the
vui canvas: in game from the HUD hook (`HudLayout.renderWidgets`, widgets only), in the editor everything that is
available and not hidden (plain elements as previews). A failure turns the layer off once and is logged; the game
keeps running.

| Package | Job |
|---|---|
| `ui/hud` | public API (`HudElement`, `HudWidget`, `TextWidget`, `HudPaint`, `HudOption`, `HudStyle`, `HudTemplates`, `HudShare`, `HudLayout` facade) and the model behind the facade: `HudRegistry` (elements, copies), `HudGeometry` (boxes, move, scale, anchor), `HudEdits`, `HudStates` + `HudStateBuilder` (capture/apply, templates), `HudFile` + `PlacementJson` + `StateJson` + `HudStyleJson` (file and share JSON), `HudCaches` (resolved styles, server rules), `WidgetPainter`, `PlainDraw` |
| `ui/hud/render` | vui adapter: `HudRenderer`, `ViewSet` (one document per element), `ElementView`, `PaintBehavior` |
| `ui/hud/edit` | editor logic without a screen: `EditSession` (lists, selection, history, guides), `EditActions`, `PointerTool` (press → move / resize / marquee), `MoveGesture`, `ResizeGesture`, `MarqueeGesture`, `AxisSnap`, `Readouts`, `Handles`, `ScaleSteps`, `EditorKeys`, `History` |
| `ui/hud/editor` | the screen (`HudEditorScreen`, a vui `InputScreen`; input routing in `EditorInput`), its vui controller (`EditorController` + `EditorSync`, `LibraryList`, `Badges`, `StylePanel`, `StyleValues`, `StyleLists`, `SharePanel`, `ContextMenu`, `Items`) and the canvas drawing under/over the HUD (`Decorations`, `Frames`, `GuideLines`, `Pen`) |
| `ui/vui` | `VibeyVui`: the client's vui setup (Vibey `Lang` behind `t()`, `vui/theme.json`) |

## Editor

Open it with `HudLayout.openEditor(parent)`: the HUD module's key (default **H**), the pause menu (*HUD*), the HUD
tile in the Vibey menu, or Media → *Edit HUD layout*. The chrome is `assets/vibeyclient/vui/hud/editor.vui` (see
[UI-NEW.md](UI-NEW.md)); it gets pointer input first, everything that misses it goes to the HUD (the full-screen
stack has `pointer-events: none`).

- **Drag** (left or middle mouse) moves; edges, centres, the 4 px margin and the 2 px gap next to other widgets
  snap (teal guides); pink readouts show the distance to the anchored screen edge and the nearest neighbours.
  Hidden widgets never snap. Alt or Ctrl = free; the magnet in the dock turns snapping off.
- **Wheel** over a widget scales it in steps (50 · 60 · 75 · 90 · 100 · 110 · 125 · 150 · 175 · 200 · 250 · 300 ·
  350 · 400 %, clamped to the element's range); the bar under the selection shows the %. **Shift+wheel** goes to the
  widget first (`HudWidget.wheel(self, dir)`, e.g. the countdown length), then falls back to scaling.
- **Handles** (single selection): proportional resize, Shift = free box size for widgets; the moving edge snaps; a
  chip shows `w × h  %`.
- **Multi-select**: marquee on empty space, Ctrl/Shift+click; the group moves, scales, hides and styles together.
- **Keys** (letters follow the keyboard layout): arrows nudge 1 px (Shift 10), Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z
  undo/redo (100 steps; nudges and wheel steps within 0.8 s merge), Ctrl+A, Ctrl+D duplicate, Delete/H hide (copies
  are removed), R reset. Esc closes the open panel, otherwise saves and leaves.
- **Bar under the selection**: name (or *n selected*), size %, *Style*, *Hide*/*Remove*, *Reset*.
- **Right-click**: on a widget *Style / Duplicate / Hide / Reset*, on empty space *Widgets / Select all / All
  widgets / Undo*.
- **Dock**: *Widgets* (library: switch on/off, + adds a copy, lock = disabled by the server), *Presets*
  (templates), undo/redo, magnet, share (copy code, import code, save file, folder), reset all (asks), *Cancel*
  (restores the layout from when it opened), *Done*. It moves to the top when the selection sits at the bottom and
  fades out while dragging.
- **Style panel** (bar → sliders icon): *This one* / *All widgets*; tabs Box (background, colour, opacity, corners,
  border), Text (paint colour/accent/rainbow, text colour, accent, shadow, align), Size (scale, detail variant,
  anchor grid + *Auto*, the widget's options). A purple dot marks an own value; click it to inherit again.
- **Server rules**: a widget the server disabled is greyed with a *Server off* badge (tooltip: the server's
  message) and a lock in the library.
- First run: a one-line key hint above the dock until the first drag (flag `intro`). Files dropped on the window are
  imported.

Tests and tools: `HudEditorScreen.session()` (selection, history, guides), `controller()` (panel, bar), `select(id)`,
`addSelection(id)`, `document()`; `HudEditorScreen.drawPointer = true` draws a mouse pointer (screenshots do not
contain the OS cursor).

## Templates and share codes

`HudTemplates.register(new HudTemplates.Template(id, labelKey, descKey, () -> HudLayout.state()
        .global(style).place("hud.fps", Anchor.TOP_LEFT, 4, 4, 1f).style(s -> s.paint = RAINBOW)...build()))`.
Applying a template (or importing a share code) hides the widgets it does not mention that start hidden (`.startHidden()`,
e.g. the HUD module's built-ins); everything else it does not mention keeps its place and visibility: a widget
another module shows by default (the waypoints minimap) is never hidden by the first-start template or a preset,
and every widget is always in the editor's *Widgets* library. Built-in: `pvp`,
`minimal`, `building`, `streamer` (HUD module).

`HudTemplates.setDefault(id)` names the template *Reset all* goes back to (the HUD module sets `minimal`).

Share code: `VHUD1.` + base64url(deflate(JSON)). JSON: `{"v":1,"g":{style},"e":{"<id>":{"a":"top_left","x":4,
"y":4,"s":1.0,"w":0,"h":0,"hide":true,"fix":true,"st":{style},"o":{options}}},"c":{"<base id>":[2,3]}}`; style
keys `bg bc bo co bd tp tc ac sh al va`. Files (`config/vibeyclient/hud-layouts/*.vhud`) hold the same JSON. Import
(`HudShare.importCode/importFile`) checks size (24 KB code, 192 KB JSON), version, shape and every value, and only
throws `HudShare.BadLayout` with an i18n key.

## Server rules

Widgets declare their key with `.rules(key)`. The built-in keys: `hud.fps`, `hud.cps`, `hud.keystrokes`,
`hud.coords`, `hud.armor`, `hud.effects`, `hud.clock`, `hud.ping`, `hud.server`, `hud.speed`, `hud.time`,
`hud.items`, `hud.memory`, `hud.compass`, `hud.timer` (`hud.*` disables all of them). A disabled widget is not drawn in game and
shows greyed with a *Disabled by server* badge (and the server's message as tooltip in the library) in the editor.

## Layout file

`config/vibeyclient/hud-layout.json`, unchanged by the vui move (no migration needed): one entry per element the
player changed (`a x y s w h hide fix st o`, the same keys as share codes), `_global` (style), `_copies`, `_flags`,
`_saved`. Entries of elements that are not registered in a run stay in the file. Before 0.3 the placements lived in
`hud.json`; they are still migrated once (`LegacyHudFile`).

## Built-in widgets of the HUD module

`hud.fps`, `cps`, `keystrokes`, `coords`, `armor`, `effects`, `clock`, `ping`, `server`, `speed`, `time`, `items`
(duplicable), `memory`, `compass` and:

| Id | What | Direct manipulation |
|---|---|---|
| `hud.timer` | stopwatch / countdown / splits (options *Mode*, *Length*); duplicable | key **U** start/stop, **Y** lap (splits) or reset; click it while the chat is open = start/stop; wheel over it in chat or Shift+wheel in the editor = countdown length (30 s … 60 min); bell + blink at 0 |
| `hud.scoreboard` | the sidebar objective as a widget (team colour slot first, 15 lines, vanilla order and number formats); options *Numbers* (hide the red scores), *Title row* | move, scale; while it is on the HUD the vanilla sidebar is off |
| `hud.tablist` | the player list while Tab is held: heads, names from `PlayerTabOverlay.getNameForDisplay` (so the nametags module's rank icons and name styles show), signal bars coloured by ping, optional ms; sort *Server* / *Name* / *Ping*; 20 rows per column | move, scale; replaces the vanilla list while on |
| `hud.bossbar` | plain element around the vanilla boss bars (box = 182 × 19 per bar) | move, scale |
| `hud.title` | plain element around the vanilla title/subtitle (box = the title at 4× and the subtitle at 2×) | move, scale (0.25–2×) |

All five start hidden (add them in *Widgets*); while one is hidden the vanilla element is drawn as usual. Boss bar
and title start exactly where vanilla draws them: `module/hud/vanilla` replaces the
vanilla HUD layers (Fabric `HudElementRegistry.replaceElement`) and draws them inside the element's translate/scale
(accessor mixins `BossOverlayAccessor`, `GuiTitleAccessor` in `vibeyclient.hud.mixins.json`).

## Tests

`HudGameTest` (Fabric 1.21.11, `-Dvibeyclient.test.only=HudGameTest`, files `Hud*Checks`, `HudTestKit`,
`HudVideo`): share code round trip and bad input; editor click-select, drag with the centre snap guide,
Ctrl+Z/Y/Shift+Z, arrow nudge, wheel steps with the % in the bar, handle resize, marquee + Ctrl+click + group move,
no snap to a hidden widget, context menu → style panel, rainbow through the panel, library, preset switch from the
dock (+ undo), share code import through the dialog, Ctrl+D, Esc; the new widgets in a world (timer keys and
splits, tab list while Tab is held, scoreboard without numbers, Shift+wheel countdown length, moved scoreboard / boss
bar / title); the server-disabled badge. Screenshots: `build/review/hud-vui/*.png`; video frames
`build/review/hud-vui/frames/` → `ffmpeg -framerate 20 -i f_%04d.png -pix_fmt yuv420p editor.mp4`.
