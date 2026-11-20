# Components

```html
<column gap="6">
  <tabs v-model="tab" :options="['General', 'Account', 'About']"/>
  <row gap="6"><text>Volume</text><slider v-model="volume" :step="0.05"/><text>{{ percent(volume) }}</text></row>
  <row gap="6"><checkbox v-model="showFps" label="Show FPS"/><segmented v-model="size" :options="['Small', 'Big']"/></row>
  <row gap="6">
    <textfield v-model.trim="name" placeholder="Name" max-length="16" @submit="save()"/>
    <select v-model="mode" :options="[{ value: 'a', label: 'Always' }, { value: 'n', label: 'Never' }]"/>
    <keybind v-model="menuKey"/>
  </row>
  <button class="primary" @click="save()" tooltip="Saves to disk">Save</button>
</column>
```

Every control with a value uses `v-model` (prop `value`, event `update:value`). Controls emit:

| Event | When | `$event` |
|---|---|---|
| `update:value` | every change (used by `v-model`) | new value |
| `input` | every change | new value |
| `change` | committed: click, key, pointer release, blur/Enter for text | new value |
| `submit` | `textfield`: Enter | text |
| `update:open`, `close` | `popover`/`modal` dismissed (Esc, scrim click) | `false` / – |

## Layout and text

| Tag | Notes |
|---|---|
| `box` `column` `row` `stack` `grid` | containers, see [layout](layout.md) |
| `scroll` | column with `overflow: scroll` |
| `card` | column with panel background, 1 px line, padding 8, gap 4 |
| `divider` | 1 px `$line` |
| `spacer` | `flex-grow: 1` |
| `text` | text; `:text="x"` or children. Rich text with `span`, `b` (bold), `i` |
| `icon` | `src` = texture id (tinted with `color`) or `glyph="★"` (one character); 9×9 by default |
| `image` | `src` texture id; `fit` = `fill` (default) / `contain` / `cover`; `slice="4"` nine-slice; `tinted` tints with `color`; sizes itself to the texture |

## Controls

| Tag | Props | Keys |
|---|---|---|
| `button` | `label` (or children), `disabled`; classes `primary` `ghost` `danger` | Enter/Space = click |
| `toggle` | `value` (bool) | Enter/Space flips; knob animates 150 ms |
| `checkbox` | `value` (bool), `label` (or children) | Enter/Space flips |
| `slider` | `value`, `min` (0), `max` (1), `step` (default range/100) | ←/↓ −step, →/↑ +step, PgUp/PgDn ±10 %, Home/End; wheel when focused; drag; `input` while dragging, `change` on release |
| `textfield` | `value`, `placeholder`, `max-length`, `password`, `readonly`, `invalid` | ←/→ (Ctrl: word), Shift selects, Home/End, Backspace/Delete (Ctrl: word), Ctrl+A/C/X/V (clipboard via backend), Enter = `submit` + `change`; double click selects all; caret blinks |
| `select` | `value`, `options`, `placeholder` | Enter/Space/click opens the menu, ↑/↓ change the value |
| `segmented` | `value`, `options` or `<segment value="…">` children | ←/→/↑/↓ |
| `tabs` | `value`, `options` or `<tab value="…">` children | ←/→/↑/↓ (content: use `v-if="tab == 'General'"`) |
| `keybind` | `value` (GLFW key code, −1 = none) | click/Enter → waiting (`:open`), next key is stored; Esc cancels; Backspace/Delete clears. Label from `Input.keyName` |
| `colorpicker` | `value` (`"#rrggbb"` string or ARGB number – the type is kept) | click opens a panel with saturation/value square and hue bar (drag) |
| `progress` | `value`, `max` (1); no `value` = indeterminate | – |
| `spinner` | – | – |

**Options** (`select`, `segmented`, `tabs`): a list of strings, a list of maps `{ value, label, icon }` (`id`
works as value), a map `value → label`, or a comma string `"a,b,c"`.

`disabled` on any element blocks pointer/key events for it and its children, removes it from Tab order and sets
`:disabled`. `checked`, `selected`, `invalid` props set the matching pseudo classes.

### Internal elements

Some controls create children you can style: `select` → `text` + `select-menu` (overlay) → `select-option`;
`segmented` → `segment`; `tabs` → `tab`; `colorpicker` → `colorpicker-panel`; labels are `text`. They show up in the
tree dump and dev bridge like any element.

## Lists

```html
<list :items="players" item-size="12" grow="1">
  <template #default="{ item, index }">
    <row><text>{{ index + 1 }}. {{ item.name }}</text></row>
  </template>
</list>
```

`list` only builds the visible rows (+2 overscan) and reuses them while scrolling. Each row is `item-size` px high
(default 16). Without a slot spec the variables are `item` and `index`. Use `v-for` for short lists.

## Overlays

```html
<button tooltip="Opens the wardrobe">Wardrobe</button>          <!-- text tooltip after 0.45 s -->
<button>Rich<tooltip><b>Rich</b> tooltip</tooltip></button>     <!-- any content -->

<button @click="menu = !menu">More
  <popover v-model:open="menu" placement="bottom" align="end">
    <button @click="rename()">Rename</button>
  </popover>
</button>

<modal v-model:open="confirm" @close="confirm = false">
  <card><text>Delete?</text><button @click="remove()">Delete</button></card>
</modal>
```

