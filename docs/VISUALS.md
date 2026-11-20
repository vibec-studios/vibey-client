# Visuals module (`visuals`)

Held item models, swing, enchantment glint, item sizes, item physics for dropped items and a particle amount
control (features #26, #27 of [FEATURES.md](FEATURES.md); item models are part `visuals.items`), plus effects and a
per-type particle editor ([below](#effects-and-particles-part-visualseffects)).
**Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `visuals: ['fabric-1.21.11']`, registered by reflection in
`BuiltinModules`). Code: `src/main/java/dev/henny/vibeyclient/module/visuals`.

> See also: [MODULES.md](MODULES.md) · [CAMERA.md](CAMERA.md)

Everything is purely visual and client side: entity positions, hitboxes, pickup and the particles' gameplay
meaning are untouched. Item physics and the global particle amount have no server rule (hiding particles is what
vanilla's *Particles: Minimal* does already). Held item poses have one (`visuals.helditem`), and per-type particle
styles and the effects that hide more than vanilla have `visuals.*` rules (see below).

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

## Effects and particles (part `visuals.effects`)

Idea from the "particles & effects" menus of other clients, own code. Part `visuals.effects` in
`gradle/modules.gradle` (`['fabric-1.21.11']`, like the module). `VisualsModule` has one line
(`effects = new EffectsPart(this)`); `effects/EffectsPart` adds its settings to the visuals settings group (section
*Effects*, labels in `i18n/*/visuals.effects.json`), declares the server rules and attaches the particle style store.
Mixins: `vibeyclient.visuals.effects.mixins.json` (package `visuals.mixin.effects`, every injector `require = 0`).

### Effects

| Setting | Range (default) | How | Server rule |
|---|---|---|---|
| `fireOpacity` | 0–100 % (100) | `ScreenEffectRendererFxMixin`: alpha of the four `setColor` calls in `renderFire` (vanilla 0.9). Fire height is the PvP module's `lowFire`/`fireHeight` (linked rows in the editor, not duplicated) | `visuals.fire` |
| `pumpkinOpacity` | 0–100 % (100) | `GuiFxMixin`: alpha of the first `renderTextureOverlay` in `renderCameraOverlays` (equippable camera overlays: the carved pumpkin) | `visuals.pumpkin` |
| `vignette` | 0–100 % (100) | `GuiFxMixin`: scales the read of `vignetteBrightness` in `renderVignette`; the red world border warning uses another branch and stays | – (vanilla has a vignette switch) |
| `totemSize` | 0–150 % (100) | `ScreenEffectRendererFxMixin`: one extra `scale` after vanilla's 0.8 in `renderItemActivationAnimation` | `visuals.totem` (only below 100 %) |
| `distortion` | 0–100 % (100) | `GameRendererFxMixin` + `GuiFxMixin`: scale `getEffectBlendFactor` (nausea) and both portal intensity fields in `renderLevel`, `tick` and `renderCameraOverlays` | – (vanilla *Distortion Effects*) |
| `hurtShake` | 0–100 % (100) | `GameRendererFxMixin`: multiplies the `damageTiltStrength` read in `bobHurt` | – (vanilla *Damage Tilt*) |
| `explosionShake` | 0–100 % (0) | new effect: `ExplosionShake` kicks `core/Shake` when an `explosion`/`explosion_emitter` particle is created near the camera (power falls off over 4 × radius, at least 8 blocks), applied at every return of `bobHurt` (≤ 3.5° roll, 1.8° pitch, fades in about a second) | – (adds, never hides) |
| `ownPotionParticles` | on | `LivingEntityFxMixin`: `@WrapWithCondition` on `addParticle` in `tickEffects`, local player only | `visuals.particles` |

Everything is vanilla when the module is off or a value is at its default. Rules use `EffectMath.fair`: while a rule
is active, a reducing value counts as 100 % (a bigger totem stays allowed).

PvP rows: block outline (colour, width, opacity, fill), hit colour (tint, strength), lower shield and low fire are the
PvP module's settings; the editor shows them in *Overlays*/*Combat* through `PvpLink` (settings looked up by key, no
import of the PvP package; *Turn on* enables the PvP module while it is off).

### Particles per type

`particles/StyleStore` keeps a `core/ParticleStyles` map (id → `ParticleStyle(amount 0–400 %, size 25–300 %, tint
RGB + strength 0–100 %, hidden)`), stored as JSON under `_data.particleStyles` in `visuals.json` (`core/StyleCodec`,
only changed fields). Each `ParticleType` gets one `StyleSlot` (identity map: category, own `ParticleBudget`, the
effective style – `fair()` while `visuals.particles` is active: amount and size at least 100 %, not hidden).

- Amount: `ParticleGate.plan` → `core/StylePlan.count`: hidden → 0; *No ambient* → 0 for ambient/weather; the global
  slider × the type's amount (max 6×). *Keep hit particles* raises only the global part to 1×, so an explicit 0 % on
  `crit` still hides crits. Default styles + 100 % global = the vanilla path (one map lookup).
- Size and colour: every created particle carries its slot (`ParticleMixin` field, set at the return of
  `createParticle` and for direct `TerrainParticle`s); `SingleQuadParticleMixin` changes the size and colour
  arguments of `QuadParticleRenderState.add` every frame, so edits apply to living particles at once.
  `core/TintMath` mixes the colour towards tint × luminance by the strength (white textures take the exact tint).
- Presets (`effects/core/Preset` + `particles/core/ParticlePresets`, `effects/core/EffectPresets`): *Vanilla*
  (everything default), *Clean* (fewer ambient/weather/block/potion particles, softer vignette, hurt shake and
  distortion), *PvP* (bigger and more crits, less noise, fire 60 %, totem 60 %, pumpkin 60 %, no own potion swirls),
  *Fancy* (more crits, flames, sparkles and totem particles, totem 120 %, blast shake 60 %). The bar shows the preset
  that matches the current values (none after own edits).

### Editor (direct manipulation)

`effects/screen/EffectsScreen` (vui `effects/editor` + `effects-tab`, `particles-tab`, `particle-detail`) opens with
the page button *Effects & particles*. It does not pause and has no background: a 192 px panel on the right over the
live world; dragging on the free area turns the view (e.g. to look at a block for the outline). Every change applies
at once.

- Tabs *Effects* / *Particles*, preset bar on top.
- Effects: the normal `setting-row`s (so server badges work) with a ▶ test button: fire overlay 3 s, pumpkin mask
  3 s, nausea ramp 3.5 s (`EffectsPreview`, cleared when the screen closes), hurt tilt (`animateHurt`), totem pop
  (+ totem particles), explosion 7 blocks left of the view (shake + sound).
- Particles: global rows (amount, keep hit particles, no ambient), search, category filter (incl. *Changed*), a
  virtual list grouped by category; each row has a live swatch (the type's sprite from `particles/<id>.json`,
  animated through its frames, tinted and sized), the amount and an eye to hide it. Click → detail: amount, size,
  colour + strength, hide, reset, *Test*. Selecting and every edit spawn 24 preview particles 3 blocks ahead
  (`ParticleBurst`; `PreviewOptions` builds options for block/item/dust/colour/spell types, vibration and trail have
  no preview).

### Code map (effects part)

| Package | Job |
|---|---|
| `visuals.effects` | `EffectsPart` (settings), `EffectRules`, `Effects` (values for the mixins), `EffectsPreview`, `ExplosionShake`, `PreviewSpot`, `PvpLink` |
| `visuals.effects.core` | pure: `EffectKeys`, `EffectMath`, `EffectPresets`, `Preset`, `PreviewCurve`, `Shake` |
| `visuals.effects.screen` | `EffectsScreen`, `EffectsController`, `EffectsTab`, `EffectGroup`, `EffectItem`, `PresetBar`, `ParticlesTab`, `ParticleItem`, `ParticleDetail` |
| `visuals.particles` | `ParticleGate`, `StyleStore`, `StyleSlot`, `StyledParticle`, `ParticleIds`, `ParticleSprites`, `ParticleBurst`, `PreviewOptions` |
| `visuals.particles.core` | pure: `ParticleStyle`, `ParticleStyles`, `StyleCodec`, `StylePlan`, `TintMath`, `ParticlePresets`, `ParticleNames` |
| `visuals.mixin.effects` | `GameRendererFxMixin`, `GuiFxMixin`, `LivingEntityFxMixin`, `ParticleEngineFxMixin`, `ParticleMixin`, `ScreenEffectRendererFxMixin`, `SingleQuadParticleMixin` |

Tests: JUnit `src/junit/java/.../visuals/{particles,effects}/core`; gametest `EffectsGameTest` (+ `EffectsEditorChecks`,
`EffectsOverlayChecks`, `EffectsWorldChecks`, `EffectsParticleChecks`, `EffectsRuleChecks`), shots in
`build/review/effects/`:

```
./gradlew :fabric-1.21.11:runClientGameTest -Dvibeyclient.test.only=EffectsGameTest -Dvibeyclient.test.relay=off
```

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
