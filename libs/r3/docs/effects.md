# Effects

Effects are object contents (`fx.Effect`): the core simulates them, the backend only draws quads. Put one into a
`WorldObject`; its transform is the emitter origin and orientation, its anchor can follow an entity.

```java
ParticleSystem sparks = new ParticleSystem(256, ParticleStyle.glowDots(0xFFFFC040).withPhysics(9.8f, 0.4f), seed)
        .emitter(new Emitter(EmitterShape.SPHERE).radius(0.3f).rate(40).speed(1, 2)
                .lifetime(0.6f, 1.0f).size(0.08f, 0.15f));
scene.add(WorldObject.create("mymod:sparks").content(sparks).anchoredTo(EntityAnchor.aboveHead(uuid, 0)));
```

## Interface

| Method | When |
|---|---|
| `simulate(dt, ctx)` | every frame while not culled by distance |
| `draw(canvas, ctx)` | when visible |
| `finished()` | true → the scene removes the object (one-shot effects) |
| `liveParticles()` | for budgets/debugging |

`EffectContext` gives origin, orientation and its axes, camera position and camera right/up (for billboards), `lod`,
`alpha`, `light` and the `EffectBudget`.

## Particles

`ParticleSystem(capacity, style, seed)` owns a `ParticlePool` (struct of arrays, fixed capacity, swap-remove) and
`Emitter`s. **Deterministic:** the same seed and the same dt sequence give the same particles (`Rng`, xorshift).
**No allocation after warmup:** simulation and drawing only touch preallocated arrays (`noAllocationAfterWarmup`
test measures 0 bytes over 2000 frames).

`Emitter(shape)`:

| Setting | Meaning |
|---|---|
| shape `POINT` / `RING` / `SPHERE` / `BEAM` / `BOX` | where particles start: origin; ring of `radius` in the object plane (right/up); inside a sphere; along `length` on the object normal; inside `box(hx, hy, hz)` |
| `rate(perSecond)`, `burst(n)` | continuous and one-shot spawning (both scaled by LOD) |
| `speed(min, max)` | along the shape's outward direction |
| `velocity(x, y, z)` + `localVelocity(true)` | base velocity, optionally rotated with the object |
| `spread(s)` | random ± per axis |
| `lifetime(min, max)`, `size(min, max)`, `spin(maxRadPerSec)` | per particle ranges |

`ParticleStyle(color ramp, size curve, gravity, drag, mode, texture, u0, v0, u1, v1)`: `ColorRamp` (stops, ARGB lerp)
and `FloatCurve` (size multiplier over life, `popAndShrink`), gravity in blocks/s², drag per second
(`v *= 1 − drag·dt`), render mode, optional sprite texture + UV rect.

Converting per-tick values (old avatar particles): velocity ×20, gravity ×400, tick drag 0.98 ≈ 0.4/s, lifetime
in seconds.

## Other effects

| Effect | What |
|---|---|
| `Beam().to(x, y, z).width(w).colors(core, glow).pulse(hz, amount)` | additive camera-facing ribbon: wide glow + thin core |
| `Ring(startRadius, endRadius, seconds).thickness().color(ramp).repeat(every).inObjectPlane()` | expanding ring/pulse, horizontal by default |
| `Trail(capacity, pointLifetime).spacing(m).width(start, end).color(ramp)` | ribbon behind a moving object (samples the origin every `spacing` meters) |
| `Glow(size, color).texture(soft).pulse(hz, amount)` | one additive billboard |

## Budget and level of detail

`EffectBudget(maxNewParticlesPerFrame, fullDetail, fadeOut, cull)` (standard: 2048, 16, 48, 96 blocks):
`lodAt(distance)` is 1 up to `fullDetail`, falls to 0.1 at `fadeOut`, 0 at `cull` (no simulation, no drawing).
Emitters spawn `rate × lod`; `take(n)` caps new particles per frame across all systems. The scene resets the budget
each frame.

## Render modes

`CUTOUT` (alpha test), `TRANSLUCENT` (blended, sorted back to front), `ADDITIVE` (`src·α + dst`, no alpha cut-off –
soft glows stay soft), `EMISSIVE` (full bright, blended), `SEE_THROUGH` (full bright, blended, ignores depth: markers and hints that must
show through walls). See [backend.md](backend.md) for the mapping to Minecraft
render types.
