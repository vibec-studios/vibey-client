# companio – living companions for Minecraft

companio puts a **living companion** next to the player: a seal that waddles after you, belly-slides on snow, swims,
naps next to you at night and claps when you pet it. Everything a companion is and does comes from a **companion
pack** – a folder or `.zip` like a resource pack, with model, textures, animations, props, sounds, particles, texts and
**behavior as data** (no code, safe from strangers). Built-in packs: seal, penguin, robot (a little robot that scans the ores you look at).

- Mod id `companio`, package `dev.henny.companio`, Apache-2.0. Needs `cosmetico` (models, animation, rendering, sounds),
  `r3` (pointer interaction, speech bubbles) and `vui` (screens). Usable without Vibey Client.
- Client-side only: the companion is drawn by your game, never by the server, and never affects gameplay.
- Minecraft: Fabric 1.21.11 (other targets later).
- **Pure core** (`src/core/java`: pack format, validation, expressions, behavior brain, physics, pathfinding,
  animation mixing, sync codec) with JUnit tests; thin Minecraft backend (`src/main/java/dev/henny/companio/mc`).

## 30 seconds: a pack

`.minecraft/companionpacks/blob/companion.json`

```json
{ "format": "companio-pack", "formatVersion": 1, "id": "blob", "name": "Blob",
  "companions": [ {
    "id": "blob", "name": "Blob", "model": "models/blob.vibey",
    "sounds": { "boop": { "sound": "minecraft:entity.slime.squish_small" } },
    "behaviors": [
      { "id": "petted", "priority": 90, "on": "pet", "anim": "jump",
        "start": [ { "do": "sound", "name": "boop" }, { "do": "say", "text": "♥" } ] },
      { "id": "nap", "priority": 40, "when": "time.night", "move": "near", "baseAnim": "sleep" }
    ] } ] }
```

Put any Vibey pet `.vibey` (Blockbench plugin or `content/` toolkit) into `models/`, open the pack screen
(`/companio packs`), activate the pack and click the companion. Save a file – folder packs reload by themselves.

## 30 seconds: from code

```java
CompanioMc.select("seal:seal");             // pick a companion of an active pack
CompanioMc.command(Command.STAY);           // follow / stay / sit
CompanioMc.trick("ball");                   // play a trick
CompanioMc.openPacks();                     // the pack screen
CompanioMc.serverHidden(true);              // hide all companions (e.g. a server rule)
CompanioMc.sync(payload -> myNet.send(payload));         // share your companion …
myNet.onReceive((uuid, payload) -> CompanioMc.receive(uuid, payload));  // … and see others'
```

## What the player does

Look at it + left click = pet · hold middle mouse = carry it, release = put it down · wheel = size · right click =
menu (sit, follow, stay, tricks, name) · right click with a named name tag = rename. Commands: `/companio packs |
reload | debug | menu | recall | hide | summon <pack:companion> | trick <id>`.

## Docs

- [docs/pack-format.md](docs/pack-format.md) – the complete pack format with examples (make a pack from this alone)
- [docs/engine.md](docs/engine.md) – how the core works: loading, brain, movement, animation, sync
- [docs/minecraft.md](docs/minecraft.md) – the Minecraft backend: hooks, rendering, interaction, files, fairness
- Example pack sources: `content/companions/seal/` (built with `node content/companions/build.mjs`)
