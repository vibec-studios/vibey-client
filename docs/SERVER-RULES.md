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

| Key | What is switched off | Module ID |
|---|---|---|
| `zoom` | Zoom | `pvp` |
| `camera.freelook` (or `freelook`) | Freelook (looking around without turning) | `camera` |
| `camera.orbit` | Rotating the third-person camera around the player with the middle mouse (F5 stays) | `camera` |
| `camera.*` | Both camera keys | `camera` |
| `cinematic.afk` | AFK cinematics: the camera flying around while you are away (stops a running one at once) | `cinematic` |
| `togglesprint` | Toggle sprint | `pvp` |
| `togglesneak` | Toggle sneak | `pvp` |
| `crosshair` | Custom crosshair | `pvp` |
| `blockoutline` | Custom block outline | `pvp` |
| `hitcolor` | Custom hit colour | `pvp` |
| `lowfire` | Low fire overlay | `pvp` |
| `lowshield` | Lowered shield | `pvp` |
| `hud.togglestate` | The toggle sprint/sneak state shown on the HUD | `pvp` |
| `hud.*` | Every HUD element below | `hud`, `pvp` |
| `hud.fps` | FPS counter | `hud` |
| `hud.cps` | Clicks per second | `hud` |
| `hud.keystrokes` | Keystrokes | `hud` |
| `hud.coords` | Coordinates | `hud` |
| `hud.armor` | Armour status | `hud` |
| `hud.effects` | Potion effects | `hud` |
| `hud.clock` | Real-time clock | `hud` |
| `hud.ping` | Ping | `hud` |
| `hud.server` | Server address | `hud` |
| `hud.speed` | Speed | `hud` |
| `hud.time` | In-game time | `hud` |
| `hud.items` | Item counter | `hud` |
| `hud.memory` | Memory usage | `hud` |
| `hud.compass` | Compass | `hud` |
| `hud.timer` | Stopwatch / countdown / split timer | `hud` |
| `emotes` | Emotes: no emote wheel, no own emotes, other players' emotes are not shown | `emotes` |
| `cosmetics.*` | Every cosmetic below | `cosmetics` |
| `cosmetics.pets` | Pets (own and other players') | `cosmetics` |
| `cosmetics.capes`, `cosmetics.wings`, `cosmetics.auras`, `cosmetics.shields`, `cosmetics.hats`, `cosmetics.backs` | That cosmetic slot (own and other players') | `cosmetics` |
| `discord.server` | The server name in the Discord / launcher status ("Playing on …"), and in saved clips | `core`, `social` |
| `clips` | Clips: no recording at all. The replay buffer stops at once and what it held is thrown away; the save key shows "Clips are off on this server" | `clips` |
| `clips.auto` | Clips: no automatic clips (kill, multi-kill, clutch, death). Saving with the key still works | `clips` |
| `nametags.*` | Every key below | `nametags` |
| `nametags.seal` | The Vibey seal before player names | `nametags` |
| `nametags.ranks` | Rank and badge icons before player names (nametag, tab list, chat) | `nametags` |
| `nametags.styles` | Coloured / animated name styles (nametag, tab list, chat) | `nametags` |
| `waypoints.minimap` | The minimap (terrain and own waypoints; it never shows players or mobs). Waypoints, beacons and the world map stay | `waypoints` |
| `info.*` | Every key below | `info` |
| `info.light` | Light level of the looked-at block | `info` |
| `info.chunks` | Chunk border key (vanilla F3+G is not touched) | `info` |
| `info.card` | Block/mob info card and cards pinned into the world | `info` |
| `info.food` | Food gain on the hunger bar | `info` |
| `info.saturation` | Saturation on the hunger bar (off by default) | `info` |
| `info.toolwarn` | Tool break warning | `info` |
| `info.tnt` | TNT fuse timer | `info` |
| `inventory.tweaks` | Quick moves in inventories (Shift-drag and mouse-wheel moves; off by default in the client too) | `inventory` |
| `inventory.presets` | Hotbar presets (switch key, preset bar) | `inventory` |
| `visuals.helditem` | Moved / resized held items and the hidden empty hand (Visuals → Held item); items show in the vanilla pose | `visuals.items` |
| `visuals.*` | Every key below | `visuals` |
| `visuals.fire` | Fire overlay opacity below 100 % (the overlay looks vanilla; its height is `lowfire`) | `visuals` |
| `visuals.pumpkin` | See-through carved pumpkin mask | `visuals` |
| `visuals.totem` | Smaller totem animation (bigger stays allowed) | `visuals` |
| `visuals.particles` | Hidden, fewer or smaller particles per type and hidden own potion particles (tint, more and bigger stay) | `visuals` |
| `companion.show` | Companions: the own companion and other players' companions are hidden (e.g. minigames). They come back when the player leaves | `companion` |

The authoritative list for a given client is what it announces in `vibey:hello` (the plugin logs it, see below);
the keys come from `ServerRules.supports(...)` calls in the modules. The current Vibey Client declares `zoom`,
`togglesprint`, `togglesneak`, `crosshair`, `blockoutline`, `hitcolor`, `lowfire`, `lowshield`, `hud.togglestate`
(PvP module), `clips`, `clips.auto` (Clips module), `nametags.seal`, `nametags.ranks`, `nametags.styles` (Names and ranks), `waypoints.minimap` (Waypoints), `info.light`, `info.chunks`, `info.card`, `info.food`, `info.saturation`, `info.toolwarn`, `info.tnt` (Info module), `camera.freelook`, `freelook`, `camera.orbit` (Camera module), `cinematic.afk` (Cinematic), `inventory.tweaks`, `inventory.presets` (Inventory), `visuals.helditem`, `visuals.fire`, `visuals.pumpkin`, `visuals.totem`,
`visuals.particles` (Visuals), `companion.show` (Companion), `emotes`, `cosmetics.*` slots, `discord.server` and every `hud.*` key
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
  - hud.*
allow: [hud.fps]
message: "Zoom, freelook and HUD is restricted on this server."
worlds:              # optional, by world folder name; overrides "disable", "allow" and "message" independently in that world
  world_nether:
    disable: [zoom, hud.*]
log-hello: true      # log who announced Vibey Client and which keys their client supports
```

4. `/vibeyrules reload` (permission `vibeyrules.admin`, operators by default) applies the file to everyone online;
   `/vibeyrules status` shows the rules and which online players announced Vibey Client.
5. Staff that should keep everything: give them `vibeyrules.bypass` (nobody has it by default, not even operators).

The log then shows lines like
`[VibeyRules] Alex announced vibey:hello: vibey-client 0.3.0, supports [zoom, freelook, …] (does not know: [waypoints.minimap])`.

## Paper-Plugin-API

Lade `VibeyRules-<version>.jar` als `compileOnly`-Abhängigkeit und ergänze in deiner `plugin.yml` die Zeile
`depend: [VibeyRules]`. Die API ist als Bukkit-Service registriert. Rufe sie auf dem Serverthread für einen
verbundenen Spieler auf, etwa in `PlayerJoinEvent` oder wenn ein Minispiel startet.

```java
import dev.henny.vibeyclient.net.ServerPresentation;
import dev.henny.vibeyrules.VibeyServerApi;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;

void enterMatch(Player viewer, Player teammate) {
    VibeyServerApi api = Bukkit.getServicesManager().load(VibeyServerApi.class);
    if (api == null) return;

    var spawn = new ServerPresentation.Waypoint(
            "spawn", "Treffpunkt", viewer.getWorld().getKey().toString(), 120, 70, -30, 0x33BBFF);
    var blue = new ServerPresentation.Team("blue", "Blau", 0x33BBFF, List.of(viewer.getUniqueId(), teammate.getUniqueId()));
    api.setPresentation(viewer, new ServerPresentation(List.of(spawn), List.of(blue), Map.of()));
    api.setRules(viewer, List.of("zoom", "hud.*"), List.of("hud.fps"), "FPS bleiben sichtbar.");
}
```

- `setPresentation` ersetzt die Anzeige für den angegebenen Betrachter. Server-Wegpunkte sind temporäre HUD-Marker in der passenden Dimension. Das Modul **Serverinfos** lässt sich im Client ausschalten. Farben gelten für sichtbare Nametags in der Spielwelt. Teams liefern eine gemeinsame Farbe, individuelle UUID-Farben haben Vorrang.
- `setRules` ersetzt die konfigurierten Regeln für diesen Spieler bis zum Disconnect oder bis `resetRules(viewer)` aufgerufen wird. `allow` gewinnt bei Überschneidungen mit `disable`, auch bei Wildcards. Eine Freigabe lässt die lokale Moduleinstellung gelten. Plugin-Konfiguration `enabled: false` und die Berechtigung `vibeyrules.bypass` haben Vorrang vor dem Regel-Override.
- Am Ende eines Minispiels löscht `setPresentation(viewer, ServerPresentation.EMPTY)` alle Serveranzeigen. `resetRules(viewer)` wendet die Datei für die aktuelle Welt wieder an. Ein Dimensionswechsel allein behält den Zustand bei. Beim Disconnect leert ihn der Client.
- Das Plugin hält den aktuellen Stand bis zur Kanalregistrierung bereit. Für eigene Server-Integrationen ist auch das direkte Protokoll unten dokumentiert. Die [Modulmatrix](MODULE-MATRIX.md) zeigt, welche Clients das Anzeigemodul mitbringen.

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

## Recognising Vibey Client (client brand)

Vibey Client reports itself in the vanilla `minecraft:brand` plugin message that every client sends while joining
(configuration phase). The brand is `vibey`, a comma and the brand of the mod loader – the same list format Fabric
Loader uses when something else already set a brand:

| Loader | Brand sent | With "Show Vibey to servers" off |
|---|---|---|
| Fabric | `vibey,fabric` | `fabric` |
| NeoForge | `vibey,neoforge` | `neoforge` |

- Match with "starts with `vibey`" (`brand.startsWith("vibey")`, ClientBrand pattern `vibey*`), like the build
  hash in `lunarclient:<hash>`. Checks for `contains("fabric")` / `contains("neoforge")` keep working.
- Only lowercase letters and a comma, at most a few dozen characters – nothing for anti-cheats to trip over.
- Players can switch it off (Vibey menu → General → Privacy → *Show Vibey to servers*); the client then sends the
  loader's own brand and is indistinguishable from any other Fabric/NeoForge client. The change applies from the next join.
- Like every brand, it is self-reported and easy to fake: use it for statistics, welcome messages or to send the
  `vibey:*` packets below, never as proof. The reliable signal that Vibey Client is listening is the registration of
  `vibey:rules` / the `vibey:hello` message described in the protocol section.
- Paper/Spigot: `player.getClientBrandName()`; Velocity: `player.getClientBrand()`; Fabric/NeoForge servers: the
  `BrandPayload` in the configuration phase.

> **Deutsch, kurz:** Vibey Client meldet sich über den Vanilla-Kanal `minecraft:brand` als `vibey,fabric` bzw.
> `vibey,neoforge` (Format wie bei Fabric Loader: Liste mit Komma). Erkennen mit „beginnt mit `vibey`“. Spieler
> können das unter Allgemein → Datenschutz → „Vibey gegenüber Servern zeigen“ abschalten; dann wird nur `fabric` /
> `neoforge` gesendet. Der Brand ist selbst gemeldet und kein Beweis – kein Anti-Cheat-Merkmal.

## Protocol (for other server software)

Each plugin-message body is the UTF-8 bytes of **one JSON object** – no length prefix, no
other framing – so `player.sendPluginMessage(plugin, "vibey:rules", json.getBytes(UTF_8))` is all it takes.

**`vibey:rules`** (server → client), max 16 KiB:

```json
{ "schema": 1, "disable": ["zoom", "hud.*"], "message": "No zoom on this server." }
```

- `schema` must be `1` (other values are ignored by this client). `disable`: list of keys, max 256, each max 64
  characters. An optional `allow` list has the same format and overrides matching blocks in `disable`. Older clients ignore `allow`. Both lists use `[a-z0-9_-]` segments separated by dots, optionally ending in `.*`, or `*` alone; invalid entries are
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

**`vibey:presentation`** (Server → Client, höchstens 16 KiB):

```json
{
  "schema": 1,
  "waypoints": [
    {"id":"spawn","label":"Treffpunkt","dimension":"minecraft:overworld","x":120,"y":70,"z":-30,"color":3390463}
  ],
  "teams": [
    {"id":"blue","label":"Blau","color":3390463,"players":["45dd6559-7f1c-4d5a-9b29-3bb1bd8a4f36"]}
  ],
  "nametagColors": {"45dd6559-7f1c-4d5a-9b29-3bb1bd8a4f36":16777045}
}
```

Sende erst nach Registrierung von `vibey:presentation`. Jedes gültige Paket ersetzt den kompletten Stand.
Fehlende Abschnitte sind leer. `{"schema":1}` löscht alle Serveranzeigen. Ungültige Pakete lassen den vorigen
Stand bestehen. Der Client verarbeitet höchstens 64 Wegpunkte, 32 Teams mit insgesamt höchstens 256
eindeutigen Mitgliedern und 256 individuellen Farben. IDs innerhalb einer Liste sind eindeutig und verwenden
1–48 Zeichen aus `[a-z0-9_.-]`. Labels haben 1–64 UTF-16-Zeichen und enthalten weder Minecraft-Farbcodes
noch Steuer- oder unsichtbare Formatierungszeichen. Dimensionen sind namespaced IDs (maximal 128 Zeichen).
Koordinaten sind ganzzahlig, horizontal innerhalb ±30.000.000 und vertikal innerhalb ±4.096. Farben sind
RGB-Ganzzahlen von 0 bis 16.777.215. Teamzuordnungen gelten nur für die Darstellung in Vibey.
Ein Server mit Proxy sendet den neuen Stand nach jedem Backend-Wechsel erneut.

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
