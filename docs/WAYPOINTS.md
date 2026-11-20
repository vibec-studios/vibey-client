# Waypoints module

Features #1–5 of [FEATURES.md](FEATURES.md): waypoints with world beacons and share codes, a fair minimap, a
full-screen map of explored terrain, a death point marker and Overworld ↔ Nether coordinates. Module id
`waypoints`, package `dev.henny.vibeyclient.module.waypoints`, **Fabric 1.21.11 only** for now
(`gradle/modules.gradle`). Ideas from Xaero/Lunar/LabyMod only, no code or assets.

> See also: [REBUILD.md](REBUILD.md) (rules, interaction model) · [MODULES.md](MODULES.md) · [HUD.md](HUD.md) ·
> [SERVER-RULES.md](SERVER-RULES.md) · [libs/r3](../libs/r3/README.md) · [libs/vui](../libs/vui/README.md)

## What the player gets

| Feature | How it works |
|---|---|
| Waypoints | `J` adds one at your feet (name *Waypoint n*, next palette colour). The list (module page button, map button or the unbound *Waypoint list* key) renames, recolours, hides, deletes, navigates and copies share codes. |
| Beacons | Every visible waypoint of the current dimension is an r3 world object: a coloured ◆ (☠ for the death point) floating 1.5 blocks above the point, a light beam and a label *name · distance*. Seen through terrain. |
| Minimap | HUD widget `waypoints.minimap` (top right, 96 × 96): explored terrain, own waypoints (dots, clamped to the edge when outside), north mark, your arrow. Turns with you or north up, 4 zoom steps. No players, no mobs, ever. Off by default, `Z` toggles it. |
| World map | `M`: full screen, explored terrain of this dimension, waypoints with names (zoom ≥ 2 or hover), your arrow. |
| Death point | Dying adds (or moves) the one *Death* waypoint. It is a normal beacon: look at it for the distance, left click for the arrow. |
| Nether coordinates | Look at a nether portal: the matching coordinates of the other dimension show under the crosshair. Hovered beacons show their counterpart too. Waypoints of the other dimension are shown at the matching place (⇄, dimmed, locked). HUD widget `waypoints.portal` (hidden by default) shows where you would be. |

## Gestures (r3 interaction model)

| Where | Do | Result |
|---|---|---|
| Beacon | look at it | selected: bigger, coordinates + counterpart shown |
| Beacon | left click | navigate to it (arrow around the crosshair + distance); again = stop; arriving (< 3 blocks) stops |
| Beacon | hold middle mouse, look elsewhere, release | moves the waypoint; snaps onto the block face you look at (the waypoint is the block in front of that face) |
| Beacon | wheel | size (stored per waypoint) |
| Beacon | right click | opens the list with this waypoint selected |
| Beacon | Esc / right click while holding, Ctrl+Z | cancel the move / undo the last move or resize |
| Map | drag | pan |
| Map | wheel | zoom around the cursor (½× … 8×) |
| Map | left click on empty ground | new waypoint there (height from the loaded chunk, else your height) |
| Map | left click on a waypoint / right click | open it in the list / navigate |
| Minimap | HUD editor (`H`) | move, wheel = size, like every widget |

Keys (Vibey Client category, rebindable): `addKey` J, `mapKey` M, `minimapKey` Z, `listKey` unbound.

## Data

| What | Where | Format |
|---|---|---|
| Waypoints | `config/vibeyclient/waypoints/<world>-<hash>.json` | `{"format":1,"waypoints":[{id,name,x,y,z,dim,color,size,visible,kind,created}]}` – read on join, written 0.6 s after a change (debounced, worker thread) and on leave |
| Map tiles | `<game>/vibeyclient/maps/<world>-<hash>/<dimension>/<tx>_<tz>.vwt` | 128 × 128 blocks per tile: `VWT1` magic, size, deflate(base colour + water depth per pixel, height per pixel) |

World key: `sp:<save folder>` in singleplayer, `mp:<server address>` otherwise (`data/WorldContext`). Broken JSON
entries are skipped (`WaypointJson`): coordinates |x|, |z| ≤ 3·10⁷, −2048 ≤ y ≤ 4096, ids `[A-Za-z0-9_-]`,
dimension ids `ns:path`, names cleaned (no control characters, no `§`, ≤ 32 chars), max 512 waypoints.

