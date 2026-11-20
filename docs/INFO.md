# Info module (`info`)

Small "what am I looking at" helpers: light level and chunk borders, a block/mob card you can pin into the world,
food gain on the hunger bar, a tool break warning and a TNT fuse timer (features #6, #7, #8, #10, #11 of
[FEATURES.md](FEATURES.md)). **Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `info: ['fabric-1.21.11']`,
`BuiltinModules` registers it by reflection). Code: `src/main/java/dev/henny/vibeyclient/module/info`.

> See also: [MODULES.md](MODULES.md) · [HUD.md](HUD.md) (card + warning are HUD widgets) ·
> [libs/r3](../libs/r3/README.md) (world labels, pinned cards) · [SERVER-RULES.md](SERVER-RULES.md) · [PVP.md](PVP.md)

## No advantage beyond vanilla

Everything shown is something a vanilla player can see or work out without mods:

| Shown | Why it is fair |
|---|---|
| Light level of the block you look at | Vanilla F3 shows light levels. Hidden while the server sets *reduced debug info*, like F3. |
| Chunk borders | The same renderer and the same check as F3+G (blocked with reduced debug info). |
| Block name, mod, right tool | Name and mod are public; the tool follows from vanilla block tags (what mining speed tells you). |
| Crop growth | Only blocks whose age is visible in the texture (crops, stems, nether wart, cocoa, berries, pitcher). Sugar cane/cactus/sapling ages are hidden (not visible in vanilla). |
| Mob health | **Only** where vanilla shows it: the mob you ride (mount hearts) and bosses (boss bar). |
| Food gain (hunger points) | Item values are public; the gain is drawn over vanilla's own hunger bar. |
| Saturation | Vanilla never shows it → setting `saturation` is **off by default** and has its own rule `info.saturation`. |
| Tool durability warning | Vanilla shows the durability bar on the item. |
| TNT seconds left | Vanilla shows the blinking fuse; the fuse is synced entity data. Own rule `info.tnt`. |

Not offered: container contents, hidden block states (note pitch, redstone power), mob health of other mobs, spawner
timers, anything through walls.

## Features

| Feature | Settings (section) | How |
|---|---|---|
| Light level | `light`, `lightTorch` ("Only with torch", default on) · key `lightKey` (unbound) toggles "always" | `light/LightLabel`: one r3 `Text3D` ("13") above the face you look at (the adjacent block's block light). Top faces of sturdy blocks are coloured by `core/SpawnLight`: teal = no spawns, yellow = spawns at night, red = spawns now (uses the dimension's `monsterSpawnBlockLightLimit` and the max of `monsterSpawnLightTest`). |
| Chunk borders | `chunks` · key `chunkKey` (**O**) | `light/ChunkBorders` toggles vanilla's `DebugScreenEntries.CHUNK_BORDERS` (exactly F3+G, same action bar message, same reduced-debug check). A server rule turns them off only if we turned them on. |
| Block / mob card | `card`, `cardMobs`, `cardTool`, `cardMod` (section `card`) | HUD widget `info.card` (`card/InfoCardWidget`, default top centre): item icon, name, mod (muted), status row = right tool icon + ✔/✖, growth % (sprout icon) or health (heart). Data per tick from vanilla's `hitResult` (no own raycast): `card/BlockCards`, `card/EntityCards`, `card/HarvestReader` → `core/HarvestRules`. |
| Pin the card | key `pinKey` (**I**, hold 0.4 s) | `pin/*`: holding the key while the card shows fills a 1 px progress line under the card, then the card becomes an r3 world panel (`assets/vibeyclient/vui/info/card.vui`, `pin/CardController`) above the block (facing you) or above the mob's head (follows it). It refreshes 4×/s (crop growth, tool in hand). Interaction model for free: **middle-drag** moves it (snaps to faces), **wheel** resizes, border = resize handle, **right click** or a **tap** of the pin key while looking at it removes it. Max 6; the oldest goes. While the crosshair is on a pinned card the HUD card hides. Pinned cards are not saved (gone when you leave the world). |
| Food gain | `food`, `saturation` (off) | `food/FoodState` picks the hovered slot of any container screen (`mixin/AbstractContainerScreenAccessor`) or, without a screen, the food in your hand; `core/FoodMath` = vanilla `FoodData.eat` (food clamp 20, saturation ≤ food). `food/HungerBarOverlay` draws the gained shanks with vanilla's sprites (`hud/food_full`/`_half`, the hunger variants for risky food from `core/FoodTable`) pulsing over the bar, only where vanilla draws it (survival/adventure, no mount hearts). Saturation: a 1 px gold line under each shank (now solid, gain pulsing). Cake has no food component: `FoodTable` knows its slice. |
| Tool warning | `toolWarn`, `toolWarnAt` (10 %) | HUD widget `info.toolwarn` (default under the crosshair): tool icon + uses left, warn colour, flashing (`core/Flash`: 1 s pulse when low, 0.4 s when critical). `core/Durability`: warn at `max(3, ceil(max × %))` uses left, critical at a third of that. Main hand first, then off hand. |
| TNT timer | `tnt` | `tnt/TntFinder` casts one ray per tick (24 blocks, stopped by blocks) against lit TNT boxes; a found TNT gets an r3 label above it (`EntityAnchor`) showing `core/FuseMath` seconds ("2.4", green → yellow → red). It stays until the TNT explodes or is 48 blocks away (max 8). |

