# AFK mode (background start)

The Vibe Launcher can start any instance "in the background" (experimental feature flag, off by default): the game
runs normally with all its mods, but the window is hidden, the game draws 2 frames per second and is muted. The
launcher shows chat, health and the connection, can send chat lines and commands, and brings the window back on
request – exactly as it was.

> **Deutsch, kurz:** Der AFK-Modus des Vibe Launchers startet eine beliebige Instanz im Hintergrund: Fenster
> versteckt, 2 FPS, Ton aus; Chat, Gesundheit und Verbindung im Launcher, Senden von Chat/Befehlen, Fenster auf
> Knopfdruck wieder da. Das macht ein Java-Agent – **ohne** Vibey Client, für Vanilla, Fabric, Quilt, Forge,
> NeoForge und jedes Modpack. Der Agent bewegt den Spieler nie und umgeht keine AFK-Kicks; viele Server kicken
> AFK-Spieler oder verbieten AFK-Farmen.

| Part | Where | Doc |
|---|---|---|
| Java agent `vibe-afk-agent.jar` (hooks, events, sending, reconnect) | this repo, `tools/afk-agent` | [AFK-AGENT.md](AFK-AGENT.md) |
| File contract launcher ↔ game (`.vibe/afk.json`, `afk-state.json`, `afk-events.jsonl`, `afk-outbox.jsonl`) | this repo (owner) | [AFK-FILES.md](AFK-FILES.md) |
| Release of the jar (`afk-agent-v<version>`, SHA-256) | `.github/workflows/afk-agent.yml` | [AFK-AGENT.md](AFK-AGENT.md#build-test-release) |
| Feature flag, start menu, session chip, chat window, toasts | vibe-launcher | launcher docs |
| Real headless mode (no window at all) | research only | [AFK-HEADLESS-RESEARCH.md](AFK-HEADLESS-RESEARCH.md) |

## Measured (agent 0.1.0)

| Instance | Foreground | Background |
|---|---|---|
| Vanilla 1.21.11 | 30 FPS, 51.7 % of one CPU core, GPU 6.7 % | 2 FPS, 6.0 % of one core, GPU 0.3 % |
| Fabric 1.21.11 | 30 FPS, 32.6 % of one core, GPU 4.5 % | 2 FPS, 4.9 % of one core, GPU 0.3 % |
| Vanilla 26.3 (SDL3) | 30 FPS, 22.6 % of one core, GPU 2.8 % | 2 FPS, 8.1 % of one core, GPU 0.2 % |

Details and what works on which version and loader: [AFK-AGENT.md](AFK-AGENT.md).

## Vibey Client

Nothing in Vibey Client is needed. An optional `afk` module that skips world and GUI rendering entirely while hidden
is not built: the agent's 2 FPS already bring the GPU near zero, and the remaining CPU is game logic. If it is
built later it only reads `afk.json` and never writes events (the agent stays the only writer, see the contract §9);
its server-rule key would be `afk`.

## Server rules

The agent does nothing a server could forbid beyond what a player with a minimised window does: it does not move,
click, respawn or send anything by itself. Chat lines and commands are only sent when the player types them in the
launcher (rate-limited to one per 1.5 s). Auto-reconnect after a kick is off by default and uses a backoff
(10 s, doubling, at most 5 attempts by default).
