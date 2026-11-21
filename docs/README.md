# Vibey Client docs

Start with the [main README](../README.md) for what Vibey Client is, installation, keys and privacy.

## For players and creators

| Doc | What's in it |
|---|---|
| [BLOCKBENCH.md](BLOCKBENCH.md) | Make cosmetics and emotes in Blockbench: install the plugin, model on the reference player, animate, particles and sounds, export, live link into the game. |
| [FORMAT.md](FORMAT.md) | The `.vibey` file format explained for creators: slots, attach points, coordinates, animations, events, limits, validation, examples. |
| [CONTENT.md](CONTENT.md) | The free starter set (every cosmetic and emote with previews) and the Node toolkit that builds it. |
| [ACCESSIBILITY.md](ACCESSIBILITY.md) | Farbsehfilter, kontrastreiche HUD-Anzeigen und Geräuschuntertitel. |
| [MODULE-MATRIX.md](MODULE-MATRIX.md) | Modul-Verfügbarkeit nach Minecraft-Version und Loader sowie unterstützte Server-Regeln. |
| [AFK.md](AFK.md) | AFK-Modus des Vibe Launchers: jede Instanz im Hintergrund (Fenster weg, 2 FPS, Ton aus), Messwerte, Serverregeln. |

## For developers

| Doc | What's in it |
|---|---|
| [SERVER-RULES.md](SERVER-RULES.md) | VibeyRules, Paper-API und Plugin-Channels für Server-Regeln, Wegpunkte und Nametags. |
| [SPEC.md](SPEC.md) | The shared contract for every part of the project: targets, coexistence with the standalone mods, module API requirements, design tokens, the normative `.vibey` format (§5), sync (§6), backend API (§7), live link protocol (§8). |
| [MODULES.md](MODULES.md) | Writing a module: lifecycle, settings, keybinds, network channel, events, services, backend access, toasts, i18n, mixins, version-specific code. |
| [UI.md](UI.md) | Design rules, screens (title, onboarding, Vibey menu, pause row), extension points (tabs, menu entries, title preview), widgets, UI tests. |
| [HUD.md](HUD.md) | HUD widgets and the HUD editor: the widget API for modules, styles, placement model, templates, share codes, server rules. |
| [AVATAR.md](AVATAR.md) | The cosmetics and emotes engine: packages, services, render path per Minecraft version, coordinates, tests. |
| [VIDEO.md](VIDEO.md) | Video screens (Fabric 1.21.11): placing, players (FFmpeg / MCEF Modern), setup prompts, sharing, wiring into the Media module. Research and licences: [VIDEO-RESEARCH.md](VIDEO-RESEARCH.md). |
| [ACCESS.md](ACCESS.md) | The beta access gate: `GET /api/v1/access`, 7-day cache of approvals, `403 ACCESS_*` / close `4403`, the title screen hint and the dev switch. |
| [NETWORK.md](NETWORK.md) | Every HTTP request the client makes (startup and during play), the shared HTTP client, caching/ETags, bootstrap, timeouts and retries, and how to measure it (`tools/net-bench`). |
| [NAMETAGS.md](NAMETAGS.md) | Names and ranks: seal, rank icons (glyph table, `rank-icons.json`), animated name styles (colour function + test vectors), chat hook, server-rule keys, local backend test. |
| [PORTING.md](PORTING.md) | Which modules run on which of the 20 targets, shared helpers for porting (vanilla HUD layers, screen input, `Gfx`), what changed between 1.21.1 and 26.3. |
| [INFO.md](INFO.md) | Info module (all targets): light level, chunk borders, block/mob card + world pinning, food gain, tool warning, TNT timer; fairness table, server-rule keys. |
| [PVP.md](PVP.md) | PvP & View module (all targets; render tweaks 1.21.11): zoom, toggle sprint/sneak, crosshair editor, block outline, hit colour, low fire, lower shield, server-rule keys. |
| [WAYPOINTS.md](WAYPOINTS.md) | Waypoints module (Fabric 1.21.11): beacons with direct manipulation, share codes, fair minimap, world map, death point, nether coordinates, map tile cache. |
| [CHAT.md](CHAT.md) | Chat module (Fabric 1.21.11): timestamps, history, search, copy a line, mention highlight, mention + DM toasts, spam collapse, filters, the chat window as a HUD element. |
| [SOCIAL.md](SOCIAL.md) | Social additions: Discord Rich Presence (own IPC client, privacy, app id) and streamer mode (names, skins, server address; `api/privacy`). |
| [AFK-AGENT.md](AFK-AGENT.md) | The AFK Java agent (`tools/afk-agent`): LWJGL/SDL/Vulkan/OpenAL hooks, name mapping (Mojang/obf/intermediary), what works on which version and loader, tests, release. Contract: [AFK-FILES.md](AFK-FILES.md), headless research: [AFK-HEADLESS-RESEARCH.md](AFK-HEADLESS-RESEARCH.md). |
| [CLIPS.md](CLIPS.md) | **Proposal** for the Clips module (replay buffer, auto clips, gallery): capture/encoder research with measurements, and the clip folder + JSON contract for the Vibe Launcher. |
| [../blockbench/README.md](../blockbench/README.md) | Developing the Blockbench plugin: tests, mock game server, code layout. |

How they fit together:

```
 Blockbench + plugin ──.vibey──► live link (SPEC §8) ──► game: avatar engine (AVATAR.md)
        │                                                     ▲
        └──export──► .vibey file (FORMAT.md / SPEC §5) ───────┤ built-in starter set (CONTENT.md)
                              │                               │
                              └──► Vibe backend catalog ──────┘ (SPEC §7)

 game: core + module API (MODULES.md) + Vibe UI (UI.md)
       modules: social · cosmetics · emotes · multiplayer · media (Fabric: lyrics & skies, video) · pvp · hud · info · camera · devlink
```

## Screenshots

[`screenshots/`](screenshots/) holds the images used by these docs, all taken from the automated client tests, the
Blockbench end-to-end test or the local social test setup.