| Tag | Behaviour |
|---|---|
| `tooltip` attribute / `<tooltip>` | shown while hovering the owner for 0.45 s, hidden on press/scroll/keys; placed below, flips and stays on screen |
| `popover` | `open` (v-model prop), `placement` = `bottom` (default) `top` `left` `right` `center` `cover` `pointer`, `align` = `start` `center` `end`; anchored to its parent; Esc closes |
| `modal` | `open` (v-model prop), covers the document with `$scrim`, centers its content, traps Tab focus, focuses the first control, Esc or a scrim click closes (`persistent` disables the scrim click) |

Overlays are painted after the main tree, hit-tested first and stay inside the document bounds.

## Toasts

```java
ToastStack toasts = new ToastStack(vui);
toasts.push("Saved", "Settings written", "mymod:icons/ok", "ok", 4f);   // kind: info | ok | warn | error
// every frame: toasts.resize(w, h); toasts.frame(dt); toasts.render(canvas);
```

Toasts stack bottom-right, slide/fade in and out (`toast:enter`/`toast:leave`, defined in the ToastStack's own
template), click dismisses, at most 5. The `<toast>` tag can also be used in your own markup; it gets the default
look, add your own `toast:enter`/`toast:leave` rules if it should animate.

Overlays: a click outside an open `popover`, `select` menu or `colorpicker` panel closes it (clicks on its anchor
or inside it don't). Esc closes the topmost popover/menu/modal; tooltips are never "closed" by Esc.

## Focus and keys

- Tab / Shift+Tab: next/previous focusable element in tree order (inside an open modal only).
- Enter / Space: activate the focused element (click for buttons and toggles, open for select/keybind).
- Esc: dispatches an `escape` event (bubbling from the focused element; `@escape.prevent` keeps it) → closes the top
  overlay → clears keyboard focus → calls the host's `doc.onEscape(…)` handler (e.g. close the screen).
- Clicking focuses the nearest focusable ancestor; clicking empty space clears focus.
- Make anything focusable with `tabindex="0"`.

## Events

| Name (aliases) | Bubbles | Notes |
|---|---|---|
| `pointermove` (`mousemove`, `move`, `hover`) | ✓ | goes to the pressed element while dragging |
| `pointerdown` (`mousedown`, `press`, `down`) / `pointerup` (`mouseup`, `release`, `up`) | ✓ | |
| `pointerenter` (`mouseenter`, `enter`) / `pointerleave` (`mouseleave`, `leave`) | – | |
| `click`, `dblclick` (`doubleclick`, `double`), `contextmenu` (`rightclick`) | ✓ | dblclick within 0.3 s |
| `dragstart`, `drag`, `dragend` | ✓ | after 3 px of movement; `$event.dx()`/`dy()` = offset since drag start |
| `wheel` (`scroll`, `mousewheel`) | ✓ | `prevent` stops default scrolling |
| `keydown` (`key`, `keypress`), `keyup`, `char` (`type`, `text`) | ✓ | to the focused element (or the root) |
| `focus`, `blur` | – | |
| `change`, `input`, `submit`, `close`, `update:*`, custom `$emit` names | – | emitted on the control only |

`PointerEvent`: `x() y() localX() localY() button() clicks() dx() dy() shift() ctrl() alt()`.
`KeyEvent`: `key() scancode() codepoint() shift() ctrl() alt() repeat()`.

## Custom components

`mymod:widgets/stat.vui`:

```html
<props>{ "label": "", "value": 0, "unit": "" }</props>
<template>
  <column class="stat" @click="$emit('pick', label)">
    <text class="value">{{ value }}{{ unit }}</text>
    <text class="label">{{ label }}</text>
    <slot/>
  </column>
</template>
<style>.stat { padding: 4; } .label { color: $muted; }</style>
```

```html
<import src="mymod:widgets/stat"/>
<stat label="FPS" :value="fps" class="wide" @pick="select($event)"/>
```

See [binding](binding.md#components) for props, `$emit`, fallthrough attributes and `v-model`.

## Your own Behavior

```java
vui.behaviors().register("meter", new Behavior() {
    public void paint(Element e, PaintContext ctx) { /* draw with ctx.canvas() */ }
    public boolean focusable(Element e) { return true; }
    public void handle(Element e, Event ev) { if (ev.type().equals("click")) Controls.commit(e, 1.0); }
});
```

A `Behavior` can measure content (`measure`), lay out its children (`arrangeChildren`), paint before/after children,
handle events, react to prop changes (`propChanged`), animate (`frame` after `doc.wantsFrames(e, true)`), be an
overlay (`overlay`) and name its v-model prop (`modelProp`). `TemplatedBehavior` receives its children as a
template instead of built elements (how `list` works). `Controls` and `Internal` help with values and internal
child elements. Register before compiling documents that use the tag.

## Limits

- No multi-line text area, date/number spinner or drag-and-drop between lists.
- `keybind` stores keyboard keys only (no mouse buttons).
