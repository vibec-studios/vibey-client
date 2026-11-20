# Visuals module (`visuals`)

Item physics for dropped items and a particle amount control (features #26, #27 of [FEATURES.md](FEATURES.md)).
**Fabric 1.21.11 only for now** (`gradle/modules.gradle`: `visuals: ['fabric-1.21.11']`, registered by reflection in
`BuiltinModules`). Code: `src/main/java/dev/henny/vibeyclient/module/visuals`.

> See also: [MODULES.md](MODULES.md) · [CAMERA.md](CAMERA.md)

Both features are purely visual and client side: entity positions, hitboxes, pickup and the particles' gameplay
meaning are untouched. No server rule (hiding particles is what vanilla's *Particles: Minimal* does already, and
the important ones stay unless the player turns that off too).

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
