# cosmetico

Cosmetics engine for Minecraft mods: `.vibey` bundles (capes, wings, auras, pets, shields, hats, backpacks and emotes
made in Blockbench), animation states, emotes, particles and sounds, previews and thumbnails. Extracted from Vibey
Client; usable without it.

Status: the pure core (`src/core`) is done and tested against the old engine. The Minecraft backend comes next, so
cosmetico does not draw anything in the game yet.

## 30 seconds

```java
Cosmetico.init(new CosmeticoHost() {
    public String language() { return "en"; }
    public String contentVersion() { return "0.3.1"; }
});

Engine e = Cosmetico.engine();
Map<String, Item> items = e.indexLoader().read(ResourceReader.classpath(MyMod.class), "/assets/mymod/cosmetics/", ItemSource.BUILTIN);
Cosmetico.items().putAll(ItemSource.BUILTIN, items, true);

Cosmetico.loadouts().set(playerUuid, Map.of(Slot.HAT, "mymod:party_hat"));
Cosmetico.emotes().play(playerUuid, "mymod:wave");
```

## Docs

- [docs/core.md](docs/core.md): packages, host interfaces, how a frame flows.
- [docs/testing.md](docs/testing.md): JUnit, golden parity files.
- [docs/extraction-plan.md](docs/extraction-plan.md): how the engine moves out of Vibey Client, step by step.
- The bundle format: [docs/FORMAT.md](../../docs/FORMAT.md) of the client.
