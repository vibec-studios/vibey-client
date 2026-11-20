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
(or `lines`). Keep `visibleWhen` cheap. `HudWidget.invalidate()` asks for an update before the next frame (e.g. when
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

## Editor

Open it with `HudLayout.openEditor(parent)`: the HUD module's key (default **H**), the pause menu (*HUD*), the HUD
tile in the Vibey menu, or Media → *Edit HUD layout*.

- Drag to move; snaps to the screen edges (4 px margin), the centre and other elements' edges and centres (teal
  guides) with distance readouts to the anchored screen edges and the nearest neighbours (pink). Ctrl/Alt = free.
- 8 handles: proportional by default, Shift = free; the moving edge snaps too. Mouse wheel scales.
- Marquee drag or Ctrl/Shift-click for several; they move, scale, hide and style together.
- Arrow keys nudge 1 px (Shift 10). Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z undo/redo (100 steps), Ctrl+A select all,
  Ctrl+D duplicate, Delete/H hide, R reset. Esc closes the open panel or menu, otherwise it saves and leaves.
  Letters follow the keyboard layout (Ctrl+Z is the key labelled Z on a German keyboard too).
- Right-click: *Style…*, *Duplicate*, *Hide* / *Remove copy*, *Reset*; on empty space: add widgets, select all,
  global style, undo.
- Dock: *Widgets* (library panel: switch widgets on/off, add copies), undo/redo, *Templates*, *Share* (copy code,
  import code, save as file, open the layouts folder), *Cancel* (restores the layout from when it opened), *Done*.
- Style popover: *This widget* / *All widgets*; pages Box (background, colour, opacity, corners, border), Text
  (colour incl. accent and animated rainbow, accent colour, shadow, alignment), Size (scale, detail, anchor, widget
  options). A dot marks values the widget overrides; click the label to inherit again. Custom colours open an HSV
  picker with a hex field.
- First run: a 3-step intro (drag, resize, right-click). Files dropped on the window are imported.

## Templates and share codes

`HudTemplates.register(new HudTemplates.Template(id, labelKey, descKey, () -> HudLayout.state()
        .global(style).place("hud.fps", Anchor.TOP_LEFT, 4, 4, 1f).style(s -> s.paint = RAINBOW)...build()))`.
Applying a template hides every widget it does not mention (plain elements keep their place). Built-in: `pvp`,
`minimal`, `building`, `streamer` (HUD module). New module widgets must be placed in all four (e.g. `info.card`, `info.toolwarn`,
`inventory.presets`), otherwise the template applied on first start hides them.

Share code: `VHUD1.` + base64url(deflate(JSON)). JSON: `{"v":1,"g":{style},"e":{"<id>":{"a":"top_left","x":4,
"y":4,"s":1.0,"w":0,"h":0,"hide":true,"fix":true,"st":{style},"o":{options}}},"c":{"<base id>":[2,3]}}`; style
keys `bg bc bo co bd tp tc ac sh al va`. Files (`config/vibeyclient/hud-layouts/*.vhud`) hold the same JSON. Import
(`HudShare.importCode/importFile`) checks size (24 KB code, 192 KB JSON), version, shape and every value, and only
throws `HudShare.BadLayout` with an i18n key.

## Server rules

Widgets declare their key with `.rules(key)`. The built-in keys: `hud.fps`, `hud.cps`, `hud.keystrokes`,
`hud.coords`, `hud.armor`, `hud.effects`, `hud.clock`, `hud.ping`, `hud.server`, `hud.speed`, `hud.time`,
`hud.items`, `hud.memory`, `hud.compass` (`hud.*` disables all of them). A disabled widget is not drawn in game and
shows greyed with a *Disabled by server* badge (and the server's message as tooltip in the library) in the editor.
