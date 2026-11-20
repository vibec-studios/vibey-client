# Visuals module (`visuals`)

Item physics for dropped items and a particle amount control (features #26, #27 of [FEATURES.md](FEATURES.md)),
plus effects and a per-type particle editor ([below](#effects-and-particles-part-visualseffects)).
**Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `visuals: ['fabric-1.21.11']`, registered by reflection in
`BuiltinModules`). Code: `src/main/java/dev/henny/vibeyclient/module/visuals`.

> See also: [MODULES.md](MODULES.md) · [CAMERA.md](CAMERA.md)

Both features are purely visual and client side: entity positions, hitboxes, pickup and the particles' gameplay
meaning are untouched. No server rule for these two (hiding particles is what vanilla's *Particles: Minimal* does
already, and the important ones stay unless the player turns that off too). Per-type particle styles and the
effects that hide more than vanilla can have rules (`visuals.*`, see below).

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
| `visuals.core` | pure, JUnit in `src/junit/java/.../module/visuals/core`: `ItemTumble`, `ItemLift`, `ParticleCategory`, `ParticleTable`, `ParticleBudget`, `ParticlePlan` |
| `visuals.items` | `ItemPhysics`, `ItemMotion`, `ItemMotionHolder`, `ItemPose` |
| `visuals.particles` | `ParticleGate` (plan per spawn, extra copies, stats for tests) |
| `visuals.mixin` | `ItemEntityMixin`, `ItemEntityRenderStateMixin`, `ItemEntityRendererMixin`, `ParticleEngineMixin` (all `require = 0`) |

## Testing

- Unit: `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.visuals.*"` (11 tests).
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
