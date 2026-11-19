# Vibey Client: candidate features from other clients and mods

Research round, 2026-11-19. No code, no builds. Goal: at least 45 candidates Vibey does not have, with
direct-manipulation (look / grab / wheel-resize) ideas where they fit.

## 0. How to read this

- **Seen in** marks where a feature was actually found in this session: `L` Lunar features page (fetched), `E` Essential
  (fetched), `LM` LabyMod 4 (fetched + dev portal search), `N` NoRisk (fetched), `B` Badlion (search only, site 403 to fetch),
  `F` Feather (official server-API docs, search), `Mr` Modrinth or minecrafthub popularity table, `k` = my own knowledge,
  **not re-checked this session**. Treat `k` rows as leads to verify.
- **Popularity** is a judgement from how many big clients list the feature, plus Modrinth download counts where a search
  gave them (third-party ranking, approximate). There is no Modrinth download data for most rows.
- **Server safety** labels:
  - `OK` = shows only what vanilla already shows, or changes only the player's own UI.
  - `Check` = depends on the server. Confirm with the server's rules before shipping on by default.
  - `Gated` = a possible visibility or timing advantage. Ship only behind a server rule (`ServerRules.supports`) and default off.
  - `No` = never. It shows info the player could not see otherwise (ESP, entity radar on Hypixel, reach, auto-clicker).
- **Complexity:** S = a few days, M = about a week, L = two weeks or more, counting the Fabric 1.21.11-first rule.

**No official Hypixel allowed-mods page was retrieved.** The safety notes rest on forum threads and an unofficial wiki
(see §6). Fetching `hypixel.net/allowed-mods` is the first thing to do before any "OK" becomes a promise.

## 1. Already in Vibey (do not re-propose)

- HUD widgets: FPS, CPS, keystrokes, coordinates, armour, effects, clock, ping, server, speed, time, items, memory, compass.
- Zoom, toggle sprint and sneak, toggle-state indicator, crosshair (+ editor), block outline, hit colour, low fire, lower shield.
- Media: synced lyrics, sky effects, now-playing bar, video screens and HUD video overlay.
- Clips: replay buffer, automatic clips, gallery. Cosmetics, emotes, wardrobe. Friends, social chat, presence.
- Play links, relay world hosting, nametag badges, Blockbench link, server rules, HUD editor with templates and share codes.

## 2. Direct manipulation: the interaction grammar

These rules hold for all the ideas in the table. They reuse what the HUD editor already does.

| Gesture | Meaning | Conflict and fix |
|---|---|---|
| **Gaze** (crosshair ray, up to ~32 blocks) | Picks a Vibey world object. It gets an outline and a short label, like the block outline. | None. Ignore hits that land on blocks the player is breaking. |
| **Middle-drag** on a HUD element or world object | Grab and move. A world object keeps its distance along the ray. Shift = snap to the block grid. | Middle-click is *pick block* in creative. Suppress pick when a Vibey target is under the crosshair, and say so in the settings. |
| **Wheel** while hovering or while holding a grab key | Resize (HUD: 0.5–3×, same as the editor). | Wheel is hotbar. Zoom already uses wheel only while its key is held: same pattern. |
| **Look at an edge ~0.5 s** | Cursor becomes a resize handle; look angle = size. | Only for world panels; HUD keeps its 8 handles. |
| **Use key** (unbound by default) on the gazed object | Play / next / open / toggle. | Never bind to left-click, which is attack. |
| **Ctrl+Z**, Esc, Cancel | Reuse the HUD editor undo stack and save/cancel. | None. |

Hard rule: every world object is **client-only and shows nothing the player could not already see**. A waypoint may
show a fixed point the player placed. It may not follow other players (see §5).

## 3. Candidate table

Legend for columns: Seen in · Popularity · Safety · Cx · Direct-manipulation idea.

