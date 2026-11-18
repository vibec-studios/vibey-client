// Ice Crown – a small circlet of translucent ice spikes with a glowing teal gem.
import { C, cosmetic, mix } from "./_common.mjs";

const ice = (img, seed) => {
  img.noisy([170, 214, 244, 228], 0.03, seed);
  img.row(0, [246, 251, 255, 235]);
  img.row(img.height - 1, [120, 176, 222, 230]);
  return img;
};

export default function () {
  const b = cosmetic("ice_crown", {
    name: { en: "Ice Crown", de: "Eiskrone" },
    description: {
      en: "A circlet of glacier ice with a glowing teal gem. Sparkles now and then.",
      de: "Ein Reif aus Gletschereis mit leuchtendem türkisen Edelstein. Funkelt ab und zu.",
    },
    tags: ["ice", "crown", "royal", "starter"],
    rarity: "epic",
    cosmetic: { slot: "hat", hideWithArmor: ["head"] },
  });
  const crown = b.bone("crown", { attach: "head", pivot: [0, 32, 0], translucent: true });
  // band around the top of the head
  crown.box([-4, 31, -4], [8, 2, 8], { sides: (img, f, face) => ice(img, 100 + face.length), up: (img) => img.fill([0, 0, 0, 0]).border([220, 240, 252, 220]), down: (img) => img.fill([0, 0, 0, 0]) }, { inflate: 0.55 });
  // spikes along the rim: tall in the middle of each side, small at the corners
  const spike = (x, z, w, h, d, seed) =>
    crown.box([x, 33.55, z], [w, h, d], {
      sides: (img) => {
        img.fill([0, 0, 0, 0]);
        for (let y = 0; y < img.height; y++)
          for (let xx = 0; xx < img.width; xx++) img.set(xx, y, y === 0 ? [255, 255, 255, 240] : mix([210, 236, 252, 215], [150, 200, 236, 205], y / img.height));
      },
      up: (img) => img.fill([255, 255, 255, 240]),
      down: (img) => img.fill([150, 200, 236, 205]),
    });
  const E = 4.55; // rim position
  const tip = (x, z, w, d, h) => spike(x, z, w, h, d, 0);
  for (const s of [-1, 1]) {
    const zf = s < 0 ? -E : E - 1; // front / back rim
    tip(-1, zf, 2, 1, 2);
    crown.box([-0.5, 35.55, zf], [1, 1, 1], { all: (img) => img.fill([255, 255, 255, 245]) });
    tip(2, zf, 1, 1, 1);
    tip(-3, zf, 1, 1, 1);
    const xs = s < 0 ? E - 1 : -E; // left (−X) / right (+X) rim
    tip(xs, -1, 1, 2, 2);
    crown.box([xs, 35.55, -0.5], [1, 1, 1], { all: (img) => img.fill([255, 255, 255, 245]) });
    tip(xs, -3, 1, 1, 1);
    tip(xs, 2, 1, 1, 1);
  }

  const gem = b.bone("gem", { parent: "crown", pivot: [0, 32, -4.6], emissive: true });
  gem.box([-1, 31.4, -5.4], [2, 2, 1], {
    north: (img) => img.fill(C.teal).set(0, 0, C.tealLight),
    sides: (img) => img.fill(C.tealDark),
    up: (img) => img.fill(C.tealLight),
    down: (img) => img.fill(C.tealDeep),
  });

  // sparkle sprite
  const glint = b.defaultTexture().region(5, 5, (img) => img.stamp(0, 0, ["..w..", "..w..", "wwWww", "..w..", "..w.."], { w: "#bff7ee", W: "#ffffff" }));
  b.sprite("glint", "main", glint);
  b.locator("tip_front", "crown", [0, 36.5, -4.2]);
  b.locator("tip_left", "crown", [-4.2, 36.5, 0]);
  b.locator("tip_right", "crown", [4.2, 36.5, 0]);
  const idle = b.anim("idle", 3, true);
  const sparkle = { sprite: "glint", count: 1, lifetime: 0.45, size: 0.14, spread: [0.02, 0.02, 0.02], velocity: [0, 0.004, 0], emissive: true };
  idle.particle(0.3, { ...sparkle, locator: "tip_front" });
  idle.particle(1.4, { ...sparkle, locator: "tip_left" });
  idle.particle(2.3, { ...sparkle, locator: "tip_right" });
  return b;
}
