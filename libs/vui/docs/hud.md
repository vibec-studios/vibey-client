# HUD

```java
HudLayer hud = new HudLayer();
hud.add("fps", vui.open("mymod:hud/fps", fpsController), HudPlacement.at(Anchor.TOP_LEFT, 4, 4));
hud.add("coords", vui.open("mymod:hud/coords", coords), HudPlacement.at(Anchor.BOTTOM_LEFT, 4, -4));
hud.fromJson(savedLayoutJson);           // per profile; unknown ids are ignored

// every frame
hud.resize(screenWidth, screenHeight);   // only when it changed
hud.frame(dt);
hud.render(canvas);
```

Each widget is a normal [document](markup.md) sized to its content (`Document.measureContent`), so a HUD widget is
just a small `.vui` file:

```html
<template><row class="hud-box" gap="3"><icon src="mymod:icons/fps"/><text>{{ fps }} FPS</text></row></template>
<style>.hud-box { padding: 2 4; background: alpha($bg, .5); }</style>
```

## Placement

`HudPlacement(anchor, offsetX, offsetY, scale, visible)`:

- `anchor`: one of the 9 `Anchor` points of the screen. The widget's matching point sits on the screen's point,
  then the offset moves it (`TOP_LEFT, 4, 4` = 4 px from the top-left corner; `BOTTOM_RIGHT, -4, -4` = 4 px from
  the bottom-right corner).
- `scale`: 0.5 … 4.
- The final rectangle is always kept on screen, so widgets survive window resizes and GUI-scale changes.

`HudPlacement.fromRect(rect, scale, w, h, visible)` turns a dragged rectangle back into a placement with the nearest
anchor (screen split into thirds), so a widget dropped near the right edge stays attached to it.

## Edit mode

```java
HudEditor editor = hud.editor();
editor.active(true);

// route input while editing (return value = consumed)
editor.pointerMove(x, y);
editor.pointerDown(x, y);       // selects and grabs the widget under the pointer
editor.pointerUp(x, y);
editor.scroll(x, y, dy);        // wheel = bigger/smaller (0.1 steps), keeps the widget centered and on screen
editor.nudge(1, 0);             // arrow keys; e.g. 10 px with Shift
editor.reset(widget);           // back to its default placement
editor.toggleVisible(widget);   // hidden widgets are shown at 35 % alpha while editing
editor.snapping(false);         // e.g. while Alt is held
```

While dragging, edges and centers snap (4 px) to the screen edges, the screen center and other visible widgets.
`editor.guides()` lists the active `SnapGuide`s; `hud.render` draws widget outlines (selected one in `$brand`) and
the guides (teal) on top when the editor is active. `editor.selected()` / `hovered()` / `dragging()` let your
screen show tooltips or a side panel.

`SnapEngine.snap(rect, others, w, h, threshold)` is usable on its own (e.g. for world panels).

## Saving layouts

```java
String json = hud.toJson();   // store per profile
hud.fromJson(json);
```

```json
{
  "version": 1,
  "widgets": {
    "fps": { "anchor": "top-left", "x": 4.0, "y": 4.0, "scale": 1.0, "visible": true }
  }
}
```

Missing fields fall back to the widget's defaults; broken JSON is ignored (`HudLayoutCodec.read` returns the number
of widgets applied).

## Limits

- Widgets do not receive pointer input while the HUD is shown in game (by design); in edit mode the editor gets it.
- No multi-select, undo or rotation in the core editor – build those in the host if needed.
