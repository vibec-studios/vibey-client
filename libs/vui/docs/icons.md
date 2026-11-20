# Icons

Small one-colour pixel icons (the 9 × 9 Vibe look) without one texture per icon. A mod ships one file per namespace,
`assets/<ns>/vui/icons.json`, and uses the icons by name:

```json
{
  "icons": {
    "gear":  ["....#....", ".#.###.#.", "..##.##..", ".##...##.", "###...###", ".##...##.", "..##.##..", ".#.###.#.", "....#...."],
    "plus":  "......... ....#.... ....#.... ....#.... .#######. ....#.... ....#.... ....#.... ........."
  },
  "aliases": { "settings": "gear" }
}
```

- Rows top to bottom, `#` = on, `.` = off; a list of strings or one string with rows split by spaces.
- All rows of an icon have the same length; icons may have any size (9 × 9 is the convention).
- `aliases` map extra names to existing icons.

```html
<icon name="vibeyclient:gear"/>                 <!-- namespace:name -->
<icon set="vibeyclient" :name="m.icon"/>        <!-- set = default namespace, name without one -->
<icon name="gear" style="color: $brand; size: 18"/>
```

A name without namespace and without `set` is looked up in every loaded set. The icon is drawn in the element's
`color`, centred in the content box, with whole-pixel cells when the box is at least the icon size (9 × 9 icon in an
18 × 18 box = 2 GUI px per cell), so it stays crisp at every GUI scale. Unknown names are reported once with file:line
and a suggestion (`unknown icon 'vibeyclient:gaer' (hint: did you mean 'vibeyclient:gear'?)`).

## Loading

Sets load lazily: the first `ns:name` lookup reads `assets/<ns>/vui/icons.json` through the document registry roots
(classpath, dev folders, overrides). In dev mode a saved `icons.json` reloads its set; documents pick it up on the
next paint. In code:

```java
vui.icons().load("mymod", "icons.json", jsonText);         // or put(new IconSet("mymod").put("dot", "###", "###", "###"))
PixelIcon gear = vui.icons().find("mymod:gear");
Diagnostics problems = vui.icons().diagnostics();           // parse errors of every loaded file
```

## Drawing (backends)

`Canvas.icon(PixelIcon, x, y, w, h, argb)` has a default that draws one `fill` per horizontal run of on-cells
(`IconFills`) – correct everywhere, a few fills per icon. A backend that prefers one quad per icon builds an atlas once:

```java
IconAtlas atlas = IconAtlas.build(allIcons, 256);   // white ARGB pixels, 1 px padding
// upload atlas.pixels() (atlas.width() × atlas.height()) as a texture, then in Canvas.icon:
// draw the quad with atlas.u0(icon) … atlas.v1(icon), tinted with argb (fall back to IconFills for unknown icons)
```

The headless canvas uses the fill default.
