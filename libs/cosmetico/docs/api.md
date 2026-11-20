# cosmetico API (for mod developers)

cosmetico draws `.vibey` cosmetics (capes, wings, auras, pets, shields, hats, back items) and emotes on players. Your mod
registers items, says who wears what, and cosmetico does the rest: render hooks, animation, particles, sounds, previews
and thumbnails. The public API has no Minecraft types except the small `mc` draw helpers. Client side only; every call
on the client thread unless noted.

The bundle format is described in [FORMAT.md](../../../docs/FORMAT.md). How the engine works inside:
[core.md](core.md) (pure part) and [backend.md](backend.md) (Minecraft part).

## 30 seconds

```java
// 1. optional: your own host (language, content version, log, thumbnails, what "equip"/"play" mean)
Cosmetico.init(new MyHost());

// 2. items: an index.json + .vibey files anywhere on your classpath (see "Items")
Engine e = Cosmetico.engine();
Cosmetico.host().workers().execute(() -> {
    Map<String, Item> items = e.indexLoader().read(ResourceReader.classpath(MyMod.class), "/assets/mymod/cosmetics/", ItemSource.BUILTIN);
    e.main.post(() -> e.items.putAll(ItemSource.BUILTIN, items, true));
});
McThumbnails.anchor(MyMod.class);

// 3. who wears what (any player UUID, also your own)
Cosmetico.loadouts().setIds(playerUuid, Map.of("cape", "mymod:sky_cape", "pet", "mymod:fox"));

// 4. emotes
Cosmetico.emotes().play(playerUuid, "mymod:wave");
```

That's all; the player renderer hooks pick it up on the next frame.

## Setup

- Fabric: depend on `cosmetico` (`"depends": {"cosmetico": ">=0.1.0"}`), which needs `r3` and `vui`. NeoForge: a
  `[[dependencies.<your mod>]]` entry with `modId = "cosmetico"`. Coordinates and jar-in-jar: [libs/README.md](../../README.md).
- cosmetico starts by itself. Without `Cosmetico.init` it uses a default host (game language, content version `0.3.1`,
  logger `cosmetico`). Call `init` once, early (client init); calling it later is fine too, it replaces the default.
  Only one host is active: if two mods call `init`, the last one wins. Vibey Client is such a host.

## Host (`dev.henny.cosmetico.host.CosmeticoHost`)

| Method | Default | Meaning |
|---|---|---|
| `language()` | – | `"en"`, `"de"`, … for item names. |
| `contentVersion()` | – | Compared with a bundle's `minClientVersion`; newer bundles become greyed-out stubs. |
| `log()` | `Log.SYSTEM` | Engine log. |
| `workers()` | 2 daemon threads | Parsing, sound decoding. Never the host's own task pool. |
| `effects()` | `FxSink.NONE` = vanilla | Particles, sounds and sprite particles of animation events. |
| `localActions()` | nothing | What "equip" and "play" mean for the local player (Blockbench live-link pushes). |
| `thumbnails()` | none | Downloads thumbnails by URL (items with an `http(s)` thumbnail). |

## Items (`Cosmetico.items()`, `ItemRegistry`)

- An item is one parsed bundle plus metadata (`Item`: id, kind, slot, name, rarity, source, sort, thumbnail …).
- Sources have a priority: `BUILTIN` (0) < `REMOTE` (100, a backend catalog) < `DEV` (1000, live link). For the same
  id the highest wins. Make your own with `new ItemSource("mymod", 50)`.
- `putAll(source, items, replace)` swaps a whole source (replaced bundles free their GPU textures and sounds, playing
  emotes restart with the new bundle). `put`/`remove` change one item. Reads (`all`, `get`, `bySlot`, `emotes`) are
  lock-free from any thread; `listen(Runnable)` fires after every change.
- `Bundles.parse(bytes, Cosmetico.versionGate())` parses and validates a bundle yourself; `IndexLoader` reads an
  `index.json` (`{"items":[{"file":"x.vibey","thumbnail":"thumbnails/x.png","sort":10}]}`).

## Loadouts (`Cosmetico.loadouts()`)

- `set(uuid, Map<Slot, String>)` / `setIds(uuid, Map<String, String>)` replace the base layer of a player.
- `override(uuid, slot, itemId)` / `clearOverrides(uuid)`: a layer on top that `set` never touches (try-on, tests,
  self-tests). `null` shows the slot empty.
- Players without a loadout are not touched at all.

## Emotes (`Cosmetico.emotes()`)

- `play(uuid, id)`, `stop(uuid)` (fades out), `playing(uuid)`.
- `listen(EmoteListener)`: `started`, `stopped`, `ended` for every avatar (use the UUID to filter).
- The local player: `LocalEmotes.play(id)` / `stop()` (in `mc.tick`) also switch to third person (if the emote asks for
  it and `config().thirdPersonEmotes`) and stop when the player moves (`stopOnMove`).

## Config (`Cosmetico.config()`)

Plain mutable switches your settings write: `cosmetics`, `showOwn`, `showOthers`, `emotes`, `showOthersEmotes`,
`emotesServerOff`, `hideWithArmor`, `thirdPersonEmotes`, `emoteVolume`, `slotShown[]`, `slotServerOff[]`, `self`.

## GUI helpers (`mc`)

```java
PreviewView view = McPreview.create();       // keep one per screen
view.setYaw(30f); view.playEmote("mymod:wave"); view.override(Slot.CAPE, "mymod:sky_cape");
McPreview.draw(graphics, view, x, y, w, h, partialTick);       // every frame
McThumbnails.draw(graphics, item, x, y, 32);                    // false until loaded: draw a placeholder
```

`graphics` is `GuiGraphics` (26.1+: `GuiGraphicsExtractor`). Drag-to-rotate: forward mouse events to
`view.mouse(MouseAction.PRESS|DRAG|RELEASE, x, y)`.

## Dev items (`Cosmetico.devItems()`)

`push(bytes or json, PushAction.EQUIP|PLAY|NONE, thumbnailPng)` registers a bundle under `ItemSource.DEV` (hot
replace keeps it equipped), `remove(id)`, `ids()`. Used by Vibey Client's Blockbench live link.

## Testing your items

`Cosmetico.engine().clock.pin(seconds)` freezes animation time (`NaN` releases it) for reproducible screenshots.
