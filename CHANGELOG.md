# Changelog

All notable changes to Vibey Client. Versions follow `mod_version` in `gradle.properties`.

## Unreleased – modules on every version

- HUD editor and widgets, Info (light level, chunk borders, block/mob card, food gain, tool warning, TNT timer), PvP
  (zoom, toggle sprint/sneak, crosshair editor) and Camera (freelook, orbit view) now run on every supported version,
  Fabric and NeoForge, 1.21.1 to 26.3 – before only on Fabric 1.21.11.
- Block outline, hit colour, low fire, lower shield and motion blur stay on 1.21.11 for now; on other versions their
  switches are hidden instead of doing nothing.
- Play links for a hosted world now carry the instance's Vibe pack (`vibe_pack` from the launcher's `instance.json`),
  also when the client renews the link: friends who join get the right modpack automatically. An older backend (400) or
  a removed pack (404) gets the link once more without it.

## 0.4.0-beta.3 – toast head icon fix (beta)

Hotfix for **0.4.0-beta.2** (beta / dev channel only; 0.3.2 stays the normal version).

- Fixed a VUI error banner (`unknown icon 'vibeyclient:head/...'`) when social/friend toasts displayed a player head texture instead of a pixel icon, and rendered toast player heads untinted via `<image>`.

## 0.4.0-beta.2 – companions, AFK cinematics and a screen editor (beta)

Another **beta**: you only get it on the beta / dev channel of the Vibe Launcher or from GitHub. 0.3.2 stays the
normal version.

> **Fabric 1.21.11 only.** Everything new in this beta is only in the Fabric 1.21.11 jar. All other jars (Fabric and
> NeoForge, other Minecraft versions) are the same as in beta.1, without these features.
>
> *Kurz auf Deutsch:* Alles Neue in dieser Beta (Item-Modelle, Effekte und Partikel, AFK-Cinematics, Screen-Editor,
> Server- und Weltenliste mit Freunde-Tab, Companions, farbige Modul-Icons) gibt es nur in der Fabric-1.21.11-Version.
> Alle anderen Versionen bleiben wie in beta.1, ohne diese Funktionen.

**Companions (new)**
- Living companions that follow you around, from companion packs. A seal, a penguin and a robot come with the client;
  more packs go into `.minecraft/companionpacks`, just like resource packs.

**AFK cinematics (new)**
- When you are AFK for a few minutes (or press **F9**), the camera flies on its own: your builds and the world, other
  players, an orbit around you, a panorama, your waypoints, the sky, a dronie or a mix of everything.
- The AFK screen has its own HUD layout, so you can show lyrics, the clock or whatever you like while it runs.

**Screen editor (new)**
- Press **F12** (or the pencil) in any menu to move, resize or hide its elements, group them in panels, and set the
  size, dimming and blur per screen.

**Item models**
- Move, turn and resize the item in each hand with an in-game editor.
- Swing styles and swing speed, enchantment glint colour and strength, and the size of dropped items and items in
  frames.

**Effects and particles**
- Tone down or switch off fire, the pumpkin overlay, the vignette, the totem animation, nausea and the hurt shake.
- Every particle type on its own: amount, size, colour or off. Presets Vanilla, Clean, PvP and Fancy, with a live
  editor.

**Server and world lists**
- Sort servers and worlds by drag and drop, including your own world order.
- A new **Friends** tab shows which friends are on which server (also on LAN), worlds they host, and your invites.
- New privacy switch *Share server address*, **off** by default.

**And also**
- Every module has its own coloured icon.

Servers can switch the new features off as before (e.g. `visuals.helditem`, `visuals.fire`, `visuals.pumpkin`,
`visuals.totem`, `visuals.particles`, `cinematic.afk`, `companion.show`).

## 0.4.0-beta.1 – the rebuilt client (beta)

This is a **beta**. It is not the normal release: you only get it if you pick the beta / dev channel in the Vibe
Launcher or download it from GitHub yourself. 0.3.2 stays the normal version. Please report anything odd.

> **Where are the new modules?** Waypoints, Info, Chat, Inventory, Servers, Camera, Visuals, HUD, Video, Clips, PvP
> and Streamer mode are **only in the Fabric 1.21.11 jar** for now. Every other jar (Fabric and NeoForge, all other
> Minecraft versions) has the rebuilt core and the new menus, but without these modules.
>
> *Kurz auf Deutsch:* Die neuen Module (Wegpunkte, Info, Chat, Inventar, Server, Kamera, Visuals, HUD, Video, Clips,
> PvP, Streamer-Modus) gibt es vorerst nur in der Fabric-1.21.11-Version. Alle anderen Versionen haben den neuen Kern
> und die neuen Menüs, aber ohne diese Module.

