// Seal Beanie – knitted Vibe-purple beanie with a teal stripe and a little seal pom-pom.
import { C, cosmetic, shade, mix } from "./_common.mjs";

const knit = (img, base, seed, stripe = null) => {
  img.noisy(base, 0.03, seed);
  for (let x = 1; x < img.width; x += 2) img.col(x, shade(base, 0.86));
  if (stripe != null) {
    img.row(stripe, C.teal).row(stripe + 1, C.tealDark);
    for (let x = 0; x < img.width; x += 2) img.set(x, stripe, C.tealLight);
  }
  return img;
};

export default function () {
  const b = cosmetic("seal_beanie", {
    name: { en: "Seal Beanie", de: "Robbenmütze" },
    description: {
      en: "Cosy knitted beanie with a teal stripe – and a tiny seal as the pom-pom.",
      de: "Kuschelige Strickmütze mit türkisem Streifen – und einer kleinen Robbe als Bommel.",
    },
    tags: ["seal", "knitted", "winter", "cute", "starter"],
    rarity: "rare",
    cosmetic: { slot: "hat", hideWithArmor: ["head"] },
  });
  const hat = b.bone("beanie", { attach: "head", pivot: [0, 24, 0] });
  // crown (covers the top of the head)
  hat.box([-4, 28, -4], [8, 5, 8], {
    sides: (img, f, face) => knit(img, C.brand, 80 + face.length, 1),
    up: (img) => {
      img.noisy(C.brand, 0.03, 81);
      for (let i = 0; i < 8; i++) img.set(i, i, shade(C.brand, 0.85)).set(7 - i, i, shade(C.brand, 0.85));
      img.rect(3, 3, 2, 2, C.brandDeep);
    },
    down: (img) => img.noisy(C.brandDark, 0.03, 82),
  }, { inflate: 0.6 });
  // folded cuff (ribbed)
  hat.box([-4, 28, -4], [8, 2, 8], {
    sides: (img, f, face) => {
      img.noisy(C.brandDeep, 0.03, 83 + face.length);
      for (let x = 0; x < img.width; x += 2) img.col(x, mix(C.brandDeep, C.brand, 0.35));
      img.row(0, C.brandLight);
    },
    up: (img) => img.noisy(C.brandDeep, 0.03, 84),
    down: (img) => img.noisy(C.brandDark, 0.03, 85),
  }, { inflate: 0.95 });

  // rounded top
  hat.box([-3, 33, -3], [6, 1, 6], {
    sides: (img, f, face) => knit(img, C.brand, 93 + face.length),
    up: (img) => img.noisy(C.brand, 0.03, 94).rect(2, 2, 2, 2, C.brandDeep),
    down: (img) => img.noisy(C.brandDark, 0.03, 95),
  }, { inflate: 0.6 });
  // seal pom-pom
  const pom = b.bone("pompom", { parent: "beanie", pivot: [0, 34.6, 0] });
  pom.box([-3, 34.6, -2.5], [6, 5, 5], {
    north: (img) =>
      img.noisy(C.body, 0.02, 86).stamp(0, 0, ["######", "#e##e#", "c#ss#c", "#snns#", "##ss##"], {
        "#": C.body, e: C.eye, s: C.snout, n: C.nose, c: C.cheek,
      }),
    up: (img) => img.noisy(C.back, 0.03, 87).px(C.spot, [[4, 1]]),
    down: (img) => img.noisy(C.belly, 0.02, 88),
    sides: (img) => img.noisy(C.body, 0.025, 89),
    south: (img) => img.noisy(C.back, 0.025, 90),
  });
  // little flippers on the pom-pom
  pom.box([-4, 35, -1], [1, 1, 2], { all: (img) => img.noisy(C.flipper, 0.02, 91) });
  pom.box([3, 35, -1], [1, 1, 2], { all: (img) => img.noisy(C.flipper, 0.02, 92) });

  const idle = b.anim("idle", 3, true);
  idle.rot("pompom", [[0, [0, 0, 0]], [0.75, [4, 0, 5]], [1.5, [0, 0, 0]], [2.25, [-3, 0, -5]], [3, [0, 0, 0]]]);
  const walk = b.anim("walk", 0.8, true);
  walk.rot("pompom", [[0, [6, 0, 0]], [0.2, [-4, 0, 7]], [0.4, [6, 0, 0]], [0.6, [-4, 0, -7]], [0.8, [6, 0, 0]]]);
  return b;
}
