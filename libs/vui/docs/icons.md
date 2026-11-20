# Icons

Small pixel icons (one-colour 9 × 9 glyphs, coloured 16 × 16 module icons) without one texture per icon. A mod ships one file per namespace,
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

## Coloured icons

An entry can also be an object with its own palette – for bigger, shaded pixel art such as module icons (16 × 16):

```json
"module_chat": {
  "palette": { "o": "#1b1430", "a": "#b9a6ff", "b": "#8b6cff", "c": "#5a3fd6", "w": "#ffffff" },
  "rows": ["..oooooooooooo..", ".oaaaaaaaaaaaabo.", "..."]
}
```

- `palette` maps one character (not `.`) to a colour (`#rgb`, `#rrggbb`, `#rrggbbaa`); `.` is transparent.
- `rows` work like the one-colour form; every character must be `.` or a palette key.
- The CSS `color` is ignored, only its alpha fades the icon; `opacity` works as usual. Both forms mix freely in
  one file, aliases too.
- Errors name the icon and row (`icon 'x': row 2 uses 'z', which is not in its palette`); a broken icon is skipped.
- Show them at their size or a whole multiple (`size: 16` / `32`) so every pixel stays one crisp cell.

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

`Canvas.icon(PixelIcon, x, y, w, h, argb)` has a default that draws one `fill` per horizontal run of same-colour
cells (`IconFills`) – correct everywhere, a few fills per icon. `PixelIcon.tinted()` tells one-colour icons (drawn in
`argb`) from coloured ones (`runColor(i)`, alpha scaled by `argb`'s alpha). A backend that prefers one quad per icon builds an atlas once:

```java
IconAtlas atlas = IconAtlas.build(allIcons, 256);   // ARGB pixels (white for one-colour icons), 1 px padding
// upload atlas.pixels() (atlas.width() × atlas.height()) as a texture, then in Canvas.icon:
// draw the quad with atlas.u0(icon) … atlas.v1(icon), tinted with argb if icon.tinted(), else white with argb's alpha
```

The headless canvas uses the fill default.
