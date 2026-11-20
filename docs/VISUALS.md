# Visuals module (`visuals`)

Held item models, swing, enchantment glint, item sizes, item physics for dropped items and a particle amount
control (features #26, #27 of [FEATURES.md](FEATURES.md); item models are part `visuals.items`).
**Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `visuals: ['fabric-1.21.11']`, registered by reflection in
`BuiltinModules`). Code: `src/main/java/dev/henny/vibeyclient/module/visuals`.

> See also: [MODULES.md](MODULES.md) · [CAMERA.md](CAMERA.md)

Everything is purely visual and client side: entity positions, hitboxes, pickup and the particles' gameplay
meaning are untouched. Particles have no server rule (hiding particles is what vanilla's *Particles: Minimal*
does already, and the important ones stay unless the player turns that off too). Held item poses have one:
`visuals.helditem` (see below).

## Item physics

Setting `itemPhysics` (on, section `items`).

- Dropped items spin over their own axis while they fly (faster when they move faster, a quarter as fast in water or
  lava), then fall over and **lie flat** on the ground; blocks come to rest on one of their sides. No bobbing, no
  endless spinning on the ground.
- Pure core: `core/ItemTumble` (per item: tumble angle + yaw from the entity id, `step(dt, grounded, inFluid, speed,
  flat)`; on the ground it eases to the nearest rest angle – multiples of π for flat items, π/2 for blocks – with a
  14/s rate; dt clamped to 0.1 s) and `core/ItemLift` (how high the model must be lifted so its rotated box touches
  the ground: `0.5 × (|cos r| × height + |sin r| × depth) + 0.01`, the depth of flat stacks includes vanilla's
  1.5 × spacing per extra item).
- Minecraft side: `mixin/ItemEntityMixin` gives each `ItemEntity` an `ItemMotion` (tumble + last frame time),
  `ItemEntityRendererMixin` updates it at the end of `extractRenderState` (`items/ItemPhysics.extract`, stored in the
  render state via `ItemEntityRenderStateMixin`) and wraps the two transforms of `submit`: vanilla's bob `translate`
  becomes the lift, vanilla's spin `mulPose` becomes yaw + tumble around the model's centre. The rest of vanilla's
  drawing (stack copies, shadow, outline, light) stays as is. Off → vanilla's own calls run unchanged.

## Particles

Settings (section `particles`): `particles` 0–300 % (100, steps of 25), `keepImportant` (on), `ambientOff` (off).

- `core/ParticleTable` sorts every vanilla particle id into a category: **important** (crit, enchanted hit, damage
  indicator, sweep, totem, explosions, sonic boom, flash, elder guardian, block marker, vibration, shriek, omens,
  trial spawner detection, gusts), **ambient** (ash, spores, mycelium, underwater, portal, enchant, leaves, fireflies,
  campfire smoke, bubbles, flames/smoke of torches, lava pops, sculk…), **weather** (rain, splash, snowflake, all
  drips), **block** (block/item crumbs, dust, scrape, wax), **effect** (potion swirls, witch, dragon breath),
  everything else (and modded ids) **other**.
- `core/ParticlePlan` decides how many particles one spawn becomes: `ambientOff` → ambient and weather 0; important
  with `keepImportant` → at least 1; otherwise `core/ParticleBudget` turns the percentage into whole particles with a
  carried remainder per category (50 % = every second one, 250 % = 2, 3, 2, 3, …).
- Minecraft side: `mixin/ParticleEngineMixin` at the head of `ParticleEngine.createParticle` (every
  `level.addParticle`, packets, emitters and the `/particle` command go through it): 0 → returns null, n > 1 → n − 1
  extra copies with a small position/speed jitter (re-entry guarded). Block-break crumbs that vanilla adds directly
  (`add(TerrainParticle)`) follow the block budget (they can be hidden or thinned, not multiplied). The category is
  cached per `ParticleType` (identity map), so a spawn costs one map lookup.

## Item models (part `visuals.items`)