### 3a. Navigation and world

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 1 | **Waypoints** | Named points with colour, coords and a world beacon; share code | L (N / M keys), LM (Waypoints addon), k (Xaero) | Very high (Xaero, Lunar, LM) | OK for self-placed; no player markers | M | Gaze a beacon to select; middle-drag it in 3D; wheel = beacon width; use-key = navigate |
| 2 | **Minimap / radar** | Corner map with terrain and optional waypoints | LM (Minimap addon), Mr (Xaero 114.6M dl, third-party table), L unverified | Extremely high | **Check**: entity radar = No on Hypixel (forum + wiki). Ship a "terrain + own waypoints" mode, no entities | L | HUD widget: middle-drag, wheel zoom; hold a key + look = pan the map |
| 3 | **World map (full screen)** | Explored terrain cache with waypoints, pan and zoom | k (Xaero World Map) | High | Check (terrain only, self-explored: OK) | L | Drag to pan, wheel zoom, gaze a point to place a waypoint |
| 4 | **Death point tracker** | Remembers last death and shows a marker + distance | LM (Death-Finder, community) | Medium | OK | S | The death marker is a world waypoint; gaze it to see the arrow |
| 5 | **Overworld ↔ Nether coordinate** | Converts coordinates and shows both on the coord widget | k (Xaero, Lunar coords) | Medium | OK | S | Gaze a portal: show the matching coordinates |
| 6 | **Light level / chunk border overlay** | Shows block light numbers or chunk edges while held / toggled | k (F3+G vanilla, light mods) | Medium (builders, mob farms) | OK (vanilla data) | M | Gaze a block: its light level appears above it |
| 7 | **Schematic ghost (Litematica-lite)** | Loads a schematic and draws it as a ghost to build over | k (Litematica) | High with builders | **Check**: fine on creative/build servers, not on survival | L | Ghost placed where you gaze; middle-drag moves; wheel rotates; use-key confirms position |
| 8 | **Sign / text search** | Highlights signs containing a string | LM (SignSearch, community) | Low–medium | **Gated** (reads text the player saw, but searches across the world) | M | Type a string, gaze the highlighted sign to frame it |

### 3b. Information on screen

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 10 | **Block info overlay** (Jade / WTHIT) | Gaze a block or mob and see name, mod, tool, harvest info, crop growth | Mr (Jade ~70M dl) | Very high | OK (vanilla data; server-sent state only) | M | **Is** gaze-to-inspect. Hold a key to pin it; middle-drag the card |
| 11 | **Food / saturation info** (AppleSkin-like) | Hunger and saturation gain for each food, tooltip + HUD | k (AppleSkin) | High | OK | S | Gaze or hover food: the tooltip shows the gain |
| 12 | **Tooltip extras** (durability %, enchant tier, shulker contents) | Adds numbers to item tooltips and previews containers | LM (Inventory mod: container preview, durability) | High | OK | S–M | Hover shulker: preview opens on hold, no click |
| 13 | **Toolbreak warning** | Warns when the held tool is nearly broken | LM (community) | Medium | OK | S | HUD widget that flashes; no world element needed |
| 14 | **TNT / fuse timer** | Shows seconds left on lit TNT in the world | B (TNT Timer) | Medium (Hypixel BedWars, PvP) | OK | S | Gaze a TNT: the timer appears; middle-drag = move label |
| 15 | **Stopwatch / countdown / speedrun timer** | Start/stop/split timer widget and countdown | LM (SpeedrunTimer, community), k | Medium | OK | S | HUD widget; use-key starts, wheel changes countdown length |
| 16 | **Scoreboard tweaks** | Hide red numbers, move or resize the sidebar, text shadow | k (vanilla options + mods) | Medium | OK (display only) | S–M | Scoreboard becomes a HUD element: middle-drag and wheel |
| 17 | **Tab list upgrades** | Sort, ping bars, team colours, hide-empty, bigger rows | k | Medium | OK (display only) | S–M | Tab list as HUD element; gaze a player row = their skin preview |
| 18 | **Boss bar mover** | Move, shrink and stack the boss bars | k | Low–medium | OK | S | Same HUD element model |
| 19 | **Rich server list** | Live pings, MOTD, sorting, search, favourites | LM (Rich Server List), k (Lunar) | Medium | OK | M | Gaze a server row: preview MOTD |
| 20 | **Floating damage numbers** | Damage popping up over hit mobs / players | k | Medium | Check (damage comes from health delta; avoid exact numbers if server hides health) | M | Gaze to toggle numbers on/off per entity |
| 21 | **Nametag: ping / health tag** | Ping or health above heads | LM (PingTag), k | Medium | Check (health may be hidden or spoofed by the server) | S | Gaze a nametag: the detail appears |
| 22 | **Hit / kill sound cues** | Hitmarker sound and kill sound (PvP) | k | Medium (PvP servers) | OK (sound only) | S | Sound settings only |

