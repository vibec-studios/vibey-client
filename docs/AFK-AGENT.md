# Vibe AFK agent

`tools/afk-agent` builds `vibe-afk-agent-<version>.jar`, a Java agent the Vibe Launcher attaches to **any**
Minecraft instance (`-javaagent:<jar>=<instance>/.vibe`): vanilla, Fabric, Quilt, Forge, NeoForge, any modpack,
with or without Vibey Client. In background mode it hides the window (also from the taskbar), slows the game to
2 frames per second and mutes the sound; back in the foreground everything is exactly as before. It also reports
chat, deaths, kicks, health and connection state to the launcher and sends chat lines/commands for it.

> **Deutsch, kurz:** Der AFK-Agent ist ein `-javaagent`, den der Launcher an jede Instanz hängt. Im Hintergrund:
> Fenster weg (auch Taskleiste), 2 FPS, Ton aus; zurück: alles wie vorher. Dazu Chat/Tod/Kick/Gesundheit als
> Ereignisse und Senden von Chat/Befehlen. Keine Bewegung, keine Anti-AFK-Tricks. Datei-Vertrag:
> vibe-launcher `contracts/afk/afk-files.md` (Kopie des Vertrags: siehe unten „Contract“).

It never moves the player, never clicks, never respawns and never works around AFK kicks. Many servers kick AFK
players or forbid AFK farming; the launcher says so on the first AFK start.

## How it works

