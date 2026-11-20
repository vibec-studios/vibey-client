# Screens module (screen editor)

Module `screens` (`module/screens`, Fabric 1.21.11 only for now) makes every menu adjustable: move, scale, resize and
hide its widgets, give the whole screen its own size, change the background dim and blur, hide the title. Works on
vanilla screens and, best effort, on screens of other mods. Ideas only from other clients, no code or assets.

> See also: [MODULES.md](MODULES.md) (module API) · [HUD.md](HUD.md) (the HUD editor this one borrows its look and
> gestures from) · [UI.md](UI.md)

## Using it

- **Pencil** (small button in a free corner of every editable screen, setting *Pencil button*) or the **edit key**
  (default **F9**, rebindable in the module page and the vanilla controls) toggles edit mode. The key only works while
  a screen is open.
- In edit mode every widget has an outline; the container panel of inventory/chest/furnace/… screens is one dashed
  block (*Panel*): moving it moves the whole container screen (slots, labels, its buttons, recipe book, other mods'
  additions) as one.
- **Drag** moves (snaps to other widgets, the screen centre and the edges with teal guides; Ctrl = free).
- **Wheel** over a widget scales it in steps (50 … 300 %); over the panel it changes the screen size.
- **Border drag** (left/right edge, white handles) changes the width. Lists and text widgets are never scaled, only
  moved/resized.
- **Right click**: *Hide/Show*, *Reset*, *Reset screen*; on empty space *Undo*, *Reset screen*, *Done*.
- **Keys**: Ctrl+Z / Ctrl+Y / Ctrl+Shift+Z undo/redo (wheel and nudges merge for 0.8 s), arrows nudge (Shift 10),
  Delete/H hide, R reset, Esc closes the menu or leaves edit mode (never the screen). All other keys are swallowed.
- **Toolbar** (top, moves to the bottom when the selection is near the top): *Size* (screen scale 50–200 %), *Dim*
  (0–100 %, *auto* = vanilla), *Blur* (0–10, *auto* = video setting), *Title* (on/off), *Reset*, *Done*. Click the
  `-`/`+` ends or use the wheel.
- **Module page**: *Try it* (opens Options in edit mode), *Reset all*, *Pencil button*, *Edit key*, a status card
  listing the edited screens (one screen is reset from its own edit mode: *Reset*), *Skip screens* under *More*
  (class names, `Name*` = prefix).

## How it works

| Part | Where | What |
|---|---|---|
| Pure core | `core/` | `Box`, `Pos` (anchor thirds + offset, survives window resizes), `Placement`, `ScreenLayout`, `LayoutStore`, `LayoutCodec` (JSON), `WidgetIds` + `IdMatcher` (identity), `Geometry` (place/move/scale/resize), `Snap`, `Hit`, `ScaleSteps`, `History`, `Spots` (pencil corner), `Blocklist`, `ScreenNames`. No Minecraft imports, JUnit in `src/junit/.../screens/core`. |
| Apply | `apply/` | `Sessions` (one `ScreenSession` per open screen object), `LayoutApplier` (scan → ids → match → place), `PanelApplier` (panel offset), `WidgetScan`, `WidgetKeys`, `WidgetTypes`, `WidgetState` (per widget, on the widget through the `StateHolder` duck), `WidgetPaint`, `Guard`. |
| Hooks | `hook/`, `mixin/` | `ScreenLifecycleMixin` (virtual size on `init`/`resize`, restore before, apply after; `rebuildWidgets`; dim), `GameRendererScreenMixin` (pose scale + panel offset + mouse around `renderWithTooltipAndSubtitles`), `MouseScreenMixin` (click/release/scroll/move/drag coordinates), `WidgetStateMixin` (render wrap), `OptionsBlurMixin`, `TitleTextMixin`, `ScreenHooks` (Fabric screen events → editor). |
| Editor | `edit/` | `EditMode`, `EditSession`, `PointerInput`, `KeyInput`, `EditActions`, `EditOverlay`, `Toolbar`, `ContextMenu`, `Pencil`, `Picking`, `Draw`. |

