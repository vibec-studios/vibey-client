# Markup

## File layout

A document lives at `assets/<ns>/vui/<path>.vui` and is opened by id `ns:path`.

```html
<import name="card" src="mymod:widgets/card"/>

<props>
{ "title": "Untitled", "count": 0 }
</props>

<template>
  <card :title="title">
    <text>{{ count }} items</text>
  </card>
</template>

<style>
text { color: $muted; }
</style>
```

| Block | Meaning |
|---|---|
| `<template>` | The tree. Required. Several top-level elements are allowed. |
| `<style>` | CSS subset, see [styles](styles.md). Several blocks are concatenated. Styles are global to the document (not scoped). |
| `<props>` | JSON object of prop defaults (`{ "title": "x" }`) or array of names (`["title"]`). Used when the file is a component. |
| `<script lang="json">` | `{ "props": {…}, "components": { "card": "mymod:widgets/card" } }`. There is no scripting; logic lives in Java. |
| `<import name="card" src="ns:path"/>` | Makes another `.vui` file usable as `<card>`. `name` defaults to the last path segment. |

### `.vui.json`

The same tree as JSON. Strings are text nodes (with `{{ }}`), objects are elements:

```json
{
  "props": { "title": "Untitled" },
  "components": { "card": "mymod:widgets/card" },
  "style": [".title { color: $brand2; }"],
  "template": {
    "tag": "column", "attrs": { "gap": "4", ":class": "{ on: active }" },
    "children": [ { "tag": "text", "attrs": { "class": "title" }, "children": ["{{ title }}"] } ]
  }
}
```

`style` may be a string or an array of lines. `attrs` keys use the same syntax as markup attributes.

## Text

```html
<text>Hello {{ user.name }}, you have {{ count }} new {{ count == 1 ? 'friend' : 'friends' }}</text>
<button>Save</button>
```

Whitespace runs collapse to one space; whitespace-only nodes are dropped. Text directly inside a non-text element
(like `button` above) is wrapped in an implicit `<text>`. Inside `<text>`, `<span>`, `<b>` and `<i>` form rich text
runs, each with its own style (color, weight, effect).

## Attributes and directives

| Syntax | Meaning |
|---|---|
| `name="value"` | static prop. `"true"`/`"false"` become booleans, numeric text becomes a number. |
| `:name="expr"` / `v-bind:name` | bound prop, re-evaluated when its inputs change |
| `@event="handler"` / `v-on:event` | event handler (statements, see below) |
| `v-if` / `v-else-if` / `v-else` | conditional blocks (must be siblings in that order) |
| `v-for="…"` | list rendering, see below |
| `:key="expr"` / `key="x"` | identity for `v-for` reuse and for hot-reload state matching |
| `v-show="expr"` | keeps the element but removes it from layout and paint |
| `v-model="path"` | two-way binding, see below |
| `v-text="expr"` | same as `:text` |
| `ref="name"` | `document.ref("name")` returns the element |
| `id`, `class`, `style` | selector id, static classes, inline declarations (`style="gap: 4; color: $brand"`) |
| `#name="…"` / `v-slot:name` / `v-slot` | slot content (on `<template>` or on the component tag) |

Style attributes are shortcuts for inline style (on a custom component whose props declare the same name, e.g.
a prop `size`, the value goes to the prop instead): `gap`, `padding`, `margin`, `width`, `height`, `size`, `grow`,
`shrink`, `basis`, `flex`, `align`, `justify`, `wrap`, `anchor`, `columns`, `min-column`, `min-width`,
`max-width`, `min-height`, `max-height`, `font-scale`, `text-align`, `opacity`. They work static (`gap="4"`) and
bound (`:gap="spacing"`).

### Events and modifiers

```html
<button @click="save">Save</button>              <!-- method ref: save($event) or save() -->
<button @click="open(item); count = count + 1">  <!-- statements separated by ; -->
<row @click.stop="select(i)">                    <!-- stop propagation -->
<textfield @keydown.enter="submit()" @keydown.ctrl.s.prevent="save()"/>
<box @pointerdown.capture.once="first()"/>
```

| Modifier | Effect |
|---|---|
| `.stop` `.prevent` | stop propagation / prevent the default action (after the handler runs) |
| `.self` | only when the element itself is the target |
| `.once` | fire once |
| `.capture` | listen in the capture phase |
| `.left` `.right` `.middle` | pointer button |
| `.shift` `.ctrl` `.alt` | modifier keys must be held (pointer and key events) |
| `.exact` | key events: no other modifier keys held |
| `.enter` `.esc` `.tab` `.space` `.up` `.down` `.left` `.right` `.delete` `.backspace` `.home` `.end` `.pageup` `.pagedown` `.a`…`.z` `.0`…`.9` | key filter for key events |