### 3c. Chat and social

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 23 | **Chat upgrade** | Timestamps, search, copy message, history, mention highlight, tabs | LM (tabbed chat), k | Very high | OK | M | Gaze/hover a line: copy button; chat window is a HUD element (middle-drag, wheel) |
| 24 | **Chat anti-spam / filters** | Collapses repeats, hides configurable patterns | k | Medium | OK | M | None |
| 25 | **Auto GG / auto-text triggers** | Sends a line on win, game end or keyword | B (AutoGG, AutoTip), Badlion Hypixel group | High | **Gated**: spam risk; off by default, rate-limited, server rule | S | None (keep it boring) |
| 26 | **Mention / notification toasts** | Toast + sound for friend mention or DM | k | Medium | OK | S | Toast stack already exists: reuse |
| 27 | **Discord Rich Presence** | Shows the current game/world to Discord | LM (dev portal lists it), k | Medium | OK, hide server address like social setting | M | Settings only |
| 28 | **Nick hider / streamer privacy** | Hides own and others' names and skins in chat and HUD | k (streamer mods); E-like | Medium | OK | M | Hide own nametag; toggle with a use-key |
| 29 | **Proximity voice chat** | Voice channel in world, mutes, push-to-talk | LM (VoiceChat), F (Voice), k (Simple Voice Chat) | High | **Check**: needs server support and its own network path | L | Gaze a player: mute/volume slider |
| 30 | **Screenshot gallery** | Browse, sort by world/server, favourite, edit | E (screenshots), LM (interactive screenshots), F (Screenshot) | Medium–high | OK (sharing = privacy review) | M | Gallery is a world panel: gaze to focus, drag to move |
| 31 | **Daily world photo** | One auto-screenshot per day | N (McReal) | Low–medium | OK | S | None |
| 32 | **Client mini-games** | Tetris / Pong in the lobby (client-only) | N | Low–medium | OK | M | A world panel you look at to play |

### 3d. Visuals and camera

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 33 | **Freelook** | Hold a key to look around without turning the body | L (named on features page) | High | Check (camera-only, reveals nothing; confirm) | S | Freelook + gaze: aim at world objects without moving |
| 34 | **Better perspective** | Third-person skin view that can be rotated | LM (Better Perspective) | Medium | OK | S | Drag the skin preview (middle) to rotate |
| 35 | **Dynamic lights** | Held items, glowing mobs and drops emit light | k (LambDynamicLights, OptiFine) | High | **Gated**: visibility in dark caves; default off | M | Gaze a light source: show the radius |
| 36 | **Item physics** | Drops fly, bounce and merge like a physics model | LM (Item Physics, official) | Medium | OK (drops are already present) | S–M | None |
| 37 | **Particle multiplier / hide** | Scale or hide particles, keep damage/crit | k | Medium | OK | S | None |
| 38 | **Motion blur** | Blur on fast camera turns (accessibility toggle) | L (k), B (Motion Blur) | Medium | OK | S | None |
| 39 | **Time & weather changer (client)** | Client-side time/weather override for screenshots | k (Badlion, Lunar Dynamic time) | Medium | **Gated** (night/fog removal reads as advantage) | S | Gaze the sky: a clock wheel |
| 40 | **Fullbright / gamma beyond max** | Lights the dark | B (Fullbright), L (Full Bright) | Very high | **Gated**: visibility advantage (PVP.md already excludes it) | S | None. Keep it off unless a server rule allows it |
| 41 | **Clear water / fog removal** | Removes underwater or fog haze | LM (Clear Water) | Medium | **Gated**: underwater visibility | S | None |
| 42 | **Custom hurtcam** | Size and shake of hurt camera | LM (CustomHurtCam) | Low–medium | OK | S | None (extend the hit-colour editor) |
| 43 | **Motion of held items / swing** | Swing and sway styles for the held item | k | Low | OK | S | Gaze at the held item in the preview to tune |
| 44 | **Cape / HD skin library** | Skins in a library, HD skins, skin slots | LM (HDSkins, Skin Browser), E (skin switching) | High | OK | M | Skin library as a world panel: middle-drag |
| 45 | **UI themes / palettes** | Switchable UI themes (Fancy, Classic, custom) | LM (Themes) | Medium | OK | M | Theme preview is a live panel |
| 46 | **Entity culling / frame boost** | Skip rendering of hidden entities and tiles | LM (Enhanced Render Engine), B (FPS boost), N | High | OK | L | None |

