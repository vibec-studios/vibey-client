# Servers module (`servers`)

A richer multiplayer screen: live pings, search, favourites, a MOTD preview on hover, drag & drop sorting and a
*Friends* tab (feature #16 of [FEATURES.md](FEATURES.md)); the singleplayer world list gets drag & drop and its own
order. **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `servers: ['fabric-1.21.11']`,
`BuiltinModules` registers it by reflection). Code: `src/main/java/dev/henny/vibeyclient/module/servers`.

> See also: [MODULES.md](MODULES.md) · [libs/vui](../libs/vui/README.md) (bar and preview documents)

Nothing here touches a server you are not looking at: it only pings the servers of your own list (and, while the
*Friends* tab is open, the server addresses friends share), with vanilla's pinger, while the multiplayer screen is open.

## Features

| Feature | Settings (section) | How |
|---|---|---|
| Search | `search` (list) | vui field in the header (`assets/vibeyclient/vui/servers/bar.vui`, left above the list). Click or **Ctrl+F**; Esc leaves the field. Words are AND-ed against name, address and MOTD text, case and accents ignored (`core/ServerFilter`). The field shows how many rows are left. |
| Favourites | `favourites`, `favouritesFirst` (list) | A star at the bottom right of every row (hover shows it, gold = favourite); click toggles. Favourites are stored in the module data (`config/vibeyclient/servers.json`, `_data.favourites`, normalised addresses: lower case, default port dropped, `core/AddressKey`). The *Favourites n* chip (right side of the header) shows only favourites. With `favouritesFirst` favourites are listed first (stable order otherwise). |
| MOTD preview | `motdPreview` (list) | Hover a row for 350 ms: a card under the row (above it near the bottom) with the name, ping, the whole MOTD (up to 4 lines, colours, bold/italic, legacy `§` codes too), players online/max, version (red when incompatible or offline) and up to 6 names from the player sample (`assets/vibeyclient/vui/servers/preview.vui`). |
| Live ping | `livePing`, `pingEvery` (10–120 s, default 30) (ping) · advanced `pingParallel` (1–4, default 2) | `ping/LivePinger` re-pings the rows that are visible (filtered) and settled, favourites first, at most `pingParallel` at once (`core/PingSchedule`), on two daemon threads, with **vanilla's `ServerStatusPinger` of the screen** on a copy of the `ServerData` – so the row never flickers to "Pinging…"; the result is copied over and the row's status icon refreshed. A ping that does not answer in 8 s marks the row offline. |
| Ping cache | – | `ping/PingStore` (`core/PingCache`, 128 addresses, TTL = `pingEvery`): when the screen opens, rows whose result is fresh start with it instead of pinging again. The *Refresh* button (and F5) clears the cache like before. |

