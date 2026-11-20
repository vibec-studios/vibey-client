# Interaction model

One state machine (`interact.Interaction`) drives every world object, and HUD objects can reuse it in 2D. It knows
nothing about Minecraft: the backend feeds a pointer ray, presses, releases and wheel notches; r3 answers with
events and "consumed" flags.

## States

| State | Entered by | Left by |
|---|---|---|
| `IDLE` | – | – |
| `CONTENT_DRAG` | left press on a control, or on content whose `pointerDown` returns true | left release (`pointerUp`) |
| `HOLDING` | middle press on the body, `beginMove`, `beginPlacement` | release (hold style) / click (click style) → place; Esc / right click → cancel |
| `RESIZING` | middle press on the border | middle release → commit; Esc / right click → cancel |

`capturing()` is true in every state but `IDLE`: the backend must then keep vanilla from attacking, using, picking
blocks and scrolling the hotbar.

## Hover

Each frame `HoverFinder` casts the pointer ray at every visible, hoverable object (nearest wins, nothing behind the
first block). The result is a `HoverState`: target, zone, resize handle, control id, the hit (distance, local meters,
u/v, panel pixels, front/back).

| Zone | Where |
|---|---|
| `CONTROL` | `content.controlAt(px, py)` returned an id (wins over the inner border) |
| `BODY` | inside the panel |
| `EDGE` / `CORNER` | the border ring; handle `LEFT/RIGHT/TOP/BOTTOM` or a corner |

**Border width** (meters) = `clamp(distance × borderPerBlock, borderMin, min(halfW, halfH) × 2 × borderMaxFraction)`
(defaults 0.03, 0.04, 0.35). Half of it lies outside the panel, half inside (`borderInsideFraction`), so the
handle is always reachable, also from far away. Locked objects and objects with `edgeResizable(false)` have no
border.

Pointer events reach `PointerContent` in panel pixels (0,0 = top left as seen from the front): `pointerMove`,
`hoverChanged(controlId)` (highlight), `pointerExit`, `pointerDown/Up`, `scroll`. During `CONTENT_DRAG` moves keep
coming from the object's plane even when the crosshair leaves the panel (sliders, seek bars). Objects are picked
from the front only unless `rules().doubleSided(true)`; coordinates are always as seen from the front.

## Gestures

| Gesture (default) | Looking at body / control | Looking at border | Holding |
|---|---|---|---|
| `USE` (left) | control → `ACTION` event + `pointerDown`; body → `pointerDown`; always consumed | consumed | click style: place |
| `GRAB` (middle) | start holding | start edge/corner drag | click style: place; hold style: release places |
| `MENU` (right) | `CONTEXT_MENU` event | `CONTEXT_MENU` event | cancel |
| `CANCEL` (Esc) | – | – | cancel and restore |
| `RESIZE` (wheel) | content `scroll` first, else uniform resize | grow/shrink that edge | uniform resize |
| `DISTANCE_MODIFIER` (Shift) | + wheel: fine resize | + wheel: fine edge step | + wheel: hold distance ±0.5 |
| `FINE_MODIFIER` (Alt) | + wheel: fine resize (¼ step) | fine edge step | fine resize |
| `ROTATE` (R) | – | – | quarter turn on floor/ceiling |
| `UNDO` (Ctrl+Z) | undo last move/resize | same | – |

Locked objects: `GRAB` and wheel are **not** consumed (vanilla keeps them), `USE` and `MENU` still work.
While holding, every mouse button is consumed, keys are not (walk while carrying a screen).

Rebinding: `GestureBindings.set(gesture, Binding.of(code))` or `Binding.with(modifier, code)`; `toMap()` / `load(map)`
store them as `"key:82"`, `"mouse:2"`, `"key:341+key:90"`. `RESIZE` is always the wheel. `InputCode` uses GLFW
numbers so the backend maps keys 1:1; Shift/Ctrl/Alt mean "either side".

