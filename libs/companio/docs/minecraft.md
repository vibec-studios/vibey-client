# companio Minecraft backend

`libs/companio/src/main/java/dev/henny/companio/mc/**` – Fabric 1.21.11 only for now (`libTargets` in
`settings.gradle`). Everything else is the core ([engine.md](engine.md)).

## Packages

| Package | Job |
|---|---|
| `mc` | `CompanioMod` (id, log), `CompanioMc` (facade for other mods) |
| `mc.fabric` | `CompanioFabric`: starts the client, tick event, `/companio` command, finds built-in packs in every mod's `assets/*/companionpacks/packs.json` |
| `mc.client` | `CompanioClient` (the runtime: config, packs, own companion, puppets, sync), `OwnCompanion`, `Riding`, `SyncSender`, `ConfigStore` |
| `mc.packs` | `PackRepository` (loads on a worker thread, swaps on the client thread, watches the folder every second), `UserPacks`, `BuiltinPacks` |
| `mc.world` | `McBlockView` (collision tops, fluids, ice/snow, hazards, loaded chunks), `McSenses`, `OwnerTracker`, `NearScan` |
| `mc.render` | `CompanionRenderer` (mixin at the end of `LevelRenderer.submitEntities`), `ViewLocators` |
| `mc.fx` | `McOutput` (pack sounds and particles), `AnimEvents` (model animation events) – both through cosmetico's `VanillaFx` |
| `mc.interact` | `CompanionTarget` (an invisible r3 world object on the companion), `Interactions` (r3 events), `Bubbles` (r3 3D text) |
| `mc.screen` | vui screens: `PacksController` (`companio:packs`), `RadialController` (`companio:radial`), `Overlay` (`companio:errors`: pack errors + `/companio debug`) |
| `mc.command` | `/companio packs | reload | debug | menu | recall | hide | summon <key> | trick <id>` |

## Rendering

The companion is no entity. `CompanionRenderer` draws every `CompanionView` (own companion + puppets) like a cosmetico
pet: translate camera-relative, rotate `180 − yaw`, scale `def.scale × size`, `WorldFrame.toModelSpace`, then
cosmetico's `BoneDraw` with the `Mix` from the `Animator` (+ hidden prop bones + head look). Light from the block at the
companion. Locator positions are collected while drawing (`ViewLocators`) for particles and animation sounds.

## Interaction (r3)

`CompanionTarget` adds one r3 `WorldObject` (kind `companio:companion`, camera-facing, the size of the hit box) whose
anchor follows the companion every frame. r3 does the rest:

| r3 event | companio |
|---|---|
| `ACTION` (look + left click) | `pet` |
| `GRAB_STARTED` (hold middle mouse) | `pickUp`: physics off, base animation `held`, the object follows the view |
| `PLACED` / `MOVE_CANCELLED` | `place` at the object's position, it falls from there |
| `RESIZED` (wheel) | size × factor, clamped to the companion's `size` range, saved |
| `CONTEXT_MENU` (right click) | a named name tag in the main hand renames, else the radial menu |

Speech bubbles and the name (shown while you look at it) are r3 `Text3D` objects anchored above the head.

## Files

| File | |
|---|---|
| `.minecraft/companionpacks/` | the player's packs (folders and zips) |
| `config/companio.json` | active packs, selected companion, name, variant, size, command, volume, needs/vars per companion |

## Fairness

- Only client-known blocks and entities are read; `near.*` counts only blocks and mobs the player can see (ray from the
  eyes, at most 48 rays per scan, every 10 ticks); paths stay within 32 blocks of the companion.
- The companion never touches the world, sends no gameplay packets and can be hidden by the host
  (`CompanioMc.serverHidden`, Vibey Client's server rule `companion.show`).

## Hot reload

Folder packs are stamped (newest modification time + file count) every second on the worker thread; a change reloads
all packs. Errors appear in the pack screen and for 12 s in the top-left overlay. The running companion keeps its last
good definition until the pack loads again (`PackRegistry`).
