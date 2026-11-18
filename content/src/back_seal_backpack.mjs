// Seal Backpack – a mini Vibe backpack with a swinging seal keychain.
import { C, cosmetic, shade, mix } from "./_common.mjs";

const fabric = (img, base, seed) => {
  img.noisy(base, 0.03, seed);
  return img;
};

export default function () {
  const b = cosmetic("seal_backpack", {
    name: { en: "Seal Backpack", de: "Robben-Rucksack" },
    description: {
      en: "A mini backpack in Vibe purple with a little seal keychain that swings as you walk.",
      de: "Ein Mini-Rucksack in Vibe-Lila mit einem kleinen Robben-Anhänger, der beim Laufen schaukelt.",
    },
    tags: ["seal", "backpack", "cute", "starter"],
    rarity: "rare",
    cosmetic: { slot: "back" },
  });
  const pack = b.bone("backpack", { attach: "body", pivot: [0, 24, 0] });
  // main bag
  pack.box([-3, 13, 2], [6, 8, 3], {
    south: (img) => {
      fabric(img, C.brand, 120);
      img.col(0, C.brandDeep).col(5, C.brandDeep).row(7, C.brandDeep);
    },
    sides: (img, f, face) => fabric(img, C.brandDeep, 121 + face.length).row(7, C.brandDark).col(1, shade(C.brandDeep, 0.85)),
    up: (img) => fabric(img, C.brandDeep, 122),
    down: (img) => fabric(img, C.brandDark, 123),
  });
  // lid flap
  pack.box([-3, 19, 2], [6, 2, 3], {
    south: (img) => fabric(img, C.brandLight, 124).row(1, C.brand).px(C.teal, [[2, 1], [3, 1]]),
    sides: (img) => fabric(img, C.brandLight, 125).row(1, C.brand),
    up: (img) => fabric(img, C.brandLight, 126).row(2, C.brand),
    down: (img) => fabric(img, C.brandDeep, 127),
  }, { inflate: 0.3 });
  // front pocket with teal zipper
  pack.box([-2, 14, 5], [4, 3, 1], {
    south: (img) => fabric(img, C.teal, 128).row(0, C.tealLight).set(3, 0, "#ffffff").row(2, C.tealDark),
    all: (img) => fabric(img, C.tealDark, 129),
  });
  // shoulder straps (visible on the chest) + chest strap
  for (const x of [-3, 2]) {
    pack.box([x, 15, -2.5], [1, 9, 5], {
      north: (img) => fabric(img, C.surface3, 130).set(0, 5, C.teal),
      sides: (img) => fabric(img, C.surface3, 131),
      up: (img) => fabric(img, C.surface3, 132),
      south: (img) => fabric(img, C.surface3, 133),
      down: (img) => fabric(img, C.surface3, 134),
    }, { inflate: 0.05 });
  }
  pack.box([-2, 19, -2.6], [4, 1, 1], { all: (img) => fabric(img, C.surface3, 135).set(1, 0, C.dim).set(2, 0, C.dim) });

  // keychain: hangs from the pocket zipper on the back, seal charm facing backwards
  const chain = b.bone("keychain", { parent: "backpack", pivot: [1.5, 17, 6.2] });
  chain.box([1.5, 15, 5.7], [0, 2, 1], { sides: (img) => img.fill("#c9ced6").set(0, 1, "#8e95a0") });
  chain.box([0, 12, 5.7], [3, 3, 2], {
    south: (img) => img.noisy(C.body, 0.02, 136).stamp(0, 0, ["e.e", "sns", ".s."], { e: C.eye, s: C.snout, n: C.nose }).set(1, 0, C.body),
    north: (img) => img.noisy(C.back, 0.02, 138),
    up: (img) => img.noisy(C.back, 0.02, 139),
    down: (img) => img.noisy(C.belly, 0.02, 140),
    sides: (img) => img.noisy(C.body, 0.02, 141),
  });
  for (const x of [-0.6, 2.6]) chain.box([x, 12.2, 6.2], [1, 1, 1], { all: (img) => img.fill(C.flipper) }, { inflate: -0.2 }); // flippers

  const idle = b.anim("idle", 2.4, true);
  idle.rot("keychain", [[0, [0, 0, 0]], [0.6, [6, 0, 8]], [1.2, [0, 0, 0]], [1.8, [-6, 0, -6]], [2.4, [0, 0, 0]]]);
  const walk = b.anim("walk", 0.8, true);
  walk.rot("keychain", [[0, [14, 0, 0]], [0.2, [-6, 0, 18]], [0.4, [14, 0, 0]], [0.6, [-6, 0, -18]], [0.8, [14, 0, 0]]]);
  walk.pos("backpack", [[0, [0, 0, 0]], [0.2, [0, 0.4, 0]], [0.4, [0, 0, 0]], [0.6, [0, 0.4, 0]], [0.8, [0, 0, 0]]]);
  return b;
}
