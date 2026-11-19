# New features (rebuild round)

30 features Vibey Client does not have yet, picked from the research of this round: other clients' feature lists
(Lunar, Badlion, Feather, LabyMod, Essential, NoRisk – ideas only, never code or assets), top client-side Modrinth
mods, Reddit comments (Arctic Shift archive), Trustpilot reviews and changelogs.

What players say, in short: they want small, focused, lean features they control; they hate ads, upsells, telemetry,
bloat, endless menus and clients that break their own mods. Minimap/waypoints and HUD tools are the most used;
replay/recording is wanted (we have Clips). So: every feature is its own small module, off-switchable, no shop,
no telemetry, as little text as possible.

Every feature follows the interaction model of `docs/REBUILD.md`: things on the HUD or in the world are grabbed with
the middle mouse, resized with the wheel, scaled at the border, used by looking at them. Nothing shows information a
vanilla player could not see; features that could be an advantage sit behind a server rule and are off by default.

| # | Feature | Module | Direct manipulation | Safety |
|---|---|---|---|---|
| 1 | Waypoints: named, coloured, world beacons, share codes | `waypoints` | Look at a beacon to select; middle-drag to move; wheel = beacon size; use key = navigate | ok (own points only) |
| 2 | Minimap, fair mode: terrain + own waypoints, no entities | `waypoints` | HUD object: middle-drag, wheel = zoom, look + key = pan | rule `waypoints.minimap` |
| 3 | World map (full screen, explored terrain) | `waypoints` | Drag pans, wheel zooms, click places a waypoint | ok |
| 4 | Death point marker + distance | `waypoints` | It is a waypoint; look at it for the arrow | ok |
| 5 | Overworld ↔ Nether coordinates | `waypoints` | Look at a portal: matching coordinates | ok |
| 6 | Light level + chunk border overlay | `info` | Look at a block: its light level floats above it | ok |
| 7 | Block/mob info card (Jade-like) | `info` | Look to inspect; hold key to pin the card; middle-drag the card | ok |
| 8 | Food + saturation info | `info` | Hover food: the gain shows on the hunger bar | ok |
| 9 | Tooltip extras: durability %, shulker/bundle preview | `inventory` | Hover a shulker: preview opens, no click | ok |
| 10 | Tool break warning | `info` | HUD object, flashes | ok |
| 11 | TNT fuse timer | `info` | Look at lit TNT: seconds left | ok |
| 12 | Stopwatch / countdown / split timer | `hud` | HUD object; use key starts; wheel = countdown length | ok |
| 13 | Scoreboard as HUD object (move, scale, hide numbers) | `hud` | Middle-drag, wheel | ok |
| 14 | Tab list upgrades (ping bars, sort, heads, ranks) | `hud` | Tab list as HUD object; hover a row = skin preview | ok |
| 15 | Boss bar + title mover | `hud` | Middle-drag, wheel | ok |
| 16 | Rich server list (pings, search, favourites, MOTD preview) | `servers` | Hover a row: MOTD preview | ok |
| 17 | Chat upgrade: timestamps, search, copy, mentions, history | `chat` | Chat is a HUD object; hover a line = copy button | ok |
| 18 | Chat filters + spam collapse | `chat` | – | ok |
| 19 | Mention + DM toasts | `chat` | Toast stack: middle-drag the stack | ok |
| 20 | Discord Rich Presence (respects privacy settings) | `social` | – | ok |
| 21 | Streamer privacy: hide names, skins, server address | `social` | One key toggles | ok |
| 22 | Screenshot gallery (with Clips) as world panel or screen | `clips` | Panel: look to browse, middle-drag, wheel | ok |
| 23 | Freelook (camera only) | `camera` | Hold key; look at world objects without turning | rule `camera.freelook` |
| 24 | Rotatable third-person / skin view | `camera` | Middle-drag rotates | ok |
| 25 | Motion blur (toggle, strength) | `camera` | – | ok |
| 26 | Item physics for drops | `visuals` | – | ok |
| 27 | Particle multiplier / hide | `visuals` | – | ok |
| 28 | UI themes (Vibe dark, light, high contrast, custom accent) | core (vui theme) | Live preview panel | ok |
| 29 | Mouse tweaks + item scroller (drag/wheel moves stacks) | `inventory` | Wheel over a stack moves it | rule `inventory.tweaks` (rate-limited) |
| 30 | Inventory search + hotbar presets | `inventory` | Search field as HUD object; look at a preset bar + key | ok |

Also this round (already decided): ranks with icons + animated name styles (`nametags`), music/video controls you
look at in the world (`media` on r3), accessibility (colour-blind safe themes, bigger UI scale) inside 28.

Not built, ever: ESP, entity radar, X-ray, reach, auto-clicker, macros, Bedwars API overlays, fullbright/fog removal
without a server rule.

Order: after vui and r3 have their Minecraft backends. One agent per module, Fabric 1.21.11 first.
Sources: `docs/research/` (`research-client-features.md`, `research-reddit.md`,
`research-player-wishes.md`, `research-names-ranks-3d.md`).