`$event` is the event's detail (the new value for `change`/`update:*`, the argument of `$emit`) or the event object
itself (`PointerEvent`, `KeyEvent`). Event names: see [components](components.md#events).

### v-if / v-for

```html
<text v-if="items.length == 0">Nothing here</text>
<text v-else-if="loading">…</text>
<column v-else>
  <row v-for="item in items" :key="item.id">{{ item.name }}</row>
  <row v-for="(item, index) in items">{{ index }}: {{ item.name }}</row>
  <row v-for="(value, key, index) in settings">{{ key }} = {{ value }}</row>
  <row v-for="n in 5">{{ n }}</row>   <!-- 1..5 -->
</column>
```

`v-for` accepts lists, `ListState`, arrays, other iterables, maps and numbers. With `:key`, existing elements are
reused and their item/index variables updated; without it, items are matched by equality. Use `<template v-for>` or
`<template v-if>` to repeat or toggle several elements without a wrapper.

### v-model

```html
<textfield v-model.trim="name"/>
<slider v-model.number="volume"/>
<popover v-model:open="menuOpen">…</popover>
```

`v-model` binds the control's model prop (`value` for most controls, `open` for popover/modal) and writes back on
`update:<prop>`. Targets: a name or path that is writable (setter, non-final field, `State`, map entry, `Model` value).
`.trim` trims strings, `.number` converts to a number.

### Slots

Component `mymod:widgets/card`:

```html
<props>{ "title": "", "highlight": null }</props>
<template>
  <column class="card">
    <row class="head"><text>{{ title }}</text><slot name="actions"/></row>
    <slot>Empty</slot>                                  <!-- default slot with fallback -->
    <slot name="row" :row="highlight" :index="0"/>     <!-- named slot with slot props -->
  </column>
</template>
```

Usage:

```html
<card title="Friends">
  <template #actions><button @click="add()">+</button></template>
  <text>Default slot content</text>
  <template #row="{ row, index }"><text>{{ index }} {{ row.name }}</text></template>
</card>
```

Slot props are passed as bindings on `<slot>` (`:row="r"`) and received with `#name="{ a, b }"` (destructure) or
`#name="props"` (one variable holding a map). `v-slot="{ item }"` on the component tag names the default slot's props.
Slot content is evaluated in the scope of the page that wrote it.

### :class and :style

```html
<row :class="{ on: m.enabled, muted: !m.visible }"/>
<row :class="['tile', size]"/>
<row :class="'tile ' + kind"/>
<box :style="{ width: w, background: color, translateY: offset }"/>
<box :style="'gap: 4; color: ' + tint"/>
```

Object keys may be camelCase (converted to kebab-case). `false`/`null` values are skipped.

## Expressions

Compiled once when the file loads; evaluation uses cached accessors (no reflection per frame).

| Kind | Syntax |
|---|---|
| literals | `12`, `1.5`, `'text'`, `"text"`, `` `text` ``, `true`, `false`, `null` |
| paths | `user.name`, `items[0]`, `map['key']`, `user?.name` (null-safe) |
| operators | `! && \|\| == != === !== < <= > >= + - * / %`, unary `-` `+`, `a ? b : c` |
| literals | `{ on: x, 'my-key': y }`, `[a, b]` |
| calls | `open(m)` (controller method or builtin), `m.toggle()` (method on a value) |
| handlers only | assignments `count = count + 1`, sequences `a(); b()`, bare method name `save` |

Truthiness: `null`, `false`, `0`, `NaN`, `""` are false. `==` compares numbers by value, strings by content and enums
by name (`mode == 'dark'`). `+` concatenates when either side is a string. Numbers display without `.0`
(`3` not `3.0`). `length`/`size` work on strings, lists, maps and arrays.

Builtin functions (a controller method with the same name wins):

| Function | Result |
|---|---|
| `t(key, args…)`, `i18n(key, args…)` | translation via `vui.functions().translator(…)` (default: the key) |
| `len(x)` | length of string/list/map |
| `min(a,b)` `max(a,b)` `clamp(v,min,max)` `abs` `round` `floor` `ceil` | math |
| `str(x)` `upper(x)` `lower(x)` | strings |
| `fixed(v, digits)` | `fixed(1.234, 1)` → `"1.2"` |
| `percent(v)` | `percent(0.4)` → `"40%"` |

Component templates also have `$emit(name, value)`.

## Errors

Parsing never throws. Every problem becomes a `Diagnostic` with position and hint, shown in the in-game error
overlay and returned by `document.diagnostics()`:

```
mymod/vui/modules.vui:4:31: error: expected ')' but found the end in 'open(m' (hint: close the call with ))
mymod/vui/modules.vui:7:5: error: unknown element <buton> (hint: did you mean <button>?)
mymod/vui/modules.vui:9:12: error: v-else without a v-if before it (hint: put it right after an element with v-if or v-else-if)
mymod/vui/modules.vui:3:14: error: no method open(1 args) on the controller in 'open(m)' (hint: add a method 'open' with 1 parameter(s), or a Model action)
```

Runtime errors (missing method, null path, exceptions in your Java code) are reported once per position.

## Limits

- No JavaScript: logic is Java; expressions have no loops, lambdas or `new`.
- Styles in a document and its components are global, not scoped.
- Overloaded controller methods are picked by argument count only.
