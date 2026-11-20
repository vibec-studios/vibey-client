# Styles

```css
.card { padding: 8; gap: 4; background: $panel; border: 1 $line; }
.tile:hover, .tile:focus-within { background: $surface2; transition: background 150ms out; }
.list > row:first-child { border-top: 1 $line2; }
#title { color: $brand2; text-transform: uppercase; }
toast:enter { opacity: 0; translate-y: 8; }
@keyframes blink { from { opacity: 1 } 50% { opacity: 0.3 } to { opacity: 1 } }
.live { animation: blink 1s infinite; }
```

Numbers are GUI pixels (`8` = `8px`). Comments: `/* … */` and `// …` (between declarations).

## Selectors

| Selector | Example |
|---|---|
| tag | `button` |
| class | `.tile`, `.tile.on` |
| id | `#title` |
| universal | `*` |
| descendant | `.card text` |
| child | `.card > row` |
| next sibling | `.label + .value` (directly after) |
| later sibling | `.title ~ .chip` (any earlier sibling matches) |
| pseudo class | `:hover` `:focus` (`:focus-visible`) `:active` (`:pressed`) `:disabled` `:checked` (`:on`) `:selected` `:open` (`:expanded`) `:enter` `:leave` `:first-child` `:last-child` `:focus-within` `:invalid` `:dragging` |
| negation | `button:not(:disabled)` (pseudo classes only) |

