// The Vibe seal as .vibey bones – a 1:1 port of vibe-launcher/src/components/seal3d.ts (without the ice block).
// seal3d space: head → +X, y up. Here: head → north (−Z), so (x, y, z)_seal → (z, y, −x).
import { C } from "./_common.mjs";

const N = 0.018;
const bellyRows = (img, from) => img.rect(0, from, img.width, 2, C.belly);

/**
 * Add the seal to bundle `b`.
 * opts: { prefix = "seal", attach = "world", parent = null, o = [0,0,0] (offset of the seal's feet),
 *         seed = 1 }
 * Returns bone names { root, head, lids, finL, finR, tailL, tailR }.
 */
export function addSeal(b, opts = {}) {
  const p = opts.prefix ?? "seal";
  const o = opts.o ?? [0, 0, 0];
  const s = opts.seed ?? 1;
  const at = (x, y, z) => [x + o[0], y + o[1], z + o[2]];
  const names = {
    root: p,
    head: `${p}_head`,
    lids: `${p}_lids`,
    finL: `${p}_fin_left`,
    finR: `${p}_fin_right`,
    tailL: `${p}_tail_left`,
    tailR: `${p}_tail_right`,
  };

  const root = b.bone(names.root, { parent: opts.parent ?? null, attach: opts.parent ? undefined : opts.attach ?? "world", pivot: at(0, 0, 0) });
  // body 10 (x) × 7 × 16 (z), lying on the ground
  root.box(at(-5, 0, -8), [10, 7, 16], {
    up: (img) => img.noisy(C.back, N, s + 3).px(C.spot, [[3, 4], [3, 5], [6, 9], [2, 12], [7, 13]]),
    down: (img) => img.noisy(C.belly, N, s + 4),
    sides: (img, f, face) => bellyRows(img.noisy(C.body, N, s + 5 + face.length), 5),
  });
  // tail 6 × 4 × 4 behind the body
  root.box(at(-3, 0, 8), [6, 4, 4], {
    up: (img) => img.noisy(C.back, N, s + 9),
    down: (img) => img.noisy(C.belly, N, s + 10),
    sides: (img) => img.noisy(C.body, N, s + 11).row(3, C.belly),
  });

  // head (turns around the neck)
  const head = b.bone(names.head, { parent: names.root, pivot: at(0, 6.5, -6) });
  head.box(at(-4, 5, -13), [8, 8, 8], {
    north: (img) =>
      img
        .noisy(C.body, 0.015, s + 20)
        .px(C.cheek, [[0, 6], [7, 6]])
        .px(C.eye, [[1, 3], [2, 3], [1, 4], [2, 4], [5, 3], [6, 3], [5, 4], [6, 4]])
        .px(C.shine, [[1, 3], [5, 3]]),
    up: (img) => img.noisy(C.back, N, s + 21).px(C.spot, [[5, 2]]),
    down: (img) => img.noisy(C.belly, N, s + 22),
    sides: (img) => img.noisy(C.body, N, s + 23),
    south: (img) => img.noisy(C.body, N, s + 24),
  });
  // snout 6 × 3 × 3 with nose + whisker dots
  head.box(at(-3, 5, -16), [6, 3, 3], {
    north: (img) =>
      img
        .noisy(C.snout, 0.012, s + 30)
        .px(C.nose, [[2, 0], [3, 0]])
        .px(C.whisker, [[1, 1], [4, 1], [0, 2], [5, 2]]),
    all: (img) => img.noisy(C.snout, 0.012, s + 31),
  });
  // eyelids (shown briefly for a blink): thin planes just in front of the eyes
  const lids = b.bone(names.lids, { parent: names.head, pivot: at(0, 9, -13) });
  for (const x of [-3, 1]) {
    lids.box(at(x, 8, -13.05), [2, 2, 0], {
      north: (img) => img.noisy(C.body, 0.015, s + 40 + x).row(1, C.eye),
    });
  }

  // flippers: 3 × 1 × 5, pivot at the shoulder, splayed outwards
  const fin = (name, pivot, from, ry, seed) => {
    const bone = b.bone(name, { parent: names.root, pivot: at(...pivot), rotation: [0, ry, 0] });
    bone.box(at(...from), [3, 1, 5], { all: (img) => img.noisy(C.flipper, 0.02, seed) });
    return bone;
  };
  fin(names.finL, [5, 1, -4], [4.5, 0, -8], -26, s + 50);
  fin(names.finR, [-5, 1, -4], [-7.5, 0, -8], 26, s + 51);
  fin(names.tailL, [1.2, 1, 12], [0.7, 0, 11.5], 29, s + 52);
  fin(names.tailR, [-1.2, 1, 12], [-3.7, 0, 11.5], -29, s + 53);
  return names;
}