**Map cache bound:** setting `cacheMb` (advanced, default 128 MB, 16–1024) for the whole `maps/` folder. On join,
every 30 s and on leave the worker lists all tiles and deletes the least recently used ones (file mtime, refreshed
when a tile is loaded) until the total fits; tiles that are open in memory are never deleted
(`TileCachePolicy`). A tile is typically 3–8 KB.

## Share codes

`VWP1.` + base64url (no padding) of UTF-8 `x,y,z,d,rrggbb,name` entries joined by `|`. `d` is `o`/`n`/`e` for the
vanilla dimensions or the full id; `%`, `,` and `|` in names are escaped as `%25`, `%2C`, `%7C`. One waypoint is
about 30–40 characters, so it fits a chat line. *Paste code* in the list finds the first `VWP1.` code anywhere in
the clipboard (e.g. a copied chat line); at most 64 waypoints and 4096 characters per code, everything is validated
like the JSON file; imported waypoints get new ids.

## Fair play

Nothing shows what a vanilla player could not see: terrain only from chunks the client has loaded (explored), own
waypoints only, no entities, no other players. The minimap is behind server rule **`waypoints.minimap`** (declared
with `ServerRules.supports`, widget `.rules(...)`, settings `.serverRule(...)`, `serverRuleKeys()`): when a server
disables it the widget is hidden and its rows show the badge. It is off by default. The world map, beacons and
death point are not gated (own data, like a written note).

## Architecture

```
module/waypoints/
  WaypointsModule      settings, keys, lifecycle → WaypointsRuntime
  WaypointsRuntime     per-world session: store, navigator, death watcher, map service, beacon layer; tick + HUD
  WaypointIcons        9×9 icons for the module tile and HUD library
  core/                pure Java (no Minecraft): Waypoint, WaypointList, WaypointJson, ShareCode, NameEscape, Names,
                       Colors, Pos, Dimensions, NetherCoords, Navigation
  core/map/            pure: TileKey, TileData, ChunkSample, TileComposer, ColumnShade, TileShader, PixelOrder,
                       TileCodec, TileCachePolicy, CacheFile, LruSet, ScanOrder, MapView, MinimapMath
  data/                WaypointStore (load/save), StoreFiles, WorldContext (world key, dimension)
  actions/             WaypointActions (add/remove/rename/colour/visible/share/import/death), Navigator, DeathWatcher
  beacon/              r3 objects: BeaconLayer (sync list ↔ scene), BeaconFactory, BeaconEffect (content),
                       BeaconAnchor (distance clamp + size), BeaconDraw, BeaconLabels, BeaconGestures, BeaconMoves, BeaconState
  map/                 MapService, ChunkScanner + ColumnSampler (client thread), TileWorker + TileStore + TileShading
                       (worker), TileDisk, TileTextures (render thread), MapPainter, ScanStats
  hud/                 WaypointHud (registration), MinimapWidget + MinimapDots + MinimapMarks, PortalWidget, NavArrow, PortalLook
  screen/              Screens, ListScreen + ListController + WaypointRow, WorldMapScreen + MapController + MapInput + MapMarkers
  view/                Placement (where a waypoint shows in a dimension), CoordText
assets/vibeyclient/vui/waypoints/{list,map,ico,btn}.vui   assets/vibeyclient/textures/waypoints/icons/*.png
```

### Beacons (r3)

`BeaconLayer.sync` runs every client tick: when the list revision, the dimension, the *other dimension* setting
changed or r3 dropped an object (it clears the scene on world leave), it rebuilds one `WorldObject` per visible
waypoint (`kind = vibeyclient:waypoint`, id `wp:<id>`). Field edits (name, colour, size, position) do not rebuild:
the content reads the `Waypoint` live, so r3's undo history survives.

- Content `BeaconEffect` is an r3 `Effect` (free quads + text) and a `PointerContent` whose whole area is one
  control (`navigate`), so left click fires `ACTION`; borders are off (`edgeResizable(false)`), wheel = uniform resize.
