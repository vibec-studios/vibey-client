# Companion (module `companion`)

A living companion next to the player, from **companion packs** (like resource packs). The engine is the lib
[`companio`](../libs/companio/README.md); this module is its Vibey Client side: settings page, keybinds, server rule
and sharing with other Vibey players. Fabric 1.21.11 only for now (`gradle/modules.gradle`).

Built-in companions: **Seal** (`seal:seal`, default), Penguin, Robot. Pack format for creators:
[libs/companio/docs/pack-format.md](../libs/companio/docs/pack-format.md).

## For players

| Do | What happens |
|---|---|
| look at it + left click | pet it (it claps, barks, hearts) |
| hold middle mouse on it | pick it up and carry it; release puts it down there |
| wheel on it | bigger / smaller (if the pack allows) |
| right click | menu: Follow, Sit, Stay, Tricks, Name |
| right click with a named name tag | rename it (the tag is not used up) |

The seal follows you (waddles, runs on its belly when you sprint), slides on snow and ice, swims and paddles in water,
shakes off the water, puts on its yellow rain hat in the rain, shivers and wears a scarf in snowy biomes,
sleeps next to you at night (and when you sleep or go AFK), begs when you hold fish, runs from lava, fire and monsters
you can see, worries when you get hurt, greets you in the morning, gets bored, zooms around when happy and does tricks
(beach ball, roll, dance, wave, hop on your shoulder).

## Settings page

| Setting | Notes |
|---|---|
| *Packs* (button) | the pack screen: available / active packs, icon, errors per pack, open folder, reload, pick a companion |
| *Call back* | the companion pops up next to you |
| Companion | one of the active packs' companions, or none (server rule `companion.show` dims it) |
| Look | texture variant (when the companion has more than one) |
| Name, Size | also changed by name tag / wheel; kept in sync both ways |
| Mode | Follow / Stay / Sit |
| Sounds, Speech bubbles | volume of companion sounds, bubbles on/off |
| Others' companions, Share mine | sharing (below) |
| Keybinds | *Companion menu*, *Companion packs* (unbound by default) |

The module writes its values into `config/companio.json` through `CompanioMc` (one source of truth: the lib's config).

## Code

| File | Job |
|---|---|
| `module/companion/CompanionModule.java` | settings, keybinds, lifecycle, server rule |
| `CompanionChoices.java` | dynamic choices (companions of active packs, variants, modes) |
| `SettingsBridge.java` | settings ↔ companio config, both ways |
| `CompanionSync.java` | `VibeyNet.channel("companion:state")` ↔ `CompanioMc.sync/receive` |

## Server rule

`companion.show` (docs/SERVER-RULES.md): when a server disables it, the own companion and everybody else's are hidden
until the player leaves. Disabling the module hides them too.

## Sharing (multiplayer)

Other players with Vibey Client see your companion when **the server runs Vibey's relay** (Vibey Client on
Fabric/NeoForge servers, or the VibeyRules plugin) and they have the same pack (same pack id and content hash;
built-in packs always match). Sent: pack id + hash, companion id, variant, name, size, position, yaw, dimension, base
and layer animation names, shown props, speech bubble text – a small JSON message on the module channel
`companion:state`, at most 4 per second, a heartbeat every 3 s. The brain runs only on the owner's client.

Gap: there is no sync on servers without the relay (the backend is not involved; a backend presence field could carry
it later), and packs are never transferred – a player without the pack simply does not see that companion.

## Tests

- JUnit (lib core): `./gradlew :companio:test` – packs, validation messages, expressions, brain, physics, pathfinding,
  animation mixing, sync codec, the built-in packs load without errors.
- Game test: `CompanionGameTest` (`-Dvibeyclient.test.only=CompanionGameTest -Dvibeyclient.test.relay=off`), shots in
  `build/review/companion/`.
