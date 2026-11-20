# vui in a Minecraft mod

The `vui` mod (`dev.henny.vui.mc`) is the Minecraft backend: it draws documents with the game's GUI renderer, feeds
them input, and hosts them as screens, HUD widgets, toasts and overlays. Fabric and NeoForge, 1.21.1 – 26.3, same API
everywhere. How to add the dependency: [libs/README.md](../../README.md#use-a-lib-from-another-mod).

## The facade

Everything starts at `VuiMc` (static, client thread):

| Call | What |
|---|---|
| `VuiMc.open("mymod:settings", controller)` | opens `assets/mymod/vui/settings.vui` as a screen |
| `VuiMc.screen(id, controller)` | the same screen without opening it, for options (below) |
| `VuiMc.hud()` | the HUD layer (`HudHost`) |
| `VuiMc.toasts()` | toast stack (`ToastHost`) |
| `VuiMc.overlays()` | documents drawn above every screen and the HUD (`OverlayHost`) |
| `VuiMc.textures()` | register dynamic textures (skins, generated images) |
| `VuiMc.animations(false)` | global "reduce motion": transitions jump, keyframes stop |
| `VuiMc.theme("mymod:dark")` | loads `assets/mymod/vui/dark.theme.json` (or `.theme`) and rebuilds open documents |
| `VuiMc.vui()` | the core `Vui` (functions, behaviors, registry, reloader) |
| `VuiMc.dev()` | dev tools: roots, bridge port, controllers for `/vui open` |

`VuiMc` initialises itself on first use, so it does not matter whether your mod's entrypoint runs before or after
vui's (Fabric does not order entrypoints of different mods). On NeoForge, declare `ordering = "AFTER"` on your `vui`
dependency anyway. Do not call it before Minecraft starts loading resources if you need text sizes (fonts are read
lazily, before that every glyph is 6 px).

## Screens

```java
VuiMc.screen("mymod:settings", new SettingsController())
        .parent(Minecraft.getInstance().screen)   // Esc returns here (default: closes to the game)
        .guiScale(2)                              // whole-number GUI scale while it is open, 0 = the game's
        .background(false)                        // no vanilla blur/dirt/panorama behind it
        .pause(false)                             // singleplayer keeps running
        .open();
```

`VuiScreen` is a normal `Screen`: subclass it if you need vanilla widgets or extra hooks. The document is created on
the first `init()` and closed in `removed()`, the controller object survives (state lives there). Esc goes through
the document first (closes popovers, modals, focus) and closes the screen last (`doc.onEscape`). Input arrives in GUI
pixels with GLFW key codes and mouse buttons on every version (26.3's SDL codes are mapped in `input/KeyCodes`).

## HUD

```java
VuiMc.hud().add("mymod:fps", "mymod:hud/fps", fpsModel, HudPlacement.at(Anchor.TOP_LEFT, 4, 4));
VuiMc.hud().openEditor();     // drag, wheel = size, arrows nudge (Shift = 10 px), R reset, H hide, Alt = no snap, Esc
```

Widgets are drawn every frame in game (not with F1), after the vanilla HUD. The layout is saved to
`config/vui/hud.json` when the editor closes and re-applied to widgets added later (`hud().file(path)` changes the
file, `hud().save()` writes it). Update widget state from a client tick (`Model.value`, `State.set`); the HUD layer
calls `frame` and lays out only what changed. More: [hud](hud.md).

## Toasts and overlays

```java
VuiMc.toasts().push("Saved", "Settings written", "mymod:icons/ok", "ok", 4f);
Document menu = VuiMc.overlays().show("mymod:radial", controller);   // Esc or VuiMc.overlays().close(menu)
```

Both are drawn above every screen (and on the HUD when no screen is open). While a screen is open they get clicks,
release, wheel and keys first, but only where they have an element: a click on empty space passes through to the
screen. Overlays do not receive typed characters; use a `VuiScreen` for text input.

## Resources

Documents are found in this order: dev override folders (dev mode only) → the resource manager (resource packs, then
mod jars) → the classpath (before resources are loaded). A resource pack can therefore replace any `.vui` file of any
mod. After a resource reload (F3+T, pack change) all open documents are rebuilt and the texture/glyph caches cleared.

- Texture ids: `mymod:icons/gear` → `assets/mymod/textures/icons/gear.png` (size read from the PNG header).
  Dynamic textures: `VuiMc.textures().register("mymod:skin/steve", identifier, 64, 64)`.
- Translations: `t('key', args…)` in markup uses Minecraft's `I18n` (your `lang/*.json`).

## Dev mode

On automatically in a development environment (Fabric `isDevelopmentEnvironment`, NeoForge not production), or with
`-Dvui.dev=true` (`-Dvui.dev=false` turns it off). It adds:

- **Override roots** `-Dvui.dev.roots=<dir>;<dir>` – each `<dir>` is a source resource folder
  (`src/main/resources`); without the property vui looks upward from the run folder for this repository's layout.
  Files there win over jars, are watched and reload on save.
- **Error overlay** – parse/style/binding errors and Java exceptions from controllers show `file:line:col` in a red
  panel on the document; nothing crashes.
- **Inspector** – Ctrl+Shift+I on any screen (or `/vui inspect`): outline, padding, `<tag>#id.class`, `file:line`,
  box of the element under the mouse.
- **Dev bridge** – HTTP on `127.0.0.1:7591` (`-Dvui.dev.port`, `0` = any free port), see [dev-loop](dev-loop.md).
- **Client command** `/vui open <id> | demo | reload | hud | inspect | errors`. Register a controller for your own
  documents so `/vui open` and the bridge can open them: `VuiMc.dev().controller("mymod:settings", SettingsController::new)`.

## Demo

`/vui demo` (or `scripts/vui-dev.sh open vui:demo`) opens `assets/vui/vui/demo.vui`: every built-in component on one
page with `dev.henny.vui.demo.DemoController`. `/vui hud` adds two demo HUD widgets and opens the editor.

## Packages

| Package | Job |
|---|---|
| `mc` | `VuiMc` facade, `VuiRuntime` (lazy singleton), `VuiHooks` (what the loader glue calls), `VuiMod` |
| `mc.compat` | version differences outside drawing/input: screens, window, GUI scale, ids |
| `mc.draw` | `McCanvas` (pixel snapping, scissor/alpha/transform stacks, string caches), `GuiTarget` (the per-version `GuiGraphics` / `GuiGraphicsExtractor` calls), `McTextMetrics` |
| `mc.input` | `McInput` (clipboard, key state, layout-correct names, cursor), SDL↔GLFW key and button mapping, modifier state |
| `mc.texture` | `McTextures`: texture ids → identifiers + sizes |
| `mc.host` | `VuiScreen`, `InputScreen` (per-version event signatures), `DocumentView`, `HudHost` + `HudEditScreen`, `ToastHost`, `OverlayHost`, `GlobalLayers`, `Guard` |
| `mc.resource` | resource-manager root, `I18n` translator, theme files, resource reload detection |
| `mc.dev` | dev mode, override roots, frame capture for `/screenshot`, inspector, bridge target, `/vui` command, demo HUD |
| `mc.fabric` / `mc.neoforge` | entrypoints and event registration of each loader |

## Version notes

| Topic | 1.21.1 – 1.21.5 | 1.21.6 – 1.21.11 | 26.1 – 26.3 |
|---|---|---|---|
| Draw target | `GuiGraphics` + `PoseStack` | `GuiGraphics` + `Matrix3x2fStack`, deferred text (`layer()` = `nextStratum`) | `GuiGraphicsExtractor` (`text` instead of `drawString`) |
| Background | drawn by `render` | drawn by MC before `render` | `extractBackground` |
| Cursor shapes | – | 1.21.9+ (`requestCursor`) | yes |
| Keys / buttons | GLFW | GLFW | 26.3: SDL scancodes and buttons, mapped to GLFW |
| Screenshot | sync readback (≤1.21.4) | async `Screenshot.takeScreenshot` | async |