### 3e. Inventory and controls

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 47 | **Mouse Tweaks** | Drag to pick matching items, shift-click matching stacks | Mr (Mouse Tweaks) | High | OK (vanilla clicks) | S–M | None |
| 48 | **Item Scroller** | Wheel moves items, faster crafting and trading | Mr (Item Scroller) | High | OK (vanilla clicks, rate-limit) | S–M | None |
| 49 | **Inventory sorting / profiles** | Sort inventory, locked slots, gear sets | Mr (Inventory Profiles Next, AGPL) | High | **Check**: auto-sort sends packet bursts; some servers flag. Gated: rate limit + server rule | M | Gaze a chest: sort preview |
| 50 | **Inventory search** | Highlights matching items | LM (Inventory mod) | Medium | OK | S | Search field as HUD panel |
| 51 | **Hotbar presets** | Save and switch hotbar loadouts with one key | k | Medium | OK | M | Gaze a preset bar in the HUD: use-key |
| 52 | **Resource pack quick-switch** | Toggle packs in-game without the menu | k | Low–medium | OK | S | None |
| 53 | **Bedwars / Hypixel stat overlays** | Bed status, stats lookups | B (Bedwars Beds, Hypixel group) | Medium | **No** unless server-approved. Stat lookups via API are typically against rules | S | None |

### 3f. Special or non-core

| # | Feature | What it does | Seen in | Popularity | Safety | Cx | Direct-manipulation idea |
|---|---|---|---|---|---|---|---|
| 54 | **Toolbar/hotkey macros** | Chains keys or commands | k | Low | **No** on most servers | M | None (do not build) |
| 55 | **Calendar / notepad widget** | Small HUD notes and calendar | k (Essential-like) | Low | OK | S | Panel: drag and type |
| 56 | **Auto-respawn / AFK helpers** | Respawn on death, AFK pings | k | Low | **Check**: Hypixel frowns on AFK tools | S | None (do not build) |
| 57 | **Build helper grid** | Shows grid planes and measurements | k | Low | OK | M | Gaze to place a grid plane |

**Not recommended at all (No):** ESP and player radar in the world, X-ray, reach or hitbox change, auto-clicker, killaura,
tab-list or nametag reveal of hidden info, Bedwars API overlays. These break Vibey's own rule (PVP.md: no advantage beyond vanilla).

## 4. Top 30 for Vibey (recommended order)

Scored on: player demand, fit with what Vibey already has, server safety, and how well it uses direct manipulation.

