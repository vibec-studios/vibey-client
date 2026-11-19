# Changelog

All notable changes to Vibey Client. Versions follow `mod_version` in `gradle.properties`.

## Unreleased – HUD editor and widgets (Fabric 1.21.11 first)

### HUD (new module, de: *HUD*)

- **14 widgets:** FPS, CPS, keystrokes (animated, with CPS), coordinates (facing, biome), armour durability (percent,
  bar, uses left), potion effects (replace the game's icons while shown), clock, ping, server (address can be hidden),
  speed, in-game time, item counter (arrows, totems, any held item; add as many as you like), memory, compass. Each
  has a compact and a detailed look and its own options.
- **Templates:** PvP, Minimal (the default on a fresh install), Building (*Bauen*), Streamer.
- Open the editor with **H**, the *HUD* entry in the pause menu, the HUD tile in the Vibey menu or Media → *Edit HUD
  layout*.

### HUD editor (every module, `ui/hud`)

- The game keeps running behind it. Drag anything; it snaps to the screen edges, the centre and other widgets with
  guide lines and distance readouts. **Eight handles** resize (proportional; Shift = free), the whole edge works on
  thin widgets. Marquee or Ctrl-click to select several and move them together; arrow keys nudge (Shift = 10 px).
  **Undo/redo** (Ctrl+Z, Ctrl+Y, Ctrl+Shift+Z – the letters of your keyboard layout).
- **Right-click:** Style…, Duplicate, Hide, Reset. **Style popover:** background, colour (swatches + colour picker),
  opacity, square or bevelled corners, border, text colour incl. accent and animated rainbow, shadow, alignment, size,
  detail, anchor and widget options – for one widget or for all (widgets inherit, can override).
- **Widget library** to add and remove widgets; **share codes** (`VHUD1.…`) and `.vhud` files (also drag & drop) to
  pass layouts around; a short **intro** the first time.
- Layouts are stored as anchor + offset + scale, so they look the same at any window size and GUI scale
  (`config/vibeyclient/hud-layout.json`, the old `hud.json` placements are taken over).
- Widgets a server switched off (`vibey:rules`, keys `hud.fps` … `hud.compass`) are hidden in game and shown greyed
  with *Disabled by server* in the editor. Modules add their own widgets through the widget API (docs/HUD.md).

## Unreleased – Media module

### Media (was "Lyrics", Fabric)

- The lyrics module is now **Media** (`media`, de: *Medien*): music, lyrics and skies, with room for more media
  parts (in-world video). Settings, on/off state and keybinds of the old `lyrics` module are carried over on the
  first start. With the standalone *Vibey Lyrics* installed only the lyrics & skies part switches off (with a note on
  the settings page); the rest of Media keeps working.
- **Sky lyrics where you look** (new default): the words rest in the sky while you look around near them and, after a
  short moment, glide calmly to where you look now – smooth, no overshoot, never below the horizon. The older modes
  (follow softly, glued to your view, one direction) stay under *Advanced*.
- **Skies that move with the music:** colors drift with the energy, the horizon glows with the bass, beats make
  stars sparkle, aurora curtains brighten with the mids, drops bloom. Own songs are analysed in sync with what you
  hear; music from the PC (Windows) through the sound the PC plays (WASAPI loopback, nothing recorded); otherwise
  the lyrics timing. *Effect strength* slider; flashes are off by default, at most 3 pulses per second.
- **HUD editor:** move and resize the HUD lyrics and the now playing bar (snapping to edges, centre and other
  elements, guides, reset). Media settings → *HUD layout → Edit*, or the *HUD editor* key. Generic, for every module
  (`ui/hud`).
- *Media* entry in the title screen and pause menu (replaces the optional *♪ Lyrics* pause button).

## 0.2.1 – open source, signed loadouts, status fix

### Fixes

- Title screen status: with an offline Minecraft profile (no Microsoft login) the status said *Signing in…* forever
  although nothing signs in. It now says *No Vibe login*; after logging out it says *Not signed in*, a refused login
  shows *Sign-in failed*, and *Signing in…* only appears while a login really runs.

### License

- The code is now licensed under the **Apache License 2.0** (was MIT); see `LICENSE` and `NOTICE`. Forks are
  welcome, but our names and the seal, our artwork, models, textures, sounds and the starter set (all rights
  reserved, paths in `NOTICE`) and the Vibe online services (official builds only) are not part of the code license
  – the policies for all Vibe Craft Studios repositories are in
  [vibec-studios/.github](https://github.com/vibec-studios/.github/blob/main/LICENSING.md). The multiplayer and lyrics modules keep the MIT notices of Vibey Multiplayer and
  Vibey Lyrics (`licenses/`). The jars now also carry `NOTICE_vibeyclient` and those MIT notices.

### Signed loadouts

- On servers that run Vibey Client, other players' cosmetics and emotes from the server channel are only shown when
  the Vibe backend signed them (Ed25519 token in the loadout packet, key from `/api/v1/meta`). Without a valid
  signature you only see items everyone has; the rest comes from the backend. A modified client can no longer show
  items it does not own. Server-channel protocol 2; older clients and servers keep working.

### Releases

- Releases are no longer pushed to the backend from CI: the GitHub release carries all jars, `vibey.js` and
  `vibey-manifest.json` (with sha256 and size of every file), and the backend imports it from GitHub.

## 0.2.0 – nametag icons and over-the-air cosmetics

### Nametag icons

- A small seal before the name of everyone who plays with Vibey Client – above their head in the world and in the
  tab list – plus badges for Vibe staff, supporters and creators.
- New module *Nametag icons* with switches for the nametag, the tab list and the badges. The icons are a tiny font
  of their own, so other mods that change nametags keep working.
- Players are recognised through the Vibe backend and, without online features, through the server channel when the
  server also runs Vibey Client (the server now tells everyone who has the mod).

### Over-the-air cosmetics

- New and updated cosmetics and emotes arrive without a client update: the client reloads the catalog when the
  backend announces a change, after reconnecting, when you join a world and every ~10 minutes.
- Downloads run in the background and in parallel; updated items swap in without a hitch and stay equipped.
  Unchanged items are no longer re-read on every catalog check.
- *New cosmetics available* notice when new items or newly unlocked items arrive (can be switched off under
  *Cosmetics*).
- Community items show *by <creator>* in the wardrobe.
- Items made for a newer Vibey Client are listed greyed out with *Update Vibey Client* instead of failing.

### Cosmetics engine

- `walk`, `fly` and `sneak` animations of cosmetics can now fire particles and sounds too (only `idle` did), and
  visibility toggles follow the animation that is playing.
- More forgiving with files from newer tools: unknown attach points, interpolations, event types and fields are
  skipped with a warning instead of rejecting the item; `minClientVersion` for items that need a newer client.

### Creator tools

- Blockbench plugin: **Submit to Vibe** sends your item to the Vibe team for review through the running game (its
  Vibe login), with an optional thumbnail from the viewport; **My submissions** shows the status and lets you
  withdraw open ones. Approved items reach every player over the air.
- Blockbench plugin version 0.2.0 (now versioned together with the mod).

### Distribution

- Releases are built and published by CI (`.github/workflows/release.yml`) to the Vibe backend, where the Vibe
  launcher installs the right jar for each Minecraft version (exact versions per jar in `gradle/mc-versions.json`).

### Fixes

- Minecraft 26.x: the dev tours can place client-side test players again.
- Emote locators placed on player bones (e.g. `right_arm`) now follow the arm (particles and sounds came from the
  player's position).

## 0.1.0 – first version

The first version of Vibey Client: one modular client mod by Vibe Craft Studios for Fabric and NeoForge,
Minecraft 1.21 – 26.3 (20 jars: 10 Minecraft lines × 2 loaders). Everything is free; there is no shop.

### Core and UI

- Modular core: modules with on/off switches, dependencies, typed settings (saved in `config/vibeyclient/`),
  keybinds, events, services, a server channel for module messages, English and German.
- Vibe design (black, purple, teal, square corners, short animations) with the grey seal mascot.
- Vibey title screen (the seal on its ice floe, backend status line), optional; the vanilla title screen keeps a
  small Vibey button.
- Three-step welcome: language, online features on/off, keys.
- Vibey menu on **Right Shift**: modules and their settings, global settings, search, Account and About tabs, full
  keyboard use, UI scale 2×–4×.
- Pause menu row (*Vibey*, *Wardrobe*, *Friends*) and toasts over every screen.

### Social

- Login to the Vibe backend with Mojang's server check; the Minecraft token never leaves the game.
- Friends with live status and activity, friend requests, direct and group chats, inbox, profiles, blocking.
- Invites into your hosted world with *Join* / *Decline* toasts, joinable worlds in the friends list.
- Privacy settings (requests, messages, invites, hosted worlds), status text, logged-in devices.
- Social sidebar on **G**, full-screen mode.
- Online features can be switched off completely (no connection at all).

### Cosmetics and emotes

- Data-driven `.vibey` format (v1) for cosmetics and emotes, one self-contained JSON file each.
- Seven cosmetic slots: cape, wings, aura, pet, shield, hat, back; animated (idle, walk, fly, sneak), glowing and
  translucent parts, animated textures, sprite particles, hidden under armor where it makes sense.
- Emotes with props, particles and embedded or vanilla sounds; third-person camera while emoting, stop on move.
- Wardrobe with categories, search, rotatable 3D preview and try-on; emote wheel on **B** with 8 slots and an
  editor.
- Sync to other players through the Vibe backend (works on any server) and through the server channel when the
  server also runs Vibey Client.
- Free starter set: 16 cosmetics and 10 emotes, all original, built into the jar so they also work offline.

### Modules from the standalone mods

- **Multiplayer:** the full Vibey Multiplayer mod: host your world for friends with one fixed address, manage
  players, whitelist and ban list, force mods.
- **Lyrics** (Fabric only): the full Vibey Lyrics mod: synced lyrics in 3D in the sky, custom skies, music from your
  PC, HUD and now-playing bar.
- Each module switches itself off when its standalone mod is installed and imports that mod's settings on first
  start.

### Creator tools

- Blockbench plugin (`blockbench/vibey.js`) with *Vibey Cosmetic* and *Vibey Emote* formats, reference player,
  particle/sound/visibility keyframes, validation, `.vibey` import and export.
- Live link (*Blockbench link* module, *Creator mode*): push from Blockbench into the running game as local *DEV*
  items, auto-push on change.
- Docs: [README](README.md), [docs index](docs/README.md), [FORMAT.md](docs/FORMAT.md),
  [BLOCKBENCH.md](docs/BLOCKBENCH.md), [CONTENT.md](docs/CONTENT.md).
