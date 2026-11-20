# cosmetico

Cosmetics engine for Minecraft mods: `.vibey` bundles (capes, wings, auras, pets, shields, hats, backpacks and emotes
made in Blockbench), animation states, emotes, particles and sounds, previews and thumbnails. Extracted from Vibey
Client; usable without it.

Status: core and Minecraft backend are done; Vibey Client draws its cosmetics through cosmetico (fabric 1.21.11 tested in
game, the other targets compile). Without a host mod cosmetico runs with a default host.

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

- [docs/api.md](docs/api.md): the API for mod developers.
- [docs/backend.md](docs/backend.md): the Minecraft part (render passes, mixins, tick, versions).
- [docs/core.md](docs/core.md): packages, host interfaces, how a frame flows.
- [docs/testing.md](docs/testing.md): JUnit, golden parity files, gametests and screenshot parity.
- [docs/extraction-plan.md](docs/extraction-plan.md): how the engine moves out of Vibey Client, step by step.
- The bundle format: [docs/FORMAT.md](../../docs/FORMAT.md) of the client.