- `BeaconAnchor` resolves the pose every frame: the point + 1.5 blocks; farther than 40 blocks it is drawn on the line
  of sight at 40 blocks (hover still works, r3's pointer reach is 64); `scale = size × max(1, distance/10)` keeps
  far beacons readable. Wheel/`RESIZED` stores `size = scale / factor`.
- `BeaconGestures` maps r3 events: `MOVED`/`PLACED` → position from the `BlockFaceAnchor` (block + face step) or
  the free position − lift, then our anchor is put back; `UNDONE` restores the stored position of the matching move.
- The ◆/☠ and the labels are see-through font glyphs; the beam is two additive ribbons (depth tested).

### Map pipeline (no frame drops)

1. **Client thread, ≤ 1.5 ms and ≤ 4 chunks per tick** (`ChunkScanner`): walks loaded chunks around the player in
   rings (`ScanOrder`, radius = render distance ≤ 16); rescans the 3 × 3 around you every 2 s, rings ≤ 4 every 20 s,
   the rest every 2 min. `ColumnSampler` reads per column the top block (`WORLD_SURFACE` heightmap; in ceiling
   dimensions the first floor below your height), skips `MapColor.NONE`, counts water depth, and emits a small
   `ChunkSample` (vanilla map colour, height, water depth).
2. **Worker thread** (`TileWorker`, one daemon thread, min priority): `TileComposer` writes the sample into its tile,
   `TileShading` shades the whole tile (relief against the northern neighbour 0.71/0.86/1.0, water darker with depth,
   `ColumnShade`) into a prepared ABGR buffer. Disk reads/writes, the 30 s autosave and the cache limit run here too.
3. **Render thread** (`TileTextures`): when a drawn tile has a newer shaded version, at most 6 uploads per painter
   per frame: one `memcpy` of the prepared buffer into the `NativeImage` + `upload()`. 160 textures max (LRU).
4. `MapPainter` draws visible tiles at integer block offsets inside one pose scale (no seams at any zoom); the
   minimap rotates the pose and clips with a scissor.

Measured (gametest, 2026-11-20, unfocused window ≈ 30 fps, render distance default): 303 chunks scanned,
0.054 ms per tick on average, 0.084 ms per chunk, worst tick 2.6 ms (first ticks, class loading); minimap paint
19 µs per frame; 15 texture uploads in total for 4 tiles.

Memory: ≤ 256 tiles in RAM (LRU, ~200 KB each incl. the shaded copy) and ≤ 160 GPU textures (64 KB each).

## Tests

- JUnit (`src/junit/java/.../module/waypoints/core`): share codes (round trip, escaping, garbage, ranges, name
  cleaning), JSON (round trip, broken entries), death marker uniqueness, names, clamping, nether maths, navigation
  angles, tile keys, compose + shade + codec round trip, north row shading, cache eviction, LRU, map view zoom/pan,
  minimap projection. `./gradlew :fabric-1.21.11:test --tests 'dev.henny.vibeyclient.module.waypoints.*'`
- Client gametest `WaypointsGameTest` (Fabric 1.21.11, real mouse/keyboard input): `J` adds, a second waypoint
  ahead, look at its beacon (selected), left click (navigation arrow), middle-drag onto the ground, wheel (bigger),
  right click (list with the waypoint selected; vui errors and every `t('…')` key checked), minimap drew tiles,
  `M` map (tiles, click places, drag pans, wheel zooms), share code round trip, portal coordinates, `/kill` → death
  marker → respawn → look at it, file written, perf numbers logged. Screenshots: `build/review/waypoints/`.
  `./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=WaypointsGameTest`

## Open

- Port to the other targets (the code uses 1.21.11 names directly: `Identifier`, `ResourceKey.identifier()`,
  `RenderPipelines` via `Gfx`, `NativeImage` pointer copy).
- Minimap "look + key = pan" from FEATURES.md is not built (wheel = zoom happens in the HUD editor as size; in game
  the zoom is a setting).
- Waypoint sync to friends / server (r3 `SharedObjects`) and keyboard input on world panels.
- Light beams are depth tested; behind a hill only the see-through marker and label show.
- The list screen uses its own small icon set (`textures/waypoints/icons`) until the client exports `ui/icons`.