The button *Edit HUD* opens the HUD editor (move/resize the card and the warning there).
The built-in HUD templates (`module/hud/BuiltinTemplates`: PvP, Minimal, Building, Streamer) place `info.card` and
`info.toolwarn`; a template hides every widget it does not list, so new info widgets must be added there too.

## Cost per frame

- One tick job (`InfoTick`, 20/s): reads `mc.hitResult`, builds the card record, one TNT ray, light lookups for one
  position. Nothing raycasts per frame.
- Per frame: the two HUD widgets (`paint` allocates nothing; strings are built in `update` at 20 Hz / 10 Hz), the
  hunger overlay (≤ 10 sprite blits, ≤ 20 rects) and r3's own drawing of at most 1 + 8 labels + 6 panels.

## Server rules

`info.light`, `info.chunks`, `info.card` (HUD card + pinning, removes pinned cards), `info.food`,
`info.saturation`, `info.toolwarn`, `info.tnt`; `info.*` = all. Every setting row carries its key
(*Disabled by this server* badge), the HUD widgets use `.rules(key)`.

## Code map

| Package | Job |
|---|---|
| `info` | `InfoModule` (settings, rule helpers `*Active()`), `InfoRules` (keys), `InfoTick` (the per-tick order) |
| `info.core` | pure Java, JUnit in `src/junit/java/.../module/info/core`: `FoodMath`/`FoodGain`/`FoodTable`/`FoodValue`, `FuseMath`, `HarvestRules`/`HarvestInfo`/`ToolKind`/`ToolTier`/`HarvestVerdict`, `Growth`, `SpawnLight`, `Durability`/`WarnLevel`/`Flash`, `HealthRule`, `ModLabel`, `HoldGesture`, `Colors` |
| `info.look` | `LookTarget`: block/face/state or entity of this tick |
| `info.card` | card data, readers, HUD widget, icons (`InfoIcons`: heart, sprout, light), tool icon items |
| `info.pin` | world cards: `PinControl` (hold/tap), `PinnedCards` (list, r3 events), `PinnedCard`, `PinPlacement`, `CardController` |
| `info.light` | `LightLabel`, `LightProbe`, `LightSources`, `ChunkBorders` |
| `info.world` | `WorldLabel`: one r3 `Text3D` object, re-created after a world/dimension change |
| `info.tnt` | `TntFinder`, `TntTimers` |
| `info.tool` | `ToolState`, `ToolWarnWidget`, `ToolWarnHud` |
| `info.food` | `FoodItem`, `FoodState`, `HungerBarOverlay` |
| `info.mixin` | `AbstractContainerScreenAccessor` (`hoveredSlot`) |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.info.*"` (25 tests, < 1 s).
- Game: `InfoGameTest` (+ `InfoTestKit`, `InfoWorldChecks`, `InfoCardChecks`, `InfoHudChecks`), screenshots to
  `build/review/info/NN_*.png`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=InfoGameTest -Dvibeyclient.test.relay=off
```

It checks: module page en/de; light label value + colour next to a torch and on a dark open floor, hidden without a
torch, shown with `lightTorch` off, hidden with reduced debug info; chunk key on/off; TNT timer counts down and
stays after looking away, goes with the TNT; ore card with iron/stone pickaxe/hand (✔/too weak/wrong tool), wheat
43 % and 100 %, cow card without health; pin (short press does not pin, hold pins), HUD card hides on the pinned
card, middle-drag moves it, wheel grows it, tap removes it, mob pin; tool warning low/critical; steak gain on 8 food
(16), saturation line, bread hovered in the inventory (+5); `info.*` server rule hides everything and the page badge.

Porting later: `DebugScreenEntries` exists since 1.21.9 (older: `debugRenderer.switchRenderChunkborder()`),
`Gui` sprite blits and `renderItem` differ on 26.x (`GuiGraphicsExtractor`), `Identifier` was `ResourceLocation`
before 1.21.11.
