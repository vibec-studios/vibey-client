# Server rules / Server-Regeln

Server owners can switch off Vibey Client features (zoom, freelook, HUD parts, emotes, pets, …) for players on
their server. The client shows the server's message once per join and marks the features as "Disabled by this
server" in its settings. When the player leaves, everything comes back.

> **Deutsch, kurz:** Serverbetreiber können Funktionen von Vibey Client (Zoom, Freelook, HUD-Teile, Emotes, Pets, …)
> für Spieler auf ihrem Server abschalten. Der Client zeigt die Nachricht des Servers einmal pro Join als Toast und
> markiert die Funktionen in den Einstellungen als „Vom Server deaktiviert“. Beim Verlassen ist alles wieder da.
> Installation: Paper/Spigot → `VibeyRules-<version>.jar` in `plugins/`, `plugins/VibeyRules/config.yml` anpassen,
> `/vibeyrules reload`. Fabric/NeoForge-Server → das normale Vibey-Client-Jar in `mods/`, dann
> `config/vibeyclient-server.json` anpassen, `/vibeyrules reload`. Es ist ein freiwilliger Mechanismus für
> kooperative Clients – **kein Anti-Cheat** (siehe FAQ).

Contract: vibe-launcher `docs/PLAY-LINKS.md` §5. Client API: `dev.henny.vibeyclient.api.ServerRules`.

## Keys

A rule is a key from this list. `hud.*` switches off every key that starts with `hud.`, `cosmetics.*` every cosmetic
slot, and `*` alone switches off everything the client supports. Wildcards only work at the end. Unknown keys are
ignored, so you can list keys that only newer clients know.

