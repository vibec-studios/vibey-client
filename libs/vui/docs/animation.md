# Animation

```css
.tile { background: $surface; transition: background 150ms out, translate-y 200ms back; }
.tile:hover { background: $surface2; translate-y: -1; }

@keyframes pulse { from { opacity: 1 } 50% { opacity: 0.4 } to { opacity: 1 } }
.live { animation: pulse 1.2s in-out infinite; }

.row:enter, .row:leave { opacity: 0; translate-x: -8; }
.row { transition: opacity 150ms, translate-x 150ms; }

.name { text-effect: gradient(#8b6cff, #3fe0c5) wave(1, 1); }
```

## Transitions

`transition: <property|all> [duration] [easing] [delay], …`

- Defaults: `all`, `150ms`, `out`. The first time value is the duration, the second the delay.
- Animatable: every length, number and color property (`background-color`, `color`, `border-color`, `opacity`,
  `width`, `padding-left`, `translate-x`, `scale`, `rotate`, …). Shorthand names expand (`transition: padding`).
  Keyword properties switch immediately.
- A transition starts whenever the computed value changes (hover, class change, bound style) and continues from the
  value currently shown, so interrupted transitions never jump.
- Inherited properties (e.g. `color`) animate on the parent and flow to children every frame.

## Easings

| Name | Curve |
|---|---|
| `out` (`ease-out`, `vibe`) | `cubic-bezier(0.2, 0.8, 0.2, 1)` – the Vibe default |
| `in` (`ease-in`) | `cubic-bezier(0.4, 0, 1, 1)` |
| `in-out` (`ease-in-out`) | `cubic-bezier(0.4, 0, 0.2, 1)` |
| `ease` | `cubic-bezier(0.25, 0.1, 0.25, 1)` |
| `back` (`back-out`, `overshoot`) | `cubic-bezier(0.34, 1.56, 0.64, 1)` |
| `linear`, `step-end` (`steps`) | |
| `cubic-bezier(x1, y1, x2, y2)` | any curve |

## Keyframes

```css
@keyframes slide { from { translate-x: -6; opacity: 0 } to { translate-x: 0; opacity: 1 } }
.panel { animation: slide 200ms out 1 forwards; }
```

`animation: <name> [duration=1s] [easing=linear] [count|infinite] [alternate] [reverse] [forwards|both] [delay]`,
several separated by commas. Stops: `from`, `to`, percentages (`0%, 50% { … }` for several at once). Numbers and
colors are interpolated between stops; other values switch halfway. Without `forwards` the element returns to its
normal style when the animation ends.

## Enter and leave

Elements inserted by `v-if`/`v-for` after the document is shown get `:enter` for their first style pass; the
pseudo class is then removed, so a `transition` animates from the `:enter` values to the normal ones. A removed
element gets `:leave` and stays (painted, in layout) until its transitions finish, then it is disposed. The first
build of a document does not animate.

```css
toast { transition: opacity 150ms out, translate-y 150ms out; }
toast:enter, toast:leave { opacity: 0; translate-y: 8; }
```

## Animations off

```java
Animations.setEnabled(false);   // user setting "reduce motion": transitions jump, keyframes stop
Animations.setSpeed(0.5f);      // slow motion for debugging
```

## In code

```java
Tween knob = new Tween(0f);
knob.to(1f, 0.15f);           // from the current value
knob.step(dt); float x = knob.value();

Spring scale = Spring.standard(1f);   // or Spring.bouncy, or new Spring(v, stiffness, damping, mass)
scale.target(1.2f);
scale.step(dt); float s = scale.value(); boolean done = scale.atRest();

float eased = Easings.OUT.apply(t);   // Easings.parse("cubic-bezier(.2,.8,.2,1)")
```

Both respect `Animations.enabled()` and `Animations.speed()`.

## Text effects

Effects are per-glyph functions of time (color, offset, alpha, visibility). Combine them with spaces:

| Effect | Arguments (defaults) | Does |
|---|---|---|
| `gradient(c1, c2, …, [speed])` | colors; speed `0` = static | color across the text; with speed it sweeps back and forth |
| `wave(amplitude, speed, spread)` | `1`, `1`, `0.6` | vertical sine wave |
| `rainbow(speed, spread, saturation)` | `0.5`, `0.08`, `0.6` | cycling hue per glyph |
| `pulse(speed, min)` | `1`, `0.4` | alpha breathes between `min` and 1 |
| `shimmer(color, speed, width)` | `#ffffff`, `0.6`, `12` | a highlight band runs over the text |
| `typewriter(charsPerSecond, delay)` | `30`, `0` | reveals glyphs one by one |
| `shake(amplitude, speed)` | `0.5`, `20` | jitter |

```css
.rank-owner { text-effect: gradient(#ff5470, #ffb547) shimmer(#ffffff, 0.5); }
.typing { text-effect: typewriter(20); }
```

Time is the document time in seconds (`doc.frame(dt)` advances it).

### Outside documents (player names, world text)

```java
TextEffect fx = TextEffects.parse("rainbow(0.4) wave(0.8)");
GlyphRenderer glyphs = new GlyphRenderer();   // reuse one; it does not allocate per glyph
glyphs.draw(canvas, metrics, name, 0, name.length(), x, y, 0xFFFFFFFF, true, false, fx, timeSeconds, 0f);
```

Own effects implement `TextEffect.apply(GlyphState g)` and change `g.color`, `g.dx`, `g.dy`, `g.alpha`,
`g.visible` using `g.index`, `g.count`, `g.time`, `g.x`, `g.width`, `g.totalWidth`.

## Limits

- Lengths interpolate their number; mixing units (`10` → `50%`) jumps.
- No `animation-play-state`, per-property timing functions inside keyframes or animation events.
- Glyph index restarts for each styled run and each line.