Ideas from other clients (NoRisk, Lunar), no code or assets taken. Part `visuals.items` in `gradle/modules.gradle`
(Fabric 1.21.11), code in `module/visuals/items/**`, mixins in `module/visuals/mixin/items/**`
(`vibeyclient.visuals.items.mixins.json`), strings in `i18n/*/visuals.items.json` (section titles in `visuals.json`),
editor documents `vui/items/editor.vui` + `menu.vui`. Everything defaults to vanilla.

### Held item (section `held`)

Per hand (main / off hand, in *right arm* space – mirrored for the left arm): sideways, height, depth (−1…1),
tilt / turn / roll (±180°), size (10–250 %). Stored as `mainX`, `mainY`, `mainZ`, `mainRotX`, `mainRotY`, `mainRotZ`,
`mainScale` and the same with `off…` in `config/vibeyclient/visuals.json`. The page shows one hand's sliders at a time
(*Hand* switch, not stored). *Preset* (not stored, applies to both hands): Vanilla, Small, Low, Centered; anything
else reads *Custom*.

- Rendering: `HeldItemMixin` adds the offset at the end of `ItemInHandRenderer.applyItemArmTransform` (so the vanilla
  swing still pivots normally) and the rotation + scale at the head of `renderItem` for the first-person display
  contexts, around a pivot near the item's centre (`HandProjection.PIVOT_*`). Maps and the empty arm are not moved.
- Server rule **`visuals.helditem`**: a smaller / lower / hidden item or arm means less on screen in PvP, so a server
  can switch it off. Poses fall back to vanilla, *Hide empty hand* is ignored, the editor won't open, the rows show
  *Disabled by this server*.

### Edit mode (direct manipulation)