**New menus (all versions)**
- A cleaner, calmer Vibey menu with themes, better contrast, search in the quick settings and toasts you can click
  through. The old menus are still there under *Advanced* if you need them.
- The emote wheel and the devlink card for Blockbench use the new look too.

**Ranks and name styles (all versions)**
- Players can show a rank icon from the Vibe team and an animated name style (gradient, wave, rainbow, pulse, shimmer)
  next to their name – on nametags, in the tab list, in chat and in the friends list. You can hide your own rank.

**New modules (Fabric 1.21.11 only)**
- **Waypoints:** waypoints, a fair minimap, a world map, your last death point and nether coordinates.
- **Info:** light level and chunk info, a card for the block you look at, food info, a tool warning, a TNT timer.
- **Chat:** timestamps, search, copy, mentions, filters and toasts.
- **Inventory:** better tooltips, quick moves, search and hotbar presets.
- **Servers:** search, favourites, pings and a MOTD preview in the server list.
- **Camera:** freelook, orbit camera and motion blur, plus a screenshot gallery.
- **Visuals:** item physics and particle options.
- **HUD:** the HUD editor now selects several widgets at once, snaps and has presets; new timer, scoreboard, tab list,
  boss bar and title widgets.
- **Video:** video screens, the music panel and lyrics in the world are rebuilt and smoother.
- **Clips** and **PvP** as in 0.3.2, now in the new menus.
- **Streamer mode** (F7) hides names, skins and the server address while you stream.

**Under the hood**
- The UI, the 3D panels and the cosmetics engine are now their own parts inside the jar. You still install just one
  file.
- Cosmetics look exactly like before (checked pixel by pixel against the old engine).

## 0.3.2 – Clips and faster online features

**Clips** *(Fabric 1.21.11 for now)*
- A replay buffer: switch it on and press **F8** to save the last 30 seconds (10–120 s) as a video. Off by default.
- Automatic clips after a kill, a multi-kill or a clutch (and optionally your own death), from what the game already
  shows you. Servers can switch clips (`clips`) or only the automatic ones (`clips.auto`) off.
- Light on your PC: your graphics card's video encoder (NVIDIA, AMD, Intel, or Windows'), 720p30 by default, only while
  you play (not in menus, inventories, when the window is in the background or the FPS are low), at most 96 MB of
  memory and a 2 GB clips folder that cleans up after itself (kept clips stay).
- A gallery in the game: watch, keep, rename and delete clips, open the folder. The Vibe Launcher shows them too.
- FFmpeg is downloaded only after you agree (the same download as for video screens).

**Faster online features**
- Friends, chats and your cosmetics are there sooner after the game starts, and the friends list no longer freezes
  everything else while it loads skins.
- Your cosmetics show right away from the last session and are then checked for updates in the background.
- Far fewer requests to the Vibe servers while you play (changes still arrive instantly over the live connection).

## 0.3.1 – crash fix

- Fixed a crash at startup together with other crosshair mods (Centered Crosshair, Crosshair Indicator, …). Vibey's
  crosshair now works alongside them.
- If another mod already changes something a Vibey feature needs, the game still starts: the feature's settings page
  says it is not available and which mod is involved, instead of crashing.
- Low fire is drawn more safely next to other mods that change the fire overlay.

## 0.3.0 – HUD editor, PvP, media and a cleaner client

**Looks and feel**
- The title screen is vanilla again, with four small Vibey buttons in the top right (wardrobe, friends, media, Vibey
  menu).
- New Vibey settings: module tiles with an on/off switch, search, plain language; technical options are tucked away
  under *Advanced*.
- Vibey messages no longer cover Minecraft's own messages.
- Minecraft 26.3: buttons in Vibey menus can be clicked again (keys and shortcuts work too).

**HUD editor and widgets** *(Fabric 1.21.11 for now)*
- Press **H**: drag, resize and style everything on your screen, with snapping guides, multi-select, undo/redo and
  right-click options. Layouts stay put at any window size.
- 14 widgets: FPS, CPS, keystrokes, coordinates, armour, effects, clock, ping, server, speed, in-game time, item
  counter, memory, compass. Templates *PvP*, *Minimal*, *Building*, *Streamer*; share your layout as a code.

**PvP & view** *(Fabric 1.21.11 for now)*
- Zoom (**C**, mouse wheel for more), toggle sprint/sneak, your own crosshair, block outline colour, hit colour, lower
  fire and shield. Nothing beyond what vanilla allows.

