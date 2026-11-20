# Social module: Discord status and streamer mode

Features 20 and 21 of [FEATURES.md](FEATURES.md). The rest of the social module (backend connection, friends,
chats, invites, presence) is described in [MODULES.md](MODULES.md#vibe-backend-and-toasts) and SPEC §7.

## Discord Rich Presence (`module/social/discord`, all targets)

Shows *Playing Minecraft* with Vibey details in Discord. Written by us: Discord's local IPC (no library, no
native code), so there is no extra licence to ship.

**Protocol.** Discord listens on a local pipe/socket: Windows `\\.\pipe\discord-ipc-0` … `-9`, macOS/Linux
`$XDG_RUNTIME_DIR`/`$TMPDIR`/`$TMP`/`$TEMP`/`/tmp` + `discord-ipc-0` … `-9` (also the Flatpak and Snap subfolders),
see `core/PipePaths`. A frame is `op (int32 LE) · length (int32 LE) · UTF-8 JSON`; ops `0` handshake, `1` frame,
`2` close, `3` ping, `4` pong (`core/IpcFrame`, `core/IpcOp`). The client sends `{"v":1,"client_id":…}`, waits for
`evt: READY`, then `{"cmd":"SET_ACTIVITY","args":{"pid":…,"activity":{…}|null},"nonce":"n"}` and reads until the
answer with the same nonce (pings are answered, `evt: ERROR` becomes an `IpcException`). All IO runs on one daemon
thread (`DiscordWorker`), never on the render thread or the shared `Tasks` pool.

| Class | Job |
|---|---|
| `core/IpcClient` | handshake, `setActivity(json or null)`, close – sequential request/answer over an `IpcChannel` |
| `core/RpcPayload` + `RpcInputs`, `RpcLabels`, `RpcPlace` | the activity JSON (pure) |
| `ipc/WindowsPipeConnector`, `ipc/UnixSocketConnector`, `ipc/Connectors` | the local channel per OS (`RandomAccessFile` on the pipe, Java 16 Unix domain sockets) |
| `DiscordWorker` | connect with backoff (15 s doubling to 2 min), send only changes (refresh every 60 s), clear + close on stop |
| `DiscordPresence` | every 2 s on the client thread: place (menu, singleplayer, multiplayer, hosting), status, privacy → `RpcInputs` |

**What is sent.** `details` = *In the menus* / *Singleplayer* / *Multiplayer* / *Hosting a world* (player's
language), `state` = *Away* / *Busy* from the social status, `timestamps.start` = when this place was entered,
`assets.large_image` = `vibey` (an asset of the Discord application), `large_text` = `Vibey Client <version> · <mc>`.
Never world names, never the player's name, never coordinates.

**Privacy.** The activity is cleared (`activity: null`) when *Show friends what I play* (`showPresence`) is off or the status
is *Invisible*. The server address is added only when *Show server* (`discordServer`, **off** by default) is on,
*Hide server address* (`hideServerAddress`) is off and streamer mode does not hide the server. Nothing runs while the
social module or online features are off, or when the build has no application id.

**Application id.** `discord_app_id` in `gradle.properties` → `BuildInfo.DISCORD_APP_ID` (like `default_backend`):
official builds only, forks register their own application. Empty = Rich Presence does nothing. Dev runs:
`-Dvibeyclient.discordAppId=<id>`. The application needs an art asset named `vibey`.

Settings (*Discord* card): *Discord status* (on), *Show server* (off, shown while the first is on).

## Streamer mode (`module/social/privacy` all targets, `module/social/streamer` Fabric 1.21.11)

One key (**F7**, *Streamer mode*) or the switch on the module page. It hides what the player picked:

| Option | Where |
|---|---|
| *Hide names* | world nametags (`NameTagStreamerMixin`, `EntityRenderer.getNameTag`), tab list (`TabListStreamerMixin`, `PlayerTabOverlay.getNameForDisplay`), every chat line incl. hover cards and shift-click insertions (chat module, `LineDecor` + `Redact`), chat toasts. Own name → *Me*, others → *Player 1*, *Player 2* … (stable while it is on). |
| *Hide skins* | every player's skin, also yours and the tab list heads → the default skin (`PlayerInfoSkinMixin`, `PlayerInfo.getSkin`). |
| *Hide server* | the address boxes of *Direct connection* and *Edit server* show `•••` (`AddressMask`, an `EditBox` formatter added on screen init), social presence sends *Multiplayer*, Discord never shows the server. |

Names known to the redactor: your own, every player in the tab list and your friends (refreshed once a second while
on). Names in other text (signs, books, scoreboard, other mods' HUDs) are not touched.

The shared state is `api/privacy/Privacy` (all targets, pure): `Flags(names, skins, server)`, `NameAliases`,
`NameRedactor` (whole-word, case-insensitive, longest names first, no overlaps), a version counter and listeners.
Social sets it (`StreamerMode`), every module may read it – this is how the chat module hides names without
importing social. The streamer mixins use priority 900 so they run before the nametags module's icon decorators.

The *Hide names* and *Hide skins* rows only show where the `social.streamer` part is built (Fabric 1.21.11);
*Hide server* works on every target (presence, Discord).

## Tests

- JUnit: `RpcPayloadTest` (payloads, privacy, frames, pipe paths), `FakeIpcServerTest` (a fake Discord on a Unix
  domain socket in a temp folder: handshake, set + clear, ping/pong, error answer, Discord not running, the worker),
  `NameRedactorTest`.
- Gametest: `ChatGameTest` covers streamer mode (chat, tab list, skins, F7 on/off, masked address box).