`:hover` and `:active` apply to the element under the pointer and all its ancestors. `:enter`/`:leave` are set on
elements inserted/removed by `v-if`/`v-for` (see [animation](animation.md#enter-and-leave)).

**Specificity**: id = 10000, each class or pseudo class = 100, tag = 1. Equal specificity: later rule wins.

**Cascade order** (later wins at equal specificity, inline always wins):
1. default component styles (Vibe look, `DefaultStyles`)
2. styles of imported components, ordered so that a component's sheet comes **after** the sheets of the components
   it imports (`pause-row` imports `icon-button` → `.pause-btn` in `pause-row` beats `.icon-btn` in `icon-button`
   at equal specificity, whatever was instantiated first)
3. the document's own `<style>`
4. inline: `style="…"`, `:style`, style attributes (`gap="4"`)

So a class you put on a component from the outside (`<icon-button class="pause-btn">`) wins over the component's own
class rule of the same specificity – no need for `.pause-row .pause-btn`.

Sibling selectors (`+`, `~`) make a restyle of one child also restyle its later siblings (and their subtrees);
documents without sibling rules pay nothing.

Restyling is incremental: a changed state (`:hover`, `:focus`, …), class or id restyles that element. Its
descendants are restyled only when an inherited value changed (`color`, `font-scale`, …), when its class or id
changed, or when some selector uses that state on an ancestor (`.tile:hover text`). Hovering a tile in a long list
therefore touches a handful of elements, not the whole document.

Inherited properties (marked below) take the parent's value unless set. `inherit` and `initial` work on every
property.

## Values

| Kind | Syntax |
|---|---|
| length | `8`, `8px`, `50%` (of the parent's content box), `auto`, `none` (max-*) |
| number | `0.5`, `50%` (= 0.5), `45deg` (rotate), `infinite` |
| color | `#rgb` `#rgba` `#rrggbb` `#rrggbbaa` `0xAARRGGBB` `rgb(255, 0, 0)` `rgba(255, 255, 255, 0.09)` `alpha($brand, .5)` `mix($brand, white, .2)` `transparent` `white` `black` `red` `green` `blue` `yellow` `gray` |
| time | `150ms`, `0.2s` |
| token | `$name` – replaced by the theme value before parsing (works inside functions: `alpha($brand, .3)`) |

Keyword aliases: `flex-start`/`left`/`top` → `start`, `flex-end`/`right`/`bottom` → `end` (except for `anchor`),
`display: flex` → `row`, `display: block` → `column`, `font-weight: 700` → `bold`, `text-shadow: true` → `on`,
`text-wrap: normal` → `wrap`, `anchor: middle` → `center`.

## Properties

| Property | Values | Default | Inh. |
|---|---|---|---|
| `display` | `column` `row` `stack` `grid` `none` | `column` | |
| `width` `height` | length | `auto` | |
| `min-width` `min-height` | length | `0` | |
| `max-width` `max-height` | length | `none` | |
| `flex-grow` `flex-shrink` | number | `0`, `1` | |
| `flex-basis` | length | `auto` | |
| `row-gap` `column-gap` | length | `0` | |
| `padding-*` `margin-*` (`top` `right` `bottom` `left`) | length | `0` | |
| `align-items` | `start` `center` `end` `stretch` | `stretch` | |
| `align-self` | `auto` `start` `center` `end` `stretch` | `auto` | |
| `justify-content` | `start` `center` `end` `space-between` `space-around` `space-evenly` | `start` | |
| `flex-wrap` | `nowrap` `wrap` | `nowrap` | |
| `position` | `relative` `absolute` `fixed` | `relative` | |
| `anchor` | `none` `top-left` `top` `top-right` `left` `center` `right` `bottom-left` `bottom` `bottom-right` | `none` | |
| `top` `right` `bottom` `left` | length | `auto` | |
| `overflow` | `visible` `hidden` `scroll` | `visible` | |
| `grid-columns` | number (0 = auto) | `0` | |
| `grid-min-column` | length | `0` | |
| `grid-column-span` `grid-row-span` | number (cells a grid child covers) | `1` | |
| `aspect-ratio` | `16/9`, `1.5`, `auto` (height = width / ratio, see [layout](layout.md#aspect-ratio)) | `auto` | |
| `border-*-width` (`top` `right` `bottom` `left`) | length | `0` | |
| `border-*-color` (`top` `right` `bottom` `left`) | color | `transparent` | |
| `border-color` | color (shorthand for the four side colours, also kept as one value for code) | `transparent` | |
| `background-color` | color | `transparent` | |
| `gradient-to` | color (≠ transparent → gradient from background-color) | `transparent` | |
| `gradient-direction` | `vertical` `horizontal` | `vertical` | |
| `background-image` | `url(ns:path)` / texture id / `none` | `none` | |
| `background-slice` | number (nine-slice border in texture px) | `0` | |
| `color` | color (text, icons) | `#ffffff` | ✓ |
| `accent-color` | color (fills, caret, active parts of controls) | `#8b6cff` | ✓ |
| `track-color` `thumb-color` | color (control tracks, knobs, check marks) | `#18181f`, `#f4f4f5` | |
| `opacity` | number 0..1 | `1` | |
| `font-scale` | number (text size multiplier) | `1` | ✓ |
| `font-weight` | `normal` `bold` | `normal` | ✓ |
| `text-align` | `start` `center` `end` | `start` | ✓ |
| `text-wrap` | `wrap` `nowrap` | `wrap` | ✓ |
| `text-overflow` | `clip` `ellipsis` | `clip` | ✓ |
| `text-transform` | `none` `uppercase` `lowercase` | `none` | ✓ |
| `text-shadow` | `none` `on` | `none` | ✓ |
| `text-effect` | effect list, see [animation](animation.md#text-effects) | `none` | ✓ |
| `max-lines` | number (0 = unlimited) | `0` | ✓ |
| `line-height` | length | `auto` (font line height) | ✓ |
| `letter-spacing` | length | `0` | ✓ |
| `translate-x` `translate-y` | length (px) | `0` | |
| `scale` | number (around the box center) | `1` | |
| `rotate` | degrees | `0` | |
| `transition` | see [animation](animation.md) | `none` | |
| `animation` | see [animation](animation.md) | `none` | |
| `cursor` | `default` `pointer` `text` `move` `grab` `resize-ns` `resize-ew` `resize-nwse` `resize-nesw` `not-allowed` | `default` | ✓ |
| `visibility` | `visible` `hidden` (keeps layout, not painted, not hit) | `visible` | ✓ |
| `pointer-events` | `auto` `none` (element itself is not hit; children still can be) | `auto` | |

**Aliases**: `grow`, `shrink`, `basis`, `wrap`, `align` (align-items), `justify` (justify-content), `bg`
(background), `columns` (grid-columns), `min-column` (grid-min-column), `col-span` (grid-column), `row-span`
(grid-row), `ratio` (aspect-ratio), `font-size` (font-scale), `accent`
(accent-color), `text-color` (color), `x` (left), `y` (top), `flex-direction` (display), `white-space` (text-wrap).

**Shorthands**

| Shorthand | Example | Sets |
|---|---|---|
| `padding`, `margin`, `inset`, `border-width` | `4`, `4 8`, `4 8 2`, `4 8 2 6` | four sides (CSS order); margins may be `auto` |
| `padding-x` `padding-y` `margin-x` `margin-y` | `6` | two sides |
| `gap` | `4` or `4 8` | row-gap, column-gap |
| `size` | `16` or `16 9` | width, height |
| `translate` | `0 8` | translate-x, translate-y |
| `border` | `1 $line`, `none` | all border widths + all side colours (`solid` is ignored) |
| `border-top` … `border-left` | `2 $brand` | that side's width + that side's colour |
| `border-color` | `$line` or `$brand $line` (1–4 values, CSS order) | the side colours |
| `grid-column`, `grid-row` | `span 2` (or `2`) | `grid-column-span`, `grid-row-span` |
| `background`, `gradient` | `$panel`, `#000 #222 horizontal`, `url(mymod:ui/panel)` | background-color, gradient-to, gradient-direction, background-image |
| `flex` | `1`, `1 0 auto`, `none` | grow, shrink (default 1), basis (default 0) |

Unknown properties and bad values are reported with position and a suggestion
(`unknown property 'paddng' (hint: did you mean 'padding'?)`), the declaration is then ignored.

## Theme tokens

`$name` refers to a token of the active theme (`vui.theme()`). Default: the Vibe tokens.

| Token | Value | | Token | Value |
|---|---|---|---|---|
| `$bg` | `#000000` | | `$text` | `#f4f4f5` |
| `$surface` | `#0b0b0e` | | `$muted` | `#a1a1aa` |
| `$surface2` | `#121217` | | `$dim` | `#6b6b75` |
| `$surface3` | `#18181f` | | `$brand` | `#8b6cff` (purple) |
| `$panel` | `#08080b` | | `$brand2` | `#3fe0c5` (teal) |
| `$line` | `#ffffff17` | | `$brand-hover` | `#9e84ff` |
| `$line2` | `#ffffff29` | | `$ok` | `#3fe0c5` |
| `$scrim` | `#000000c8` | | `$danger` | `#ff5470` |
| `$pad` | `10` | | `$warn` | `#ffb547` |
| `$row` | `24` | | `$gap` | `4` |
| `$duration` | `150ms` | | `$on-brand` | `#ffffff` (text/thumbs on `$brand`) |

Change tokens in code (`vui.theme(VibeTheme.create().put("brand", "#ff8800"))`) or load a theme file with
`ThemeLoader.load(fileName, source, base, diagnostics)`. Two formats:

```json
{ "colors": { "brand": "#ff8800", "panel": "#101010" }, "sizes": { "pad": "8" } }
```

```
$brand: #ff8800;
$panel: #101010;
```

Groups `colors`, `sizes`, `tokens`, `times` are flattened. Tokens may refer to other tokens.

Switch themes at run time with `vui.useThemes(List.of("assets/mymod/vui/theme.json", "assets/mymod/vui/theme.light.json"),
Map.of("brand", "#13a387"))`: the files are applied in order over the defaults, the map last; it replaces the list of
`loadTheme` calls (the files keep hot reloading). Declarations re-resolve their tokens when the theme version changes;
`vui.reloader().reloadAll()` rebuilds open documents so bound inline styles pick it up at once.

## Default look

`DefaultStyles` gives every built-in tag the Vibe look: square corners, 1 px lines, deep blacks, purple for
active/primary, teal for small labels. Override any of it in your `<style>`:
`button.primary`, `button.ghost`, `button.danger`, `card`, `divider`, `toggle:checked`, `segment:selected`,
`tab:selected`, `select-menu`, `select-option`, `tooltip`, `popover`, `modal` (scrim), `toast`, … See
[components](components.md) for the internal tags each control creates.

## Limits

- No `!important`, attribute selectors, `:nth-child`, media queries or nesting.
- No rounded corners, shadows or blur (by design of the Vibe look). Border corners belong to the top/bottom sides.
- `%` resolves against the parent's content box; percent heights of auto-height parents behave like `auto`.
- Styles are not scoped per component.