**Media** *(Fabric)*
- *Lyrics* is now *Media*: lyrics float in the sky where you look, and the sky moves with the music (soft pulses,
  sparkling stars, aurora; flashes off by default).
- In-world screens *(Fabric 1.21.11 for now)*: put YouTube, web pages, your own videos or streams on a wall; friends
  with Vibey see them too (only after a click). Needed downloads are only fetched after you agree.

**Cosmetics**
- Pets can nap, slide when you sprint, glide and cheer when you play an emote; wings make room for backpacks.
  Soft, see-through effects like aurora look smooth now.

**Friends and servers**
- *Copy invite link* while you host – friends open it with the Vibe Launcher and land in your world.
- Servers can switch off single Vibey features (for example zoom) and tell you why when you join.
- The Vibe launcher can show what you are playing in your Discord status.
- Vibe moved to new addresses (vibe.axolite.app, *.axolite.cc). Your settings and login move along automatically, old
  links keep working, and a short notice explains it once.

The Blockbench plugin now has its own releases (`blockbench-v…`).

## Technical details of 0.3.0

### Domain move to axolite

Vibe moves from `henny.dev` to new addresses (vibe-launcher `docs/DOMAINS.md`). The old addresses keep working in
parallel for now, with the same server and database.

- **New defaults:** backend `https://vibe.axolite.app` (also the website link under *About*), relay
  `relay.axolite.cc:25565`. Invite links are now `https://axolite.cc/j/<code>` and hosted worlds get addresses like
  `mossy-otter-27.axolite.cc` (both come from the backend/relay as before).
- **Your settings move along once:** a stored old default backend (`https://vibe.henny.dev`, with or without a
  trailing slash) and relay (`relay.vibey.henny.dev` / `vibey.henny.dev`) switch to the new ones; your own addresses
  stay. Logins in `vibeyclient/auth.json` move to the new backend address, so nobody has to log in again; the cached
  loadout signing key counts for the new address too.
- **Old addresses keep working:** old invite links and `*.vibey.henny.dev` world addresses work as before; the dev
  login refuses both production names.
- **Notices:** the domain move is shown once as a toast and as a card at the top of *General* until you dismiss it
  (DE/EN). The client also shows the backend's announcements (`GET /api/v1/announcements`, public, at start and every
  6 hours, only with online features on), each once, respecting `until`, `apps` and `max_version`.
- README: resolved a leftover merge conflict in the multiplayer/HUD sections.

### Blockbench plugin released on its own

- Client releases no longer carry `vibey.js`; `vibey-manifest.json` says `"blockbench": null`. The plugin has its own
  GitHub releases (tag `blockbench-v<version>`, assets `vibey.js` + `vibey-blockbench.json`, never *Latest*) and its
  own changelog ([blockbench/CHANGELOG.md](blockbench/CHANGELOG.md)); the Vibe Launcher installs it from there.
- The optional cosmetic states, `offsetWhen` and DEV thumbnails ship with client 0.3.0 (docs said 0.2.2).

### HUD editor and widgets (Fabric 1.21.11 first)

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

### Media module

### Server rules

- Servers can switch off Vibey features for their players (zoom, freelook, HUD parts, emotes, pets, the server name
  in the Discord status, …) over the open `vibey:rules` channel. The client shows the server's message once per
  join, marks the settings "Disabled by this server" and restores everything when you leave. It announces its
  version and supported keys with `vibey:hello`. For server owners: the **VibeyRules** Paper/Spigot plugin
  (`VibeyRules-<version>.jar` on the release) or `config/vibeyclient-server.json` on Fabric/NeoForge servers
  (`/vibeyrules reload|status`). See [docs/SERVER-RULES.md](docs/SERVER-RULES.md).

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

### Invite links and Discord status (multiplayer)

- **Invite link** while you host (needs a Vibe login): *Copy invite link* in the host window (with the link and how
  long it is valid) and in the pause menu; toast "Link copied". Friends with the Vibe Launcher open it and land in
  your world with the right modpack. The link is renewed when your address changes and deleted when you stop
  hosting or quit. Hidden while online features are off or when the Vibe server has no invite links yet.
- The host window tells you how many mods you **added by hand** (friends need them too), read from the launcher's
  `.vibe/instance.json`.
- `.vibe/session.json` for the Vibe Launcher's Discord status (menu, singleplayer, hosting with link and players,
  multiplayer server – hidden when a server asks for it or you hide server addresses). New setting *Show what I'm
  playing to the launcher* (General → Online).

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
