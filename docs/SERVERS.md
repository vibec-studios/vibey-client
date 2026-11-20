# Servers module (`servers`)

A richer multiplayer screen: live pings, search, favourites and a MOTD preview on hover (feature #16 of
[FEATURES.md](FEATURES.md)). **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `servers: ['fabric-1.21.11']`,
`BuiltinModules` registers it by reflection). Code: `src/main/java/dev/henny/vibeyclient/module/servers`.

> See also: [MODULES.md](MODULES.md) · [libs/vui](../libs/vui/README.md) (bar and preview documents)

Nothing here touches a server you are not looking at: it only pings the servers of your own list, with vanilla's
pinger, while the multiplayer screen is open.

## Features

| Feature | Settings (section) | How |
|---|---|---|
| Search | `search` (list) | vui field in the header (`assets/vibeyclient/vui/servers/bar.vui`, left above the list). Click or **Ctrl+F**; Esc leaves the field. Words are AND-ed against name, address and MOTD text, case and accents ignored (`core/ServerFilter`). The field shows how many rows are left. |
| Favourites | `favourites`, `favouritesFirst` (list) | A star at the bottom right of every row (hover shows it, gold = favourite); click toggles. Favourites are stored in the module data (`config/vibeyclient/servers.json`, `_data.favourites`, normalised addresses: lower case, default port dropped, `core/AddressKey`). The *Favourites n* chip (right side of the header) shows only favourites. With `favouritesFirst` favourites are listed first (stable order otherwise). |
| MOTD preview | `motdPreview` (list) | Hover a row for 350 ms: a card under the row (above it near the bottom) with the name, ping, the whole MOTD (up to 4 lines, colours, bold/italic, legacy `§` codes too), players online/max, version (red when incompatible or offline) and up to 6 names from the player sample (`assets/vibeyclient/vui/servers/preview.vui`). |
| Live ping | `livePing`, `pingEvery` (10–120 s, default 30) (ping) · advanced `pingParallel` (1–4, default 2) | `ping/LivePinger` re-pings the rows that are visible (filtered) and settled, favourites first, at most `pingParallel` at once (`core/PingSchedule`), on two daemon threads, with **vanilla's `ServerStatusPinger` of the screen** on a copy of the `ServerData` – so the row never flickers to "Pinging…"; the result is copied over and the row's status icon refreshed. A ping that does not answer in 8 s marks the row offline. |
| Ping cache | – | `ping/PingStore` (`core/PingCache`, 128 addresses, TTL = `pingEvery`): when the screen opens, rows whose result is fresh start with it instead of pinging again. The *Refresh* button (and F5) clears the cache like before. |

## How it hooks in

- `screen/ServerScreenHooks` (Fabric `ScreenEvents.AFTER_INIT` for `JoinMultiplayerScreen`) creates one
  `screen/ServerSession` per screen: two `DocumentView`s (bar, preview) drawn after the screen, clicks/keys routed to the
  bar, the live pinger ticked with the screen. Typed characters come from `mixin/ServerCharMixin`.
- `mixin/ServerSelectionListMixin`: `updateOnlineServers` restores cached pings, `refreshEntries` passes the entries
  through `screen/ServerListView` (filter + favourites first; LAN rows stay at the end).
- `mixin/OnlineServerEntryMixin`: draws the star (and marks the hovered row for the preview), handles the star click and
  translates vanilla's move up/down (`swap`) to the real list positions while the list is filtered or reordered
  (`screen/ServerSwap`).
- `mixin/JoinMultiplayerScreenMixin`: the refresh button clears the ping cache.

## Code map

| Package | Job |
|---|---|
| `servers` | `ServersModule` (settings, favourites storage) |
| `servers.core` | pure Java, JUnit in `src/junit/java/.../module/servers/core`: `AddressKey`, `Favourites`, `ServerFilter`, `ServerOrder`, `PingSchedule`, `PingCache`, `MotdSegment`, `MotdLines`, `LegacyFormat`, `HoverDelay` |
| `servers.ping` | `LivePinger`, `PingThreads`, `PingResult`, `PingStore` |
| `servers.screen` | `ServerScreenHooks`, `ServerSessions`, `ServerSession`, `BarController`, `PreviewController`, `PreviewData`, `MotdReader`, `ServerListView`, `ServerSwap`, `StarMark`, `ServerHover` |
| `servers.mixin` | `JoinMultiplayerAccess`, `JoinMultiplayerScreenMixin`, `ServerSelectionListMixin`, `ServerSelectionListInvoker`, `OnlineServerEntryMixin`, `OnlineServerEntryInvoker`, `ServerCharMixin` |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.servers.*"`.
- Game: `ServersGameTest` (+ `ServersTestKit`, `FakeStatusServer` – a tiny local status-protocol server that counts
  pings), screenshots in `build/review/servers/NN_*.png`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=ServersGameTest -Dvibeyclient.test.relay=off
```

It checks: module page en/de; a local fake server pings successfully and an unreachable one shows offline; the star
favourites the server and moves it first; Ctrl+F + "alpha" leaves one row, typing does not leave the screen, clearing
shows all; hovering opens the MOTD preview (second MOTD line, 3/20 players); the live pinger pings again after
`pingEvery` (set to 10 s); reopening the screen restores the cached result without a new ping. The test adds and
removes its two servers in `servers.dat`.

Porting later: `ServerSelectionList.Entry` render signature (`renderContent` since 1.21.9), `MouseButtonEvent`/
`KeyEvent`/`CharacterEvent` since 1.21.9, `EventLoopGroupHolder` (1.21.11).
