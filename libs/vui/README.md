# vui

A small UI library for Minecraft mods. You write screens, HUD widgets and panels as Vue-like `.vui` files
(template + CSS-like styles), bind them to a plain Java controller, and get layout, styling, animation, focus,
hot reload and a headless renderer for free.

- **Pure-Java core** (`src/core/java`, package `dev.henny.vui`): no Minecraft imports, only JDK + Gson. Tested
  with JUnit in milliseconds.
- **Thin per-version backend** (`src/main/java`): implements `Canvas`, `TextMetrics`, `Input`, `Textures` for one
  Minecraft version. See [docs/backend.md](docs/backend.md).
- **Built for fast iteration**: every node knows its `file:line:col`, errors come with a hint, files reload on save,
  and a localhost dev bridge lets a tool (or an AI agent) read the tree, click and take screenshots.

## 30-second example

`assets/mymod/vui/modules.vui`:

```html
<template>
  <column class="card" gap="4">
    <text class="title">{{ title }}</text>
    <row v-for="m in modules" :key="m.id" class="tile" :class="{ on: m.enabled }" @click="open(m)">
      <icon :src="m.icon"/> <text>{{ m.name }}</text> <toggle v-model="m.enabled"/>
    </row>
  </column>
</template>
<style>
.card { padding: 8; background: $panel; border: 1 $line; }
.tile:hover { background: $surface2; transition: background 150ms out; }
</style>
```

Controller:

```java
public final class ModulesController {
    public final State<String> title = new State<>("Modules");
    public final ListState<Module> modules = new ListState<>(List.of(new Module("zoom", "Zoom")));

    public void open(Module m) { /* … */ }

    public static final class Module {
        public final String id, name, icon = "mymod:icons/gear";
        public final State<Boolean> enabled = new State<>(false);
        Module(String id, String name) { this.id = id; this.name = name; }
    }
}
```

Host (a screen, the HUD, a world panel – anything that can draw):

```java
Vui vui = new Vui(backend);                                   // backend = Canvas/TextMetrics/Input/Textures of your MC version
vui.registry().addRoot(new ClasspathRoot(getClass().getClassLoader()));
Document doc = vui.open("mymod:modules", new ModulesController());

// every frame
vui.tick();                      // hot reload + dev bridge tasks (dev mode only)
doc.resize(guiWidth, guiHeight);
doc.frame(deltaSeconds);         // bindings, styles, animations, layout (only what changed)
doc.render(canvas);

// input (return value = consumed)
doc.pointerMove(x, y);
doc.pointerDown(x, y, button, mods);
doc.pointerUp(x, y, button, mods);
doc.scroll(x, y, dx, dy, mods);
doc.keyDown(key, scancode, mods);
doc.charTyped(codepoint, mods);
```

Clicking a tile calls `open(m)`, the toggle writes `m.enabled` back, `{{ title }}` updates when `title.set(…)` is
called. No reflection per frame, no allocation in steady state.

## Preview without Minecraft

```
java -cp vui-core.jar;gson.jar dev.henny.vui.dev.Preview modules.vui 427 240 out.png state.json [--scale N] [--root DIR] [--serve [PORT]]
```

Renders the file with a built-in 5×7 pixel font to a PNG, prints the element tree with boxes and `file:line`, and
lists errors (exit code 1 if there are any). `state.json` is a JSON object used as the controller. `--serve` keeps
running, reloads on save and serves the [dev bridge](docs/hot-reload.md#dev-bridge).

## Packages

| Package | Job |
|---|---|
| `diag` | `SourcePos`, `Diagnostic(s)`, `ErrorSink` – every error has file:line:col and a hint |
| `json` | JSON parser that keeps positions (for `.vui.json`, props, state files) |
| `markup` | `.vui` / `.vui.json` → AST (`ComponentFile`, `ElementNode`, `Attribute`) |
| `template` | AST → compiled template (expressions compiled once, directives checked) |
| `expr` | the expression language: lexer, parser, nodes, scopes, builtins |
| `access` | cached `MethodHandle` getters/setters/methods on controller classes |
| `bind` | reactivity: `State`, `ListState`, `Computed`, `Effect`, `Scheduler`, `Model` |
| `style` | CSS subset: parser, selectors, cascade, theme tokens, computed style |
| `anim` | easings, `Tween`, `Spring`, transitions and `@keyframes` players |
| `text` | per-glyph text effects (gradient, wave, rainbow, …) and `GlyphRenderer` |
| `layout` | flex/stack/grid/absolute/anchored layout, text wrapping |
| `dom` | runtime tree: `Element`, `Document`, overlays, hot swap state |
| `dom.build` | instantiates templates: bindings, `v-if`/`v-for` blocks, slots, components |
| `event` | hit testing, capture/bubble dispatch, pointer/drag/click, keyboard, focus, tooltips |
| `component` | `Behavior` interface, registry, default Vibe stylesheet |
| `component.builtin` | built-in controls (button, toggle, slider, textfield, select, list, modal, …) |
| `component.text` | text editing state and key handling for `textfield` |
| `paint` | draws the tree onto a `Canvas` (boxes, text, scroll, transforms, error overlay) |
| `render` | backend interfaces: `Canvas`, `TextMetrics`, `Input`, `Textures`, `Backend` |
| `geom` | `Rect`, `Placement` (popover/tooltip placement that stays on screen) |
| `hud` | HUD layer: anchored widgets, edit mode (drag, wheel resize, snap), JSON layouts |
| `host` | ready-made hosts: `ToastStack` |
| `reload` | `DocumentRegistry`, resource roots, file watcher, `HotReloader` |
| `dev` | dev bridge (localhost HTTP), `Preview` CLI |
| `headless` | PNG canvas + pixel font backend, `TreeDump` – for tests and tools |

## Docs

[markup](docs/markup.md) · [styles](docs/styles.md) · [layout](docs/layout.md) · [binding](docs/binding.md) ·
[components](docs/components.md) · [animation](docs/animation.md) · [hud](docs/hud.md) ·
[hot reload + dev bridge](docs/hot-reload.md) · [backend](docs/backend.md)

License: Apache-2.0.