Only stable, non-obfuscated places are hooked: LWJGL classes. The agent's `ClassFileTransformer` renames the
original method and adds a wrapper with the same signature that calls the agent first (`hook/LwjglTransformer`,
`hook/WrapperWriter`). The wrapper only uses `java.*` types and fetches the agent once through the system class
loader into a private static field, so it works in every class loader / module layer (vanilla, Knot, ModLauncher,
NeoForge's module layers).

| Hook | Used for |
|---|---|
| `GLFW.glfwSwapBuffers`, `SDLVideo.SDL_GL_SwapWindow`, `KHRSwapchain.vkQueuePresentKHR`, LWJGL 2 `Display.update(boolean)` | frame throttle (sleep until the next 1/fps slot) |
| `GLFW.glfwPollEvents`, `SDLEvents.SDL_PumpEvents` / `SDL_PollEvent`, LWJGL 2 `Display.update` | main-thread tick: apply the mode, read game state, send outbox lines (once per second) |
| `GLFW.glfwCreateWindow`, `SDLVideo.SDL_CreateWindow` | start hidden (`GLFW_VISIBLE=false` / `SDL_WINDOW_HIDDEN`), remember the window |
| `GLFW.glfwShowWindow`, `SDLVideo.SDL_ShowWindow` | the game's own "show" is skipped while the agent keeps the window hidden |
| `AL10.alListenerf(AL_GAIN, …)` | mute: the listener gain is 0 while muted, the game's value is remembered and restored |
| `AL.createCapabilities` (LWJGL 3), `AL.create(…)` (LWJGL 2) | OpenAL is only touched after the game created it (touching it earlier breaks LWJGL's static init, seen in testing) |

Hide/show uses `glfwHideWindow` / `glfwShowWindow` + `glfwFocusWindow` (or the SDL equivalents) on the main
thread. A hidden window is not in the taskbar. Mute is re-applied every 5 s while muted (an audio device change
makes Minecraft create a new OpenAL context with gain 1).

Game state comes from reflection with Mojang's names (`game/GameBridge`): `Minecraft.getInstance()`, `player`,
`screen`, `getCurrentServer()`, `LivingEntity.getHealth()`, `Player.getFoodData()`, `DeathScreen.causeOfDeath`,
`DisconnectedScreen.reason` / `details`, `ClientPacketListener.sendChat` / `sendCommand` (older: `LocalPlayer.chat`),
`ConnectScreen.startConnecting` for auto-reconnect. Runtime names are resolved per environment
(`game/BridgeLoader`):

- **mojang**: the class `net.minecraft.client.Minecraft` exists (26.1+ everywhere, NeoForge 1.20.5+, Forge 1.20.6+, Fabric dev runs) – names used directly.
- **obf**: vanilla ≤ 1.21.11 – `client_mappings.txt` (path in `afk.json`, the launcher downloads it from the
  version JSON `downloads.client_mappings`) maps Mojang → obfuscated names.
- **intermediary**: Fabric/Quilt ≤ 1.21.11 – Mojang → obfuscated (`client_mappings.txt`) → intermediary
  (`mappings/mappings.tiny` of the intermediary jar on the classpath).
- **srg/unknown**: Forge 1.17–1.20.4 (SRG member names) – game state off, the rest works.

Chat comes from `logs/latest.log` (`[CHAT]` lines, every version since 1.8, every loader). Whispers and mentions
are a heuristic (`ChatParser`). The player name comes from the game or the log line `Setting user: <name>`.

## What works where (agent 0.1.0)

| | Vanilla | Fabric / Quilt | NeoForge | Forge |
|---|---|---|---|---|
| Hide/show window, taskbar | 1.13+ | 1.14+ | all | 1.13+ |
| 2 FPS throttle | all | all | all | all |
| Mute / restore | all | all | all | all |
| Chat events (log) | 1.8+ | all | all | 1.8+ |
| Health, death, kick reason, send chat/commands, auto-reconnect | 1.14.4+ with `mappings`; 26.1+ without | 1.14.4+ with `mappings` (intermediary from the classpath); 26.1+ without | 1.20.5+ (Mojang names) | 1.20.6+; not 1.17–1.20.4 (SRG) |
| Auto-reconnect | 1.17+ (`ConnectScreen.startConnecting`) | 1.17+ | 1.20.5+ | 1.20.6+ |

LWJGL 2 (≤ 1.12.2): throttle and mute work, `capabilities.window` is `false` (the launcher hides the window by PID),
game state is off (no Mojang mappings before 1.14.4).

### Tested (21.11.2026, Windows 11, i5-12600K, Intel Arc A750)

Local server (offline mode, flat world), client joined with `--quickPlayMultiplayer`, no other mods.

| Run | Names | Result |
|---|---|---|
| Vanilla 1.21.11 | obf (`client_mappings.txt`) | hide/show, 2 FPS, chat, mention, whisper, outbox chat + `/me` + rate limit, health low, death with message, kick reason, auto-reconnect after 5 s, `agent_stop` on close – all ok |
| Fabric Loader 0.20.1 + 1.21.11 | intermediary | started hidden (window never visible), joined, chat, whisper, outbox, mute, foreground – all ok |
| Vanilla 26.3 (SDL3 backend, Java 25) | mojang (no mappings) | started hidden (SDL window and RenderPearl utility window invisible), joined, chat, outbox, health low, death with message, foreground – all ok |
| NeoForge 21.1.233 (1.21.1, module layers) | mojang | early FML window and game window started hidden, game state found through the module layer, mute, foreground – ok (title screen only, no 1.21.1 server at hand) |

Measured with `Get-Process` CPU time and the Windows "GPU Engine" counters over 20 s (window not focused, so
Minecraft's own unfocused limit applies in the foreground):

| Run | Foreground | Background (agent) |
|---|---|---|
| Vanilla 1.21.11 | 30 FPS, CPU 51.7 % of one core (3.2 % of 16), GPU 6.7 % | 2.0 FPS, CPU 6.0 % of one core (0.4 % of 16), GPU 0.3 % |
| Fabric 1.21.11 | 30 FPS, CPU 32.6 % of one core (2.0 % of 16), GPU 4.5 % | 2.0 FPS, CPU 4.9 % of one core (0.3 % of 16), GPU 0.3 % |
| Vanilla 26.3 | 30 FPS, CPU 22.6 % of one core (1.4 % of 16), GPU 2.8 % | 2.0 FPS, CPU 8.1 % of one core (0.5 % of 16), GPU 0.2 % |
| NeoForge 1.21.1, title screen | 60 FPS, CPU 12.9 % of one core, GPU 5.3 % | 2.2 FPS, CPU 2.7 % of one core, GPU 1.4 % |

RAM does not change (~850–950 MiB working set); the agent itself adds ~1 MiB.

Not tested yet: Forge, Quilt, NeoForge in a world, LWJGL 2 versions, Vulkan backend of 26.2/26.3 (hook verified in the JVM,
not in a running game), `AllowSetForegroundWindow` from the launcher, audible check of mute (verified through the
agent's state: the listener gain is set to 0 and restored to the game's value).

## Build, test, release

```
./gradlew -p tools/afk-agent --max-workers=2 test shadowJar sha256
```

- Output: `tools/afk-agent/build/libs/vibe-afk-agent-<version>.jar` + `.sha256`. Java 8 bytecode, ASM 9.8 shaded
  to `dev.henny.vibeafk.shaded.asm`, ~230 KiB.
- Tests: JSON/config/outbox/rate limit/event rotation/log tail/chat parser, ProGuard + tiny (v1 and v2) name
  mapping, game watcher (connect, health low, death, respawn, kick, reconnect backoff, give-up), stub LWJGL classes
  driven end to end (start hidden, skip the game's show call, throttle timing, mute and exact restore), and the real
  LWJGL jars (GLFW/OpenAL/Vulkan 3.3.3, SDL/Vulkan 3.4.3, LWJGL 2.9.3) transformed and loaded by the JVM verifier.
- Release: bump `agent_version` in `tools/afk-agent/gradle.properties`, merge, push tag `afk-agent-v<version>`.
  `.github/workflows/afk-agent.yml` tests, builds and creates the release with the jar and its `.sha256` (never
  "latest"). The launcher pins version + SHA-256.

## Contract

The file contract (owner: this repo) lives in the launcher hand-off folder as `contracts/afk/afk-files.md`;
summary: `afk.json` (launcher → agent: mode, fps, mute, hide, auto-reconnect, alerts, mappings path),
`afk-state.json` (agent → launcher: capabilities, env, game, stats, heartbeat every 5 s), `afk-events.jsonl`
(agent → launcher, `seq` + `ts`, rotated at 2 MiB), `afk-outbox.jsonl` (launcher → agent, every line acknowledged
with `outbox_ack`, 1 line per 1.5 s, burst 3). A copy is in [AFK-FILES.md](AFK-FILES.md).

## Not built

- The optional Vibey Client module `afk` (skip world/GUI rendering entirely while hidden). With 2 FPS the agent
  already brings rendering close to zero; see [AFK-HEADLESS-RESEARCH.md](AFK-HEADLESS-RESEARCH.md).
- A real headless mode (research only, same file).
