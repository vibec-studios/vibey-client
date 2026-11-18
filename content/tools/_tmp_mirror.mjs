// Mirror an old-convention (right arm at -X) bundle JSON into Blockbench space (right arm at +X).
const nx = (p) => [-p[0], p[1], p[2]];
export function mirrorBundle(j) {
  for (const b of j.model?.bones ?? []) {
    b.pivot = nx(b.pivot);
    for (const c of b.cubes) {
      const f = c.from, t = c.to;
      c.from = [-t[0], f[1], f[2]];
      c.to = [-f[0], t[1], t[2]];
      if (c.pivot) c.pivot = nx(c.pivot);
    }
  }
  for (const l of Object.values(j.model?.locators ?? {})) l.position = nx(l.position);
  for (const a of Object.values(j.animations ?? {})) {
    for (const ch of Object.values(a.bones ?? {})) for (const k of ch.position ?? []) { k.v = nx(k.v); if (k.pre) k.pre = nx(k.pre); }
    for (const e of a.events ?? []) if (e.velocity) e.velocity = nx(e.velocity);
  }
  return j;
}
