# 3D text

```java
Text3D name = new Text3D("Alex")
        .style(TextStyle.NAMETAG.withGlow(0xFF6FD3FF, 0.6f))
        .facing(Facing.CAMERA_YAW)
        .effect(GlyphEffects.rainbow(0.2f, 0.05f, 0.8f));
scene.add(WorldObject.create("mymod:name").content(name).anchoredTo(EntityAnchor.aboveHead(uuid, 0.3)));
```

## Placement

- **Billboard:** `facing(Facing.CAMERA)` (default) – turns fully towards the camera; `CAMERA_YAW` stays upright.
- **Fixed:** `facing(Facing.KEEP)` – uses the object's orientation (set it, or let a `BlockFaceAnchor` do it).
- **Follow entity:** `EntityAnchor` (above the head, sneaking lowers it) + a facing.
- **In front of / around the view:** `CameraAnchor` (glued or smoothed) or `ViewFollowAnchor` (sky lyrics).

## Size

- `pixelSize(m)` – meters per font pixel (default 0.025, like vanilla nametags), times `transform.scale()`.
- `constantScreenSize(k)` – multiplies by `distance × k`, so far text keeps its size on screen (sky lyrics used
  `distance × 0.013 × size`).
- `drawInFront(d)` – beyond `d` blocks everything is drawn shrunk towards the eye to distance `d`: it looks the same
  on screen but sits in front of hills and trees while still depth-testing its own glyphs.

## Style

`TextStyle(color, background, padding, glowColor, glowStrength, depthColor, seeThrough, lineSpacing)` with
`withColor/withBackground/withGlow/withDepth/withSeeThrough/withLineSpacing`.

| Part | How it is drawn |
|---|---|
| background | one translucent quad behind the text (`padding` font px) |
| depth | 6 darker copies stepping back (0.13, 0.22, −0.3 font px per layer) – 3D sides |
| glow | two rings of 8 copies (1.6 px at 20 %, 0.8 px at 50 % of `glowStrength`) behind the glyph |
| glyph | the glyph itself; `seeThrough` = drawn on top of the world (polygon offset / no depth test) |

Multi-line text (`\n`) is centered per line; the block is centered on the object position. Layout happens once per
text change (`GlyphLayout`, no allocation per frame).

## Glyph effects

A `GlyphEffect` is a function `(GlyphState glyph, float time)` that changes one glyph before drawing: `offsetX/Y/Z`
(font px), `scale`, `roll` (radians), `argb`. Read-only: `index()`, `count()`, `line()`, `codepoint()`, `x()`, `y()`,
`progress()`. Effects run in order, every frame, without allocation. `Text3D.update(dt)` advances `time()`; set it with
`time(seconds)` to sync with music.

Built in (`GlyphEffects`): `wave(amplitude, cyclesPerSecond, glyphsPerWave)`, `rainbow(cyclesPerSecond,
huePerGlyph, saturation)`, `pulse(cyclesPerSecond, amount)`, `shimmer(glyphsPerSecond, bandWidth, boost)`,
`gradient(from, to)`, `typewriter(glyphsPerSecond)`. vui text effects can be adapted to this interface 1:1.

```java
GlyphEffect bounce = (g, t) -> g.offsetY -= Math.abs((float) Math.sin(t * 6 + g.index() * 0.5f)) * 3f;
```

## Canvas calls

`TextDrawer` translates to the object (camera relative), rotates by its orientation, scales by `(s, −s, s)` (font
y goes down) and calls `WorldCanvas.glyph(codepoint, x, y, argb, light, seeThrough)` per glyph; transformed glyphs are
wrapped in `push/translate/rotate/scale/pop`. `VideoSurface` status lines use `WorldCanvas.text`.