| Drag & drop (servers) | `dragReorder` (list) | Grab a row (not the icon, not the star) and drag it: after 4 px the row follows the mouse, the other rows slide apart and a teal box marks the drop place, the list scrolls near its top/bottom edge, **Esc** cancels. Dropping moves the server in `servers.dat` (the list vanilla's move arrows change) and keeps it selected. A click without moving still selects, a double click still joins. With a search or *Favourites first* active the drop is mapped to the real list: the server lands right before the visible row under it (`core/MoveTarget`); favourites only move among favourites (`DropSlot.group`). |
| World order | `worldSort` = custom / last played / name, `worldDrag` (worlds) | Drag a world in the singleplayer list the same way. The order is stored in `config/vibeyclient/world-order.json` (`{"version":1,"order":[<level folder ids>]}`, `core/OrderCodec`); worlds that are not in it yet (new ones) go to the top, by last played (`core/WorldOrder`). The chip right of the search field shows the sort and cycles it on click; dragging while sorted by date or name switches to *custom*. |
| Friends tab | `friendsTab` (list) | *Servers / Friends* tabs replace the title. *Friends* shows one list with sections **Invites** (Join = accept, x = decline, time left), **Playing on servers** (server, live ping, Join and **+** only when the friend shares the address), **Hosted worlds** (players, Join through the relay address). Rows show the server/world name with a dim detail: the shared address, *Address hidden* when the friend only shares the name, nothing for *Hidden server*. Hover a row for a card (what, since when). Drag a server row onto the *Servers* tab (it lights up) or press **+** to add it to your list. States: not signed in -> *Sign in*; social off -> *Open friends*; nobody playing -> *Open friends*. |

## Friends data (`api/service/FriendsActivity`)

Read-only service registered by the social module (`module/social/activity/FriendsActivityService`, all targets),
looked up with `Services.get(FriendsActivity.class)`; the servers module never imports social.

| Member | Meaning |
|---|---|
| `state()` | `ready`, `signedOut`, `connecting`, `error` |
| `revision()`, `listen(Runnable)` | changes (social state revision; listeners run on the client thread from social's tick) |
| `servers()` -> `OnServer(friend, server, address, since, lan)` | online friends whose activity has `server`, `serverName` or `serverAddress`. `server` is the display name (`serverName`, else a `server` that is no host name, else the shared address; null = hidden). `address` (`host[:port]`, IPv6 in brackets) only when shared: with `addressShared` from the backend, `true` -> `serverAddress`/`serverPort`, `false` -> none; without the field (old backend/launcher) the old guess: `join` address when `joinable`, else `server` when it looks like a host name (`SharedAddress`). `lan` = `serverKind` `lan`/`local`: `server` is only `serverName` (the backend's German "LAN-Welt" placeholder is dropped), `address` is always null; the row shows *LAN world* (or the name with *LAN world* as detail), the `lan` glyph, no ping, no Join/+ and no drag |
| `worlds()` -> `HostedWorld(host, world, address, port, players, since)` | the backend's friend sessions (`/hosting/friends`, `hosting.update`): `world` = `worldName`, else the presence world; `address`/`port` = `joinHost`/`joinPort`, else parsed from `address` (`HostedJoin`); plus friends whose activity is `hosting` with a join address |
| `invites()` -> `Invite(id, from, kind, target, host, port, expiresAt)` | open incoming invites; `target` = `destination.name`, else `target.name`; `host`/`port` from `destination` (null without one), `address()` joins them |
| `joinServer`, `joinWorld`, `accept`, `decline`, `signIn`, `openFriends` | the existing social flows (`InviteFlow.join`, invite accept/decline, login, friends menu) |

`Player(id, name, skinUrl, status, rank)`: `rank` is the identity's rank id; the row shows its label in the rank
colour from `client/ranks/Ranks`.

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
- Drag & drop: `drag/ListDrag` (one drag per list: press -> `core/DragTracker`, slot -> `core/DropSlot`, row offsets ->
  `core/GapOffsets`, `core/AutoScroll`) is fed by the screen session (press/release/Esc, mouse position every frame).
  `mixin/SelectionListDragMixin` shifts the rows around `AbstractSelectionList.renderListItems` (the dragged row is
  moved out of view, the gap box drawn) and puts them back afterwards, so clicks and hover keep vanilla positions.
  `drag/ServerDrop` and `worlds/WorldDrop` apply the drop.
- World list: `worlds/WorldScreenHooks` (`SelectWorldScreen`) -> `worlds/WorldSession` (sort chip
  `vui/servers/worlds.vui`, drag); `mixin/WorldSelectionListMixin` orders the summaries handed to `fillLevels`
  (singleplayer list only), `worlds/WorldOrderStore` reads/writes the order file.
- Friends tab: the tabs live in the bar document; `screen/VanillaChrome` hides the vanilla title and list and disables
  Join/Edit/Delete while *Friends* is open; `friends/FriendsTab` draws `vui/servers/friends.vui` over the list area,
  rebuilds rows from `core/FriendSections` (+ `friends/FriendItems`) on changes and once a second, pings shared
  addresses (`ping/FriendPings`), shows the hover card (`friends/FriendCard`) and handles the drag onto the *Servers*
  tab (`screen/TabDropZone`, `friends/AddServer`).

## Code map

| Package | Job |
|---|---|
| `servers` | `ServersModule` (settings, favourites storage) |
| `servers.core` | pure Java, JUnit in `src/junit/java/.../module/servers/core`: `AddressKey`, `Favourites`, `ServerFilter`, `ServerOrder`, `PingSchedule`, `PingCache`, `MotdSegment`, `MotdLines`, `LegacyFormat`, `HoverDelay`, `DragTracker`, `DropSlot`, `MoveTarget`, `ListMove`, `GapOffsets`, `AutoScroll`, `WorldSort`, `WorldOrder`, `OrderCodec`, `Durations`, `FriendSections`, `FriendLabels` (row title/detail) |
| `servers.ping` | `LivePinger`, `PingThreads`, `PingResult`, `PingStore`, `FriendPings` |
| `servers.screen` | `ServerScreenHooks`, `ServerSessions`, `ServerSession`, `BarController`, `PreviewController`, `PreviewData`, `MotdReader`, `ServerListView`, `ServerSwap`, `StarMark`, `ServerHover`, `VanillaChrome`, `TabDropZone` |
| `servers.drag` | `ListDrag`, `DragTarget`, `DragPaint`, `ServerDrop` |
| `servers.worlds` | `WorldScreenHooks`, `WorldSession`, `SortController`, `WorldDrop`, `WorldListView`, `WorldOrderStore` |
| `servers.friends` | `FriendsTab`, `FriendsController`, `FriendItem`, `FriendItems`, `FriendCard`, `AddServer` |
| `servers.mixin` | `JoinMultiplayerAccess`, `JoinMultiplayerScreenMixin`, `ServerSelectionListMixin`, `ServerSelectionListInvoker`, `OnlineServerEntryMixin`, `OnlineServerEntryInvoker`, `ServerCharMixin`, `SelectionListDragMixin`, `WorldSelectionListMixin`, `WorldSelectionListAccess`, `SelectWorldAccess` |

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

`ServerListsGameTest` (+ `ServerDragChecks`, `WorldOrderChecks`, `FriendsTabChecks`, `FieldChecks`, `FakeFriends`, `WorldFixtures`),
screenshots in `build/review/servers-friends/`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=ServerListsGameTest -Dvibeyclient.test.relay=off
```

It drags server 3 above server 1 (saved in `servers.dat`, stays selected), cancels a drag with Esc, checks a plain
click still selects, drags inside a search filter (mapped to the real list); builds three worlds (copies of one test
world with other names/dates), drags the oldest to the top (stored, survives reopening), cycles the sort chip; opens
*Friends* with a fake `FriendsActivity` (all three sections, ping of a shared address, hidden address without Join,
server name + address, name with *Address hidden*, hosted world name, invite destination),
hover card, Join/accept/decline, drags a friend's server onto *Servers* (added), empty, signed-out and off states.

Porting later: `ServerSelectionList.Entry` render signature (`renderContent` since 1.21.9), `MouseButtonEvent`/
`KeyEvent`/`CharacterEvent` since 1.21.9, `EventLoopGroupHolder` (1.21.11).
