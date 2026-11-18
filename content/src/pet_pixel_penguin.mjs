// Pixel Penguin – a tiny waddling penguin with a teal Vibe scarf.
import { C, cosmetic } from "./_common.mjs";

const K = "#24242c", K2 = "#2f2f39", W = "#f1f3f6", O = "#ffad3b", O2 = "#e88a1c";

export default function () {
  const b = cosmetic("pixel_penguin", {
    name: { en: "Pixel Penguin", de: "Pixel-Pinguin" },
    description: {
      en: "A tiny penguin in a teal scarf that waddles after you and flaps when it gets excited.",
      de: "Ein kleiner Pinguin mit türkisem Schal, der dir hinterherwatschelt und vor Freude mit den Flügeln schlägt.",
    },
    tags: ["penguin", "cute", "winter", "pet", "starter"],
    rarity: "rare",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.55 } },
  });
  const root = b.bone("penguin", { attach: "world", pivot: [0, 0, 0] });
  const body = b.bone("pen_body", { parent: "penguin", pivot: [0, 1, 0] });
  body.box([-3.5, 1, -3], [7, 9, 6], {
    north: (img) => img.noisy(K, 0.03, 150).stamp(0, 0, [
      ".......",
      ".wwwww.",
      "wwwwwww",
      "wwwwwww",
      "wwwwwww",
      "wwwwwww",
      "wwwwwww",
      ".wwwww.",
      "..www..",
    ], { w: W }),
    sides: (img) => img.noisy(K, 0.03, 151).row(img.height - 1, K2),
    up: (img) => img.noisy(K, 0.03, 152),
    down: (img) => img.noisy(K2, 0.03, 153),
  });
  // scarf (teal, Vibe!) with a tail on the back
  body.box([-3.5, 8.5, -3], [7, 2, 6], {
    sides: (img) => { img.noisy(C.teal, 0.03, 154).row(1, C.tealDark); for (let x = 0; x < img.width; x += 3) img.set(x, 0, C.tealLight); },
    up: (img) => img.noisy(C.teal, 0.03, 155),
    down: (img) => img.noisy(C.tealDark, 0.03, 156),
  }, { inflate: 0.35 });
  body.box([1, 5, 3.3], [2, 4, 1], {
    south: (img) => img.noisy(C.teal, 0.03, 157).row(3, C.brand).row(2, C.tealDark),
    all: (img) => img.noisy(C.tealDark, 0.03, 158),
  });

  const head = b.bone("pen_head", { parent: "pen_body", pivot: [0, 10.5, 0] });
  head.box([-3.5, 10.5, -3], [7, 6, 6], {
    north: (img) => img.noisy(K, 0.03, 160).stamp(0, 1, ["kwwwwwk", "wewwwew", "wwwwwww", "pwwwwwp", ".kwwwk."], { w: W, e: "#141418", k: K, p: "#ffb3c7" }),
    sides: (img) => img.noisy(K, 0.03, 161),
    up: (img) => img.noisy(K, 0.03, 162).set(2, 1, K2),
    down: (img) => img.noisy(K2, 0.03, 163),
  });
  head.box([-1, 12, -5], [2, 1, 2], {
    north: (img) => img.fill(O),
    up: (img) => img.fill(O),
    down: (img) => img.fill(O2),
    sides: (img) => img.fill(O2),
  });
  const lids = b.bone("pen_lids", { parent: "pen_head", pivot: [0, 14, -3] });
  for (const x of [-2.5, 1.5]) lids.box([x, 13.5, -3.05], [1, 1, 0], { north: (img) => img.fill("#8c939e") });

  for (const s of [1, -1]) {
    const side = s > 0 ? "left" : "right";
    const fl = b.bone(`pen_flipper_${side}`, { parent: "pen_body", pivot: [s * 3.5, 9, 0] });
    fl.box([s > 0 ? 3.5 : -4.5, 3, -1.5], [1, 6, 3], { all: (img) => img.noisy(K, 0.03, 165).row(img.height - 1, K2) });
    const foot = b.bone(`pen_foot_${side}`, { parent: "penguin", pivot: [s * 1.5, 1, -1] });
    foot.box([s > 0 ? 0.5 : -2.5, 0, -3.5], [2, 1, 3], { up: (img) => img.fill(O), north: (img) => img.fill(O2), sides: (img) => img.fill(O2), down: (img) => img.fill(O2) });
  }

  const idle = b.anim("idle", 3.2, true);
  idle.rot("pen_head", [[0, [0, 0, 0]], [0.8, [0, 18, 4]], [1.5, [0, 18, 4]], [2.1, [-6, -12, -3]], [2.7, [0, -8, 0]], [3.2, [0, 0, 0]]]);
  idle.rot("pen_flipper_left", [[0, [0, 0, 0]], [2.2, [0, 0, 0]], [2.35, [0, 0, -35]], [2.5, [0, 0, -5]], [2.65, [0, 0, -35]], [2.85, [0, 0, 0]], [3.2, [0, 0, 0]]]);
  idle.rot("pen_flipper_right", [[0, [0, 0, 0]], [2.2, [0, 0, 0]], [2.35, [0, 0, 35]], [2.5, [0, 0, 5]], [2.65, [0, 0, 35]], [2.85, [0, 0, 0]], [3.2, [0, 0, 0]]]);
  idle.pos("pen_body", [[0, [0, 0, 0]], [2.2, [0, 0, 0]], [2.4, [0, 0.8, 0]], [2.6, [0, 0, 0]], [3.2, [0, 0, 0]]]);
  idle.visible(0, "pen_lids", false).visible(1.1, "pen_lids", true).visible(1.24, "pen_lids", false);

  const walk = b.anim("walk", 0.6, true);
  walk.rot("pen_body", [[0, [0, 0, -8]], [0.3, [0, 0, 8]], [0.6, [0, 0, -8]]]);
  walk.rot("pen_head", [[0, [0, 0, 5]], [0.3, [0, 0, -5]], [0.6, [0, 0, 5]]]);
  walk.rot("pen_flipper_left", [[0, [0, 0, -18]], [0.3, [0, 0, -8]], [0.6, [0, 0, -18]]]);
  walk.rot("pen_flipper_right", [[0, [0, 0, 8]], [0.3, [0, 0, 18]], [0.6, [0, 0, 8]]]);
  walk.pos("pen_foot_left", [[0, [0, 0, 0]], [0.15, [0, 0, 0]], [0.3, [0, 0.8, -1]], [0.45, [0, 0, 0]], [0.6, [0, 0, 0]]]);
  walk.pos("pen_foot_right", [[0, [0, 0.8, -1]], [0.15, [0, 0, 0]], [0.45, [0, 0, 0]], [0.6, [0, 0.8, -1]]]);
  return b;
}