1. **Waypoints** (#1): demand, a direct-manipulation showcase, OK on servers.
2. **Minimap, fair-play mode** (#2): biggest demand; ship without entities.
3. **Block info overlay, Jade-like** (#10): gaze-to-inspect is the signature interaction.
4. **Food / saturation info** (#11): small, safe, popular.
5. **Freelook** (#33): asked for by almost every client user; camera-only.
6. **Chat upgrade** (#23): timestamps, search, copy, mention highlight.
7. **Inventory tweaks: Mouse Tweaks + Item Scroller** (#47, #48): two popular mods, easy to reproduce.
8. **Tooltip extras and container preview** (#12): safe, lots of value.
9. **Death point tracker** (#4): short, reuses the waypoint beacon.
10. **Toolbreak warning** (#13): small, widely used.
11. **TNT timer** (#14): very common on Hypixel-style servers.
12. **Stopwatch / countdown widget** (#15): HUD-only, low risk.
13. **Discord Rich Presence** (#27): cheap, respects the social "hide server" setting.
14. **Nick hider / streamer privacy** (#28): Vibey already has a streamer template; this makes it real.
15. **Screenshot gallery** (#30): pairs with clips gallery and launcher.
16. **Scoreboard and tab-list tweaks** (#16, #17): display-only, many requests.
17. **Hotbar presets** (#51): useful for PvP and building.
18. **Rich server list** (#19): replaces part of the vanilla screen.
19. **Better perspective (3rd-person skin rotate)** (#34): cheap, makes cosmetics look good.
20. **Item physics** (#36): small visual upgrade, listed by LabyMod.
21. **Particle multiplier / hide** (#37): cheap and popular.
22. **Motion blur** (#38): accessibility option, cheap.
23. **Chat anti-spam and filters** (#24): useful with social chat.
24. **Light-level / chunk-border overlay** (#6): builders and farm players.
25. **Overworld ↔ Nether coordinates** (#5): small extension of coords.
26. **Entity culling / frame boost** (#46): large effort but big performance gain (L); flag as a later milestone.
27. **Cape / HD skin library** (#44): ties to cosmetics module.
28. **UI themes / palettes** (#45): ties to HUD editor.
29. **Dynamic lights, gated** (#35): popular but behind a rule; default off.
30. **Schematic ghost (Litematica-lite)** (#7): biggest direct-manipulation win (ghost placed where you gaze), but L and only on build servers.

Pass-over (for now): voice chat (#29, L, needs server cooperation), mini-games (#32), time changer (#39), Bedwars overlays (#53, No).

## 5. Vibey-specific notes

- **Server rules:** every feature above that is Gated or Check gets its own `ServerRules.supports` key (e.g. `minimap`,
  `dynlights`, `inventory.sort`, `chat.autotext`) and a default-off setting. This is the same pattern as PvP and clips.
- **Minimap entity radar** must never ship on by default. Keep it behind a rule that the server can grant, and default it off.
- **Waypoints and minimap** should show only what the player placed or what the terrain shows. Do not draw other players.
- **Direct manipulation** is cheapest when built on the HUD editor (`HudElement` + `HudLayout.draw`), so world panels
  reuse drag, snap, wheel-resize and undo. A world-panel element needs only the ray-pick and the 3D transform.
- **Middle-click conflict:** creative pick-block. Suppress it when a Vibey target is under the crosshair.
- **Wheel conflict:** the hotbar. Use the Zoom pattern: wheel changes meaning only while a key is held.
- **i18n:** every row needs `en_us` and `de_de` strings (`checkI18n`).

## 6. Sources

Official, fetched or searched this session:
- Lunar Client features page: https://www.lunarclient.com/features (fetched; `/mods` 404)
- Lunar Waypoints guide: https://www.lunarclient.com/news/everything-you-need-to-know-about-lunar-clients-waypoints-mod
- Lunar PvP mods news: https://www.lunarclient.com/news/lunar-client-mods-for-the-best-pvp-experience
- Essential: https://essential.gg/en (fetched); changelog https://essential.gg/changelog
- LabyMod services: https://labymod.net/services (fetched); addon dev portal: https://dev.labymod.net/pages/addon/ and https://dev.labymod.net/pages/server/labymod/overview/
- LabyMod community addons: https://github.com/RappyLabyAddons
- NoRisk: https://norisk.gg (fetched)
- Badlion mod library: https://client.badlion.net/mods (search only; https://www.badlion.net/free-minecraft-mods returned 403)
- Feather server-API mods: https://docs.feathermc.com/server-api/mods/ (official docs, search)
- Modrinth popularity (third-party ranking): https://minecrafthub.io/articles/best-client-side-minecraft-mods
- Mod pages: Inventory Profiles Next https://modrinth.com/mod/inventory-profiles-next ; Item Scroller https://modrinth.com/mod/item-scroller ; Mouse Tweaks https://minecrafthub.io/mods/mouse-tweaks
- Xaero's Minimap: https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap

Third-party or unofficial (lower trust):
- BisectHosting Lunar mods list: https://www.bisecthosting.com/blog/lunar-client-mods-list
- Lunar minimap claims conflict across sites; treat as unverified.
- Salwyrr: https://github.com/armandinadreyer53472979039/Salwyrr-Client and https://endmods.com/salwyrr-launcher-for-minecraft/ (marketing; cracked-account launcher, no verified feature list)
- NoRisk: https://endmods.com/norisk-client/ ; Feather: https://github.com/bilalatif089/feather-client (unofficial)
- Hypixel forum: https://hypixel.net/threads/xaeros-minimap-allowed-just-for-use-of-waypoints.4789292/ ; https://hypixel.net/threads/anyone-know-a-good-waypoints-mod.3234815/
- Games Learning Society (minimap and waypoint policy, unofficial): https://www.gameslearningsociety.org/wiki/are-minimaps-banned-on-hypixel/
- Hypixel allowed-mods page (not retrieved, the authority to check): https://hypixel.net/allowed-mods

Gaps:
- Badlion site returned 403 to the fetch agent: its list comes from search snippets only.
- Salwyrr and Feather lists are unofficial: no reliable feature list.
- Popularity numbers exist only for Xaero's (~114M) and Jade (~70M) from a third-party ranking. Everything else is judgement.
- AppleSkin, dynamic lights, freelook (other than Lunar), time changer and the Bedwars overlays were not confirmed this session (`k`).
