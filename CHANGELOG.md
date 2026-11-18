# Changelog

All notable changes to Vibey Client. Versions follow `mod_version` in `gradle.properties`.

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
