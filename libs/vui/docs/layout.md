# Layout

```html
<column class="screen" padding="10" gap="6">
  <row gap="6">
    <text class="title">Settings</text>
    <spacer/>                                   <!-- flex-grow: 1 -->
    <button>Close</button>
  </row>
  <grid min-column="110" gap="4">               <!-- as many 110 px columns as fit -->
    <card v-for="m in modules" :key="m.id">…</card>
  </grid>
  <scroll grow="1">…long content…</scroll>      <!-- takes the rest, scrolls -->
  <box style="position: absolute; anchor: bottom-right; right: 4; bottom: 4">v1.0</box>
</column>
```

All sizes are GUI pixels. The document root is a `stack` filling the host size (`doc.resize(w, h)`), so a single
top-level element fills the screen.

## Display modes

| `display` | Children | Default for tags |
|---|---|---|
| `column` | flex, top to bottom | `box`, `column`, `card`, `label`, `scroll`, `list` |
| `row` | flex, left to right (`align-items: center` by default for `row`) | `row`, `button`, `select`, … |
| `stack` | all on top of each other; `align-items`/`align-self` place them on **both** axes, `stretch` fills | `stack`, root, `modal` |
| `grid` | equal-width columns, rows as tall as their tallest cell | `grid` |
| `none` | not laid out, painted or hit | – |

## Flex (row/column)

- **Basis** of each child: `flex-basis` → else `width`/`height` on the main axis → else its content size.
- **Grow**: positive free space is shared by `flex-grow`. **Shrink**: missing space is taken by
  `flex-shrink × basis`. Results are clamped to `min-*`/`max-*` (repeated up to 4 rounds).
- **Cross size**: explicit `height`/`width` → else `stretch` (the default `align-items`) fills the line → else
  content size measured at the final main size (so wrapped text gets the right height).
- **Gap**: `gap: 4` (both), `row-gap`, `column-gap`. In a row the main-axis gap is `column-gap`.
- **justify-content**: `start` `center` `end` `space-between` `space-around` `space-evenly` (only with free space).
- **align-items** / **align-self**: `start` `center` `end` `stretch`.
- **flex-wrap: wrap**: items move to a new line when they don't fit; lines stack with the cross gap.
- **Margins** add to the item's outer size; `padding` and `border-*-width` shrink the content box.
- `min-width`/`min-height` default to `0`, so items can shrink below their text width – use `text-overflow: ellipsis`
  or `flex-shrink: 0` (built-in controls already have it).

## Grid

`grid-columns: 3` (alias `columns`) fixes the column count; `grid-min-column: 110` (alias `min-column`) picks as
many columns of at least that width as fit. Without either the grid has one column. `gap` works as usual; cells
stretch to the row height unless `align-self` says otherwise.

## Absolute, fixed and anchored

```css
.badge { position: absolute; top: -2; right: -2; }                 /* inside the parent's padding box */
.fill  { position: absolute; left: 0; right: 0; top: 0; bottom: 0; } /* left+right with auto width = stretch */
.hint  { position: fixed; anchor: bottom; bottom: 8; }               /* relative to the whole document */
.hud   { position: absolute; anchor: top-right; left: -4; top: 4; }
```

Positioned elements leave the flow. With `anchor` (9 values: `top-left` `top` `top-right` `left` `center`
`right` `bottom-left` `bottom` `bottom-right`) the element is aligned to that point of the reference box and
`left`/`top` add an offset, `right`/`bottom` subtract one. Without `anchor`, `left`/`right`/`top`/`bottom` work like CSS.

## Overflow and scrolling

- `overflow: hidden` clips painting and hit testing to the padding box.
- `overflow: scroll` (default for `scroll` and `list`) also scrolls: children are laid out without shrinking on the
  main axis, the wheel scrolls the nearest scroll container under the pointer that can still move (18 px per
  notch), a 2 px thumb (`$line2`) is painted when content overflows. `element.scrollTo(x, y)` clamps.
- Off-screen children of clipping containers are skipped while painting.

## Text

| Property | Effect |
|---|---|
| `text-wrap: wrap` (default) | breaks at spaces, long words are cut; `\n` always breaks |
| `text-wrap: nowrap` | one line |
| `text-overflow: ellipsis` | cuts with `…` when a `nowrap` line is too wide or `max-lines` was reached |
| `max-lines: 2` | at most 2 lines |
| `line-height: 10` | line pitch (default: the font's line height, 9 in Minecraft) |
| `font-scale: 2` | text size multiplier (scales glyphs, keeps them crisp at integers) |
| `letter-spacing: 1` | extra px between glyphs |
| `text-align` | `start` `center` `end` inside the text box |

## When layout runs

Layout runs only when something changed: a layout-affecting style (size, padding, display, font, …), children added
or removed, text content, `v-show`, the document size. Then the whole tree is laid out once, with measured sizes
cached per pass. Frames without changes do no layout and no allocation (layout and paint of a 200-node document are
allocation-free after warm-up).

## Measuring content (HUD)

```java
Size size = doc.measureContent(maxWidth, maxHeight);  // intrinsic size of the document's content
doc.resize(size.width, size.height);
```

`HudLayer` does this for every widget (see [hud](hud.md)).

## Limits

- No `auto` margins, `order`, `align-content`, baseline alignment, grid spans/areas or `aspect-ratio`.
- Min sizes default to 0 (no CSS min-content).
- Hit testing undoes `translate`/`scale`/`rotate`; scissor/culling inside rotated elements is approximate.