## Holding (move)

`GrabSession` keeps the distance at which you grabbed and the point you grabbed (the crosshair stays on it).

- Each frame it casts a block ray. If a block face is within `holdDistance + snapReach` (1.5) the object snaps to it
  (`FaceSnap`, grid 1/8, offset 0.02) and gets a `BlockFaceAnchor`.
- Otherwise it floats `holdDistance` in front of you and turns to face you (`turnToViewer`), `FreeAnchor`.
- New objects (`beginPlacement`) use **surface first**: any face up to `maxReach` (24) is used and the distance
  follows it; Shift+wheel switches to keep-distance mode.
- Shift+wheel changes `holdDistance` in 0.5 steps (0.75 … 16).
- Cancel restores transform and anchor exactly; a cancelled new placement fires `PLACEMENT_CANCELLED`.

## Resizing

- **Wheel (uniform):** width ± `wheelStep` (0.25 m, fine ×¼) per notch, factor applied to `scale`, aspect kept,
  clamped by `SizeLimits` (default 0.25–16 m both ways) around the center.
- **Edge/corner drag:** the pointer ray is intersected with the object's plane as it was when the drag started; the
  grabbed edge follows it, the **opposite edge/corner stays fixed**. `keepAspect` objects scale uniformly (edge:
  the other axis stays centered; corner: the larger change wins). Size limits apply; dragging past the opposite
  edge clamps at the minimum.
- **Wheel on the border:** that edge moves by `edgeStep` (0.125 m) per notch, opposite edge fixed.

## Undo

`UndoHistory` (16 entries) stores a `PoseSnapshot` (transform + anchor) before every move, edge drag and wheel
series; wheel notches on the same object within 1 s are one entry. `undo()` (or the binding) restores it and fires
`UNDONE`. Removing an object from the scene forgets its entries.

## Events

`interaction.listen(event -> …)`: `ACTION` (control id), `CONTEXT_MENU` (control id or null), `GRAB_STARTED`,
`MOVED`, `PLACED`, `PLACEMENT_CANCELLED`, `MOVE_CANCELLED`, `RESIZE_STARTED`, `RESIZED`, `RESIZE_CANCELLED`,
`UNDONE`. `event.changedPose()` is true for the four that should be saved/synced.

## Keyboard focus

A content that implements `KeyContent` can take the keyboard: `keyFocused()` (true while e.g. a text field has the
focus), `keyDown/keyUp/charTyped`, `releaseKeyFocus()`. `R3Scene.keyTarget()` finds the object, `releaseKeys()`
drops the focus. The backend routes keys there before any gesture; Esc releases. `VuiPanelContent` and
`VideoSurface` (its overlay) implement it.

## First-time hints

`Interaction.hint()` / `hintText()` return one short line for the current situation until the player has done that
gesture once (or seen the line for 6 s): `HOVER` "Middle mouse: move · Wheel: size", `HOLD`, `PLACE`, `EDGE`. Texts
follow the bindings; ids (`r3.hint.hover` …) are for translations. `SetHintStore` persists the seen ids
(`toJson()/fromJson()`, `onChange` to save). Draw the line small, under the crosshair; when the hovered or held object covers that spot, below the object
(`HintPlacement.bottomOf` projects the object's corners, `lineY` picks the row, `ScreenProjection` is the math).

## Config

`InteractionConfig` holds every number above (public fields). `InteractionConfig.hud()` is a preset for screen
pixels.

## Reuse on the HUD (2D)

Implement `Interactable` for a HUD widget with a transform in screen pixels: position = center, orientation
`Axes.fromAxes((1,0,0), (0,-1,0), (0,0,-1))` (y down), size in pixels, `InteractionRules.hud()`. Feed
`new Pointer().fromScreenPoint(mouseX, mouseY)` and use `InteractionConfig.hud()` with no `WorldQuery`. Hover,
drag, edge resize, wheel, undo and hints work the same (`HudReuseTest`).
