# Hot reload and dev bridge

Edit a `.vui` file, save, see it in game a moment later – with your controller state, scroll positions and focus
kept. Errors show as an overlay with `file:line`, never as a crash.

## Setup

```java
Vui vui = new Vui(backend).devMode(Boolean.getBoolean("vui.dev"));   // devMode also defaults to -Dvui.dev
vui.registry()
   .addOverride(new DirectoryRoot(Path.of("../src/main/resources")))  // dev: read sources directly
   .addRoot(new ClasspathRoot(MyMod.class.getClassLoader()));        // normal: from the jar
if (vui.devMode()) vui.reloader().watch();

// every frame, on the UI thread
vui.tick();      // in dev mode: applies file changes, runs dev bridge requests
```

## Document ids and roots

`vui.open("mymod:settings/main", controller)` looks for `assets/mymod/vui/settings/main.vui`, then
`…/main.vui.json`, in every root: **overrides first**, then normal roots. Imports (`src="mymod:widgets/card"`) use
the same lookup. A missing file yields an empty document with an error, not an exception.

| Root | Reads from | Watched |
|---|---|---|
| `ClasspathRoot(loader)` | resources in jars / the classpath | no |
| `DirectoryRoot(dir)` | `dir/assets/<ns>/vui/…` | yes |
| `LooseFileRoot(dir)` | `dir/<name>.vui` as id `preview:<name>` (used by `Preview`) | yes |
| `MemoryRoot` | `put("ns:path", source)` – tests, generated UIs | no |
| your own `ResourceRoot` | `read(path)`, `describe(path)`, optional `directory()` / `idFor(file)` | if it has a directory |

`DocumentRegistry.template(id)` compiles once and caches; `reload(id)`/`forget(id)` drop the cache.
`sourceOf(id)` tells which file was used.

## What happens on save

1. `SourceWatcher` (a `java.nio` `WatchService` on all root directories, daemon thread) records the changed file.
2. `vui.tick()` → `HotReloader.poll()` maps it to a document id, drops it from the cache and rebuilds every open
   document (`vui.open` tracks them; `vui.close(doc)` stops tracking). Changed components are picked up too.
3. Each document swaps its tree in place:
   - the **controller** is the same object, so all state stays;
   - **scroll positions** and **focus** are restored by element path (`key` or `id` first, otherwise tag + index);
   - the new styles, layout and bindings apply in the same frame.
4. If the new file has errors and the old one had none, the **old tree stays** and the errors are shown on top
   (`Document.reloadFailed`). Fix the file and save again.

`vui.reloader().reloadAll()` rebuilds everything (e.g. from a keybind or `/reload`).

Other watched resources:

- **Theme files** loaded with `vui.loadTheme("assets/mymod/vui/theme.json")` (read through the roots, JSON or
  `$token` lines, layered over the theme active at the first call; several files stack in call order). Saving one
  re-applies all theme files, recompiles the default styles and rebuilds every open document.
  `vui.themeDiagnostics()` lists problems in them.
- **Icon sets** (`assets/<ns>/vui/icons.json`, see [icons](icons.md)) reload their set; documents repaint with it.
- Anything else: `vui.reloader().onResource(path -> …)` gets every changed root-relative path; return `true` to
  rebuild all documents. `vui.reloader().changed(files)` feeds changes by hand (tests, own watchers).

## Error overlay

When a document has errors (parse, style, unknown component, runtime evaluation), `DiagnosticsOverlay` draws a red
panel at the top with up to 8 lines `file:line:col  message` and `→ hint`. Turn it off with
`vui.showErrorOverlay(false)`; read them with `doc.diagnostics()`.

## Dev bridge

A tiny HTTP server for tools and AI agents: read the live tree, click, type, reload, screenshot – without restarting
Minecraft. Dev mode only, bound to `127.0.0.1`, requests from other addresses get 403.

```java
DevBridge bridge = new DevBridge(vui, new DevTarget() {
    public List<Document> documents() { return vui.reloader().documents(); }
    public Document open(String id) { return openScreen(id); }
    public byte[] screenshot() { return captureFramebufferAsPng(); }   // backend callback
}).start(DevBridge.DEFAULT_PORT);   // 7591; 0 = any free port, see bridge.port()
```

A backend whose frame is only readable later (GPU readback) overrides `screenshotAsync()` instead and completes the
future when the PNG is ready; `/screenshot` waits up to 5 s for it. The Minecraft backend does this: the request is
served at the next client tick after a frame was drawn, encoded off the render thread. In Minecraft everything above
is already wired: see [minecraft](minecraft.md#dev-mode) and the agent loop in [dev-loop](dev-loop.md).

Requests are executed on the UI thread inside `vui.tick()` (via `UiTasks`, 5 s timeout), so the game must be
ticking.

| Endpoint | Returns |
|---|---|
| `GET /` | list of endpoints |
| `GET /docs` | open documents: id, file, error count |
| `GET /tree[?doc=ns:path][&styles]` | JSON tree of the active (last opened) or given document: tag, id, class, states, `at` (file:line:col), box, text, props, children; `styles` adds non-default computed styles |
| `GET /errors` | diagnostics of all documents and the default stylesheet |
| `GET /reload` | reloads all documents, returns their errors |
| `GET /open?doc=ns:path` | asks the host to open a document, returns its tree |
| `GET /click?id=save` or `?selector=.tile:first-child` | clicks the element's center (move, down, up) |
| `GET /key?key=tab&mods=shift` · `?code=258` · `?text=hello` | key press and/or typed text to the focused element |
| `GET /screenshot` | PNG from the host's `screenshot()` |

```
curl -s localhost:7591/errors
curl -s "localhost:7591/tree?styles" | jq '.root.children[0]'
curl -s "localhost:7591/click?id=save"
curl -s "localhost:7591/key?key=tab&mods=shift"
curl -s localhost:7591/screenshot -o shot.png
```

### Agent workflow

1. Edit the `.vui` file (positions in errors and in `/tree` point at the exact line).
2. `GET /errors` – fix until empty.
3. `GET /tree` – check boxes/text; `GET /click`/`/key` to drive it.
4. `GET /screenshot` – look at it.

### Without Minecraft

```
java -cp vui-core.jar;gson.jar dev.henny.vui.dev.Preview path/to/screen.vui 427 240 out.png state.json --serve
```

`--serve` renders with the headless backend, watches the file's folder and serves the same endpoints
(`/screenshot` renders a fresh PNG). Without `--serve` it writes one PNG, prints the tree dump and errors and exits
with code 1 if there are errors – handy in scripts and CI. Files under `…/assets/<ns>/vui/` get their real id and
imports; other files become `preview:<name>` and can import siblings as `preview:<other>` and – with `--root` – any
document under that root. Options for translations, themes, icons, textures and a pointer script are in the
[README](../README.md#preview-without-minecraft).

## Limits

- Java code and themes set in code (`vui.theme(…)`) are not reloaded; theme files loaded with `vui.loadTheme` are.
- Component-local state does not exist (all state is in controllers), so nothing else needs to survive a swap.
- `/click` and `/key` go to one document; the host decides which document is "active".