*Edit in game* (the page's primary button, or the unbound *Edit key* in the controls) opens `ItemEditScreen` over the
world in first person (third person is switched to first person). It does not pause. One bar on top (Main hand /
Off hand, Cancel, **Save**) and one hint line; everything else happens on the item:

| Gesture | Effect |
|---|---|
| hover | ring around the item + preset name and size |
| left or middle drag | move the item (follows the pointer at its depth; the hand nearest the pointer is picked) |
| Ctrl + drag / Alt + drag | turn and tilt / roll |
| wheel / Shift + wheel | size (×1.08 per step) / depth |
| right click | menu: Vanilla, Small, Low, Centered, *Copy to other hand*, *Undo changes* |
| Tab / R / Space | other hand / undo changes / swing once (preview) |
| Enter / Esc | save / cancel (Esc closes the menu first) |

Edits live in a pure `core/EditSession` (working copy + start snapshot); the render hooks read the session while the
editor is open, so nothing is written to the settings until Enter.

### Swing (section `swing`)

`swingSpeed` 25–300 % (the local player's swing length: `LivingEntity.getCurrentSwingDuration` scaled, at least 1
tick; other players and the server are untouched), `swingStyle` *Vanilla* / *Classic* (no dip after a hit – the
attack-cooldown dip is skipped, like before 1.9) / *Smooth* (smaller arc) / *Punch* (a short jab forward),
`noEquipBob` (the item never dips on switch or use and the new item shows at once), `hideEmptyHand` (no arm when the
main hand is empty).

### Item sizes (section `items`)

`dropScale` 50–300 % for dropped items: with item physics the model is scaled around its centre and the lift is
scaled too, without it the vanilla bob height is corrected so the item still floats at the same height above the
ground (`core/DropScale`). `frameScale` 50–200 % for items in item frames (maps are not scaled).

### Glint (section `glint`)

`glintStrength` 0–200 % (0 = off), `glintSpeed` 0–400 % (multiplies vanilla's *Glint Speed* accessibility option),
`glintTint` + `glintColor`. The glint shader adds `texture² × ColorModulator²`, so `core/GlintTint` turns a colour
into a per-channel modulator from the measured energy of vanilla's purple glint texture (R 0.128, G 0.030, B 0.335):
any colour (even green, which the texture barely has) comes out with the brightness of vanilla's blue channel.
`GlintColorMixin` swaps the `ColorModulator` that `RenderType.draw` writes for the four glint render types (items,
entities, armour); `GlintSpeedMixin` scales the speed read in `TextureTransform.setupGlintTexturing`.

## Code map

| Package | Job |
|---|---|
| `visuals` | `VisualsModule` (settings, `itemPhysicsActive`, `particlesActive`) |
| `visuals.items.core` | pure, JUnit in `src/junit/java/.../module/visuals/items/core`: `HandPose`, `HandPreset`, `HandProjection`, `HandPick`, `PoseGestures`, `EditSession`, `SwingStyle`, `SwingShape`, `SwingTiming`, `GlintTint`, `DropScale` |
| `visuals.items.held` / `swing` / `drop` / `glint` | settings + render facades (`HeldSettings`, `HandSettings`, `HeldRender`, `SwingSettings`, `SwingRender`, `DropSettings`, `DropRender`, `GlintSettings`, `GlintRender`), `ItemRules` |
| `visuals.items.edit` | `ItemEditor` (open, active session), `ItemEditScreen`, `ItemEditController`, `EditGestures`, `EditMenu` |
| `visuals.mixin.items` | `HeldItemMixin`, `HeldSwingMixin`, `SwingDurationMixin`, `ItemFrameScaleMixin`, `GlintColorMixin`, `RenderSetupAccess`, `GlintSpeedMixin` |
| `visuals.core` | pure, JUnit in `src/junit/java/.../module/visuals/core`: `ItemTumble`, `ItemLift`, `ParticleCategory`, `ParticleTable`, `ParticleBudget`, `ParticlePlan` |
| `visuals.items` | `ItemPhysics`, `ItemMotion`, `ItemMotionHolder`, `ItemPose` |
| `visuals.particles` | `ParticleGate` (plan per spawn, extra copies, stats for tests) |
| `visuals.mixin` | `ItemEntityMixin`, `ItemEntityRenderStateMixin`, `ItemEntityRendererMixin`, `ParticleEngineMixin` (all `require = 0`) |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.visuals.*"` (11 + 14 item model tests).
- Game (item models): `ItemModelsGameTest` (+ `ItemHeldChecks`, `ItemEditorChecks`, `ItemWorldChecks`), shots in
  `build/review/items/`: page EN/DE, vanilla / Small / Low / Centered / custom pose, the editor (hover ring, grab and
  drag, wheel bigger, preset menu, saved), Enter saves and Esc cancels, swing length at 50 / 100 / 200 %, the punch
  style, glint vanilla / red / green / off, dropped items at 300 % with and without physics, item frames at 180 %.
- Game: `VisualsGameTest` (+ `VisualsItemChecks`, `VisualsParticleChecks`), shots in `build/review/visuals/`: module
  page EN/DE, 10 summoned items falling and on the ground with physics and vanilla (all settle), `/particle flame`
  ×300 at 0 / 100 / 200 % (0 / 300 / 600 created), crits at 0 % kept and not kept, `ambientOff`, frame time with ~60
  items physics on/off.

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=VisualsGameTest -Dvibeyclient.test.relay=off
```

## Frame time and counts (2026-11-20, Intel Arc, 1280 × 720, shared PC)

| Measured | Result |
|---|---|
| 60 dropped items, item physics off / on (3 alternating rounds, medians) | 2.719 ms / 2.642 ms – no measurable cost (difference is noise) |
| `/particle flame` × 300 at 0 / 100 / 200 % | 0 / 300 / 600 particles created (300 dropped at 0 %, 300 extra at 200 %) |
| `/particle crit` × 300 at 0 %, keep on / off | 300 / 0 |
| `ambientOff`, flames at 100 % | 0 |

Per spawn the gate costs one identity-map lookup and a few arithmetic operations; at 0 % it saves the whole particle.