| Key | What is switched off |
|---|---|
| `zoom` | Zoom |
| `camera.freelook` (or `freelook`) | Freelook (looking around without turning) |
| `camera.orbit` | Rotating the third-person camera around the player with the middle mouse (F5 stays) |
| `camera.*` | Both camera keys |
| `togglesprint` | Toggle sprint |
| `togglesneak` | Toggle sneak |
| `crosshair` | Custom crosshair |
| `blockoutline` | Custom block outline |
| `hitcolor` | Custom hit colour |
| `lowfire` | Low fire overlay |
| `lowshield` | Lowered shield |
| `hud.togglestate` | The toggle sprint/sneak state shown on the HUD |
| `hud.*` | Every HUD element below |
| `hud.fps` | FPS counter |
| `hud.cps` | Clicks per second |
| `hud.keystrokes` | Keystrokes |
| `hud.coords` | Coordinates |
| `hud.armor` | Armour status |
| `hud.effects` | Potion effects |
| `hud.clock` | Real-time clock |
| `hud.ping` | Ping |
| `hud.server` | Server address |
| `hud.speed` | Speed |
| `hud.time` | In-game time |
| `hud.items` | Item counter |
| `hud.memory` | Memory usage |
| `hud.compass` | Compass |
| `hud.timer` | Stopwatch / countdown / split timer |
| `emotes` | Emotes: no emote wheel, no own emotes, other players' emotes are not shown |
| `cosmetics.*` | Every cosmetic below |
| `cosmetics.pets` | Pets (own and other players') |
| `cosmetics.capes`, `cosmetics.wings`, `cosmetics.auras`, `cosmetics.shields`, `cosmetics.hats`, `cosmetics.backs` | That cosmetic slot (own and other players') |
| `discord.server` | The server name in the Discord / launcher status ("Playing on …"), and in saved clips |
| `clips` | Clips: no recording at all. The replay buffer stops at once and what it held is thrown away; the save key shows "Clips are off on this server" |
| `clips.auto` | Clips: no automatic clips (kill, multi-kill, clutch, death). Saving with the key still works |
| `nametags.*` | Every key below |
| `nametags.seal` | The Vibey seal before player names |
| `nametags.ranks` | Rank and badge icons before player names (nametag, tab list, chat) |
| `nametags.styles` | Coloured / animated name styles (nametag, tab list, chat) |
| `waypoints.minimap` | The minimap (terrain and own waypoints; it never shows players or mobs). Waypoints, beacons and the world map stay |
| `info.*` | Every key below |
| `info.light` | Light level of the looked-at block |
| `info.chunks` | Chunk border key (vanilla F3+G is not touched) |
| `info.card` | Block/mob info card and cards pinned into the world |
| `info.food` | Food gain on the hunger bar |
| `info.saturation` | Saturation on the hunger bar (off by default) |
| `info.toolwarn` | Tool break warning |
| `info.tnt` | TNT fuse timer |
| `inventory.tweaks` | Quick moves in inventories (Shift-drag and mouse-wheel moves; off by default in the client too) |
| `inventory.presets` | Hotbar presets (switch key, preset bar) |

The authoritative list for a given client is what it announces in `vibey:hello` (the plugin logs it, see below);
the keys come from `ServerRules.supports(...)` calls in the modules. The current Vibey Client declares `zoom`,
`togglesprint`, `togglesneak`, `crosshair`, `blockoutline`, `hitcolor`, `lowfire`, `lowshield`, `hud.togglestate`
(PvP module), `clips`, `clips.auto` (Clips module), `nametags.seal`, `nametags.ranks`, `nametags.styles` (Names and ranks), `waypoints.minimap` (Waypoints), `info.light`, `info.chunks`, `info.card`, `info.food`, `info.saturation`, `info.toolwarn`, `info.tnt` (Info module), `camera.freelook`, `freelook`, `camera.orbit` (Camera module), `inventory.tweaks`, `inventory.presets` (Inventory), `emotes`, `cosmetics.*` slots, `discord.server` and every `hud.*` key
above (HUD module). Listing a key an older client does not know is harmless.

## Paper / Spigot: the VibeyRules plugin

1. Download `VibeyRules-<version>.jar` from the [releases](https://github.com/vibec-studios/vibey-client/releases)
   (or build it: `./gradlew -p server-plugins/vibey-rules build` → `server-plugins/vibey-rules/build/libs/`).
2. Put it into `plugins/` and start the server once. Needs Java 21 and Paper/Spigot 1.20.6 or newer (built against
   the 1.21.4 API, only uses API that exists since 1.20; tested on Paper 1.21.11). No dependencies, no metrics.
3. Edit `plugins/VibeyRules/config.yml`:

```yaml
enabled: true
disable:
  - zoom
  - freelook
  - hud.coords
message: "Zoom, freelook and coordinates are off on this server."
worlds:              # optional, by world folder name; replaces "disable" (and "message" if set) in that world
  world_nether:
    disable: [zoom, hud.*]
log-hello: true      # log who announced Vibey Client and which keys their client supports
```

4. `/vibeyrules reload` (permission `vibeyrules.admin`, operators by default) applies the file to everyone online;
   `/vibeyrules status` shows the rules and which online players announced Vibey Client.
5. Staff that should keep everything: give them `vibeyrules.bypass` (nobody has it by default, not even operators).

The log then shows lines like
`[VibeyRules] Alex announced vibey:hello: vibey-client 0.3.0, supports [zoom, freelook, …] (does not know: [waypoints.minimap])`.

## Fabric / NeoForge servers

The normal Vibey Client jar also works on a dedicated server (`environment: "*"`). Put it into `mods/`, start the
server once and edit `config/vibeyclient-server.json`:

```json
{
  "enabled": true,
  "disable": ["zoom", "freelook", "hud.coords"],
  "message": "Zoom, freelook and coordinates are off on this server.",
  "worlds": { "minecraft:the_nether": { "disable": ["zoom", "hud.*"] } },
  "bypass": ["StaffName", "00000000-0000-0000-0000-000000000000"],
  "logHello": true
}
```

- `worlds` uses dimension ids (`minecraft:the_nether`; a bare `the_nether` means `minecraft:`). A world entry replaces
  `disable` (and `message` if it sets one) while the player is in that dimension.
- `bypass`: player names or UUIDs that get no rules.
- `/vibeyrules reload` and `/vibeyrules status` (operators, permission level 2), same as the plugin.
- A broken file is reported in the log and means "no rules" until it is fixed and reloaded.
- Singleplayer, LAN and Vibey-hosted worlds never send rules.

## Protocol (for other server software)

Two plugin-message channels. Each message body is the UTF-8 bytes of **one JSON object** – no length prefix, no
other framing – so `player.sendPluginMessage(plugin, "vibey:rules", json.getBytes(UTF_8))` is all it takes.

**`vibey:rules`** (server → client), max 16 KiB:

```json
{ "schema": 1, "disable": ["zoom", "hud.*"], "message": "No zoom on this server." }
```

- `schema` must be `1` (other values are ignored by this client). `disable`: list of keys, max 256, each max 64
  characters, `[a-z0-9_-]` segments separated by dots, optionally ending in `.*`, or `*` alone; invalid entries are
  skipped. `message` is optional, plain text (colour codes and control characters are stripped), max 256 characters.
- Each packet **replaces** the previous rules; `{"schema":1,"disable":[]}` clears them (send that when a player moves
  to a world without rules). The client clears the rules itself when it leaves the server.
- The client shows **one** toast per join (with `message`, or a generic text), however often you send.

**`vibey:hello`** (client → server):

```json
{ "schema": 1, "client": "vibey-client", "version": "0.3.0", "supports": ["zoom", "freelook", "hud.cps", "emotes"],
  "minecraft": "1.21.11", "loader": "fabric" }
```

Extra fields may be added; ignore what you do not know.

**Timing.** Like every plugin channel, the client only accepts `vibey:rules` after it registered the channel, and it
only sends `vibey:hello` after the server registered that one (`minecraft:register`). Vibey Client registers
`vibey:rules` right after joining; send the rules when you see that registration (Bukkit:
`PlayerRegisterChannelEvent`), and again when the hello arrives if you missed it. The client sends its hello as soon
as the server's registration of `vibey:hello` arrives (it keeps checking for 30 seconds after the join). Fabric and
NeoForge servers running Vibey Client negotiate the channels during login, so there the rules go out right at join.

The channels use the `vibey` namespace on purpose: they are a documented, client-neutral protocol, separate from
Vibey Client's internal `vibeyclient:*` channels (cosmetics/emote relay). The standalone Vibey Multiplayer mod
(mod id `vibey`) only uses `vibey:mods`, so both can be installed together.

## FAQ

**Can players bypass it?** Yes, if they want to. Server rules are a *cooperative* mechanism: a well-behaved Vibey
Client follows them, but a modified client, another client or an older version can simply ignore the packet. Use it
to state your server's expectations and to keep honest players within them – it is **not an anti-cheat**. For
anything that gives a real advantage, enforce it on the server (or with an anti-cheat).

**Players without Vibey Client?** Nothing happens. The plugin only sends to clients that registered `vibey:rules`,
vanilla clients never see a packet.

**Does it work with BungeeCord / Velocity?** The plugin runs on the backend servers; the proxy forwards plugin
messages. Put the plugin on every backend that should have rules (each can have its own config).

**Why does the player still see the feature in the settings?** The rows stay visible but dimmed, with "Disabled by
this server" instead of the control. The player's own values are kept and apply again when they leave.

**Is a key from the list not doing anything?** The player's client may be older; the plugin's hello log line says
which of your keys the client does not know.
