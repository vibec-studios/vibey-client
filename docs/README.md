# Vibey Client docs

Start with the [main README](../README.md) for what Vibey Client is, installation, keys and privacy.

## For players and creators

| Doc | What's in it |
|---|---|
| [BLOCKBENCH.md](BLOCKBENCH.md) | Make cosmetics and emotes in Blockbench: install the plugin, model on the reference player, animate, particles and sounds, export, live link into the game. |
| [FORMAT.md](FORMAT.md) | The `.vibey` file format explained for creators: slots, attach points, coordinates, animations, events, limits, validation, examples. |
| [CONTENT.md](CONTENT.md) | The free starter set (every cosmetic and emote with previews) and the Node toolkit that builds it. |

## For developers

| Doc | What's in it |
|---|---|
| [SPEC.md](SPEC.md) | The shared contract for every part of the project: targets, coexistence with the standalone mods, module API requirements, design tokens, the normative `.vibey` format (§5), sync (§6), backend API (§7), live link protocol (§8). |
| [MODULES.md](MODULES.md) | Writing a module: lifecycle, settings, keybinds, network channel, events, services, backend access, toasts, i18n, mixins, version-specific code. |
| [UI.md](UI.md) | Design rules, screens (title, onboarding, Vibey menu, pause row), extension points (tabs, menu entries, title preview), widgets, UI tests. |
| [HUD.md](HUD.md) | HUD widgets and the HUD editor: the widget API for modules, styles, placement model, templates, share codes, server rules. |
| [AVATAR.md](AVATAR.md) | The cosmetics and emotes engine: packages, services, render path per Minecraft version, coordinates, tests. |
| [VIDEO.md](VIDEO.md) | Video screens (Fabric 1.21.11): placing, players (FFmpeg / MCEF Modern), setup prompts, sharing, wiring into the Media module. Research and licences: [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md). |
| [NETWORK.md](NETWORK.md) | Every HTTP request the client makes (startup and during play), the shared HTTP client, caching/ETags, bootstrap, timeouts and retries, and how to measure it (`tools/net-bench`). |
| [PVP.md](PVP.md) | PvP & View module (Fabric 1.21.11): zoom, toggle sprint/sneak, crosshair editor, block outline, hit colour, low fire, lower shield, server-rule keys. |
| [../blockbench/README.md](../blockbench/README.md) | Developing the Blockbench plugin: tests, mock game server, code layout. |

How they fit together:

```
 Blockbench + plugin ──.vibey──► live link (SPEC §8) ──► game: avatar engine (AVATAR.md)
        │                                                     ▲
        └──export──► .vibey file (FORMAT.md / SPEC §5) ───────┤ built-in starter set (CONTENT.md)
                              │                               │
                              └──► Vibe backend catalog ──────┘ (SPEC §7)

 game: core + module API (MODULES.md) + Vibe UI (UI.md)
       modules: social · cosmetics · emotes · multiplayer · media (Fabric: lyrics & skies, video) · pvp (Fabric 1.21.11) · devlink
```

## Screenshots

[`screenshots/`](screenshots/) holds the images used by these docs, all taken from the automated client tests, the
Blockbench end-to-end test or the local social test setup.