**Identity.** Each widget gets `type:key` where `key` is the translation key of its message (for
`options.generic_value`-style composites the key of the caption argument, so `FOV: 70` → `slider:options.fov`), else
its literal text (cut at 40 chars); `#n` for repeats. Every saved entry also stores `type@index` as fallback: a widget
whose text changed (server names, counters) is still found by its position among widgets of the same type, unless that
widget has its own entry. Screens are keyed by class name (`net.minecraft.class_…` in production), the label shown in
the settings is the screen title then.

**Applying.** After `Screen.init`, `resize` and `rebuildWidgets` the session scans `children()` (+ widgets only in
`renderables`, + tab buttons of a `TabNavigationBar`), reads each widget's vanilla rect (its *base*; a widget that
still sits where we put it keeps its old base), computes the visual rect (`Geometry.place`) and sets it. Before vanilla
lays out again (`init`/`resize` HEAD) every widget is put back to its base, so layouts never measure our sizes.

**Moving the panel** never touches `leftPos`/`topPos`: many vanilla container screens (chest, hopper, anvil,
enchanting, …) centre their background on `width` instead of `leftPos`, and some hit-test the same way. The panel
offset is a translation of the whole container screen instead, exactly like the screen size: pose translated while it
renders, every mouse event shifted back. Slots, labels, buttons, the recipe book and other mods' additions stay
consistent; the dim background is drawn compensated so it still covers the window. The editor's toolbar, menu, hint and
the pencil are drawn and hit-tested in unshifted coordinates.

**Scaling a widget** keeps hit-testing vanilla: the widget's real rect is the scaled one (x, y, w·s, h·s), so every
click, hover and other mod sees the right area. Only while it renders, `WidgetPaint` puts the logical size back, scales
the pose around (x, y) and passes the inverse-transformed mouse. Sliders and edit boxes map the pointer slightly
differently inside a scaled widget (good enough, see gaps).

**Screen size** is a virtual GUI scale: the screen is initialised with `width / k`, `height / k`, rendered with the
pose scaled by `k` (Fabric's before/after render overlays included) and every mouse event is divided by `k` (then the
panel offset is subtracted; drag deltas are only divided). Over the panel, the wheel changes this size.

**Safety.** Every hook runs inside `Guard`: on any exception the layout of that screen is restored, the screen class
is skipped until *Reset all*, one toast says so. Never applied to the blocklist: every `dev.henny.*` screen (Vibey's
own vui screens, HUD editor, …), chat, loading/progress/connect screens, sign/book editing, plus the user's list.
Vibey's title/pause corner proxies (`ui/vui/corner`) are never touched.

**File.** `config/vibeyclient/screens-layout.json`:

```json
{"version": 1, "screens": {"net.minecraft.client.gui.screens.options.OptionsScreen": {
  "scale": 0.75, "dim": 25, "blur": 0, "hideTitle": true, "label": "Options",
  "widgets": {"button:options.video": {"ax": 1, "ay": 1, "x": -60, "y": 46, "scale": 1.25, "fallback": "button@2"}}}}}
```

`ax`/`ay` = 0 left/top third, 1 centre, 2 right/bottom; `x`/`y` = offset of the matching edge/centre in GUI px.

## Tests

- JUnit: `src/junit/java/dev/henny/vibeyclient/module/screens/core/*` (identity, geometry/anchors, snapping, codec,
  helpers).
- Game test `ScreensGameTest` (`-Dvibeyclient.test.only=ScreensGameTest -Dvibeyclient.test.relay=off`): Options
  (pencil, drag + wheel, undo, click on the scaled area outside the old rect opens Video settings, layout kept after
  reopening, screen at 75 % via the toolbar + click, dim/blur/title, context menu, reset), title screen (move, scale,
  hide Realms through the menu, click the moved button, reset), inventory (panel moved, slot click, panel wheel =
  screen 110 %, slot click again) and chest (panel moved, slot click), settings page EN/DE, *Reset all*.
  Screenshots: `build/review/screens/`.

## Known gaps

- Fabric 1.21.11 only. Scaled edit boxes place the text cursor by unscaled widths; sliders map the pointer with the
  vanilla 4 px padding unscaled.
- Screens that move their own widgets every frame or call `repositionElements()` outside `init`/`resize` lose the
  layout until the next resize.
- Tooltips of a scaled screen clamp to the real window size.
- Moving a container panel moves everything the container screen draws (also widgets outside the panel, e.g. the
  recipe book).
