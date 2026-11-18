// Cheer – jump with both arms up and a burst of Vibe-coloured confetti.
import { emote, C } from "./_common.mjs";

const CONFETTI = { purple: C.brand, teal: C.teal, pink: "#ff7eb6", gold: C.warn };

export default function () {
  const b = emote("cheer", {
    name: { en: "Cheer", de: "Jubeln" },
    description: { en: "Jump for joy with a burst of confetti!", de: "Spring vor Freude – mit einer Ladung Konfetti!" },
    tags: ["celebrate", "confetti", "particles", "starter"],
    preview: { hero: 0.85 },
  });
  const tex = b.defaultTexture();
  for (const [name, col] of Object.entries(CONFETTI)) {
    b.sprite(`confetti_${name}`, "main", tex.region(2, 2, (img) => img.fill(col).set(1, 1, [0, 0, 0, 0]).set(0, 1, col)));
  }
  b.bone("burst", { attach: "root", pivot: [0, 34, 0] });
  b.locator("above", "burst", [0, 35, -1]);

  const a = b.anim("main", 2.4);
  const up = 5.5;
  a.pos("root", [[0, [0, 0, 0]], [0.3, [0, 0, 0]], [0.55, [0, up - 1, 0]], [0.7, [0, up, 0]], [0.95, [0, 0, 0]], [1.15, [0, 1.6, 0]], [1.35, [0, 0, 0]], [2.4, [0, 0, 0]]]);
  a.rot("root", [[0, [0, 0, 0]], [0.3, [-6, 0, 0]], [0.6, [3, 0, 0]], [0.95, [0, 0, 0]], [2.4, [0, 0, 0]]]);
  a.rot("right_arm", [[0, [0, 0, 0]], [0.3, [-35, 0, 8]], [0.6, [12, 0, 150]], [0.95, [10, 0, 145]], [1.15, [10, 0, 160]], [1.35, [10, 0, 140]], [1.85, [8, 0, 135]], [2.4, [0, 0, 0]]]);
  a.rot("left_arm", [[0, [0, 0, 0]], [0.3, [-35, 0, -8]], [0.6, [12, 0, -150]], [0.95, [10, 0, -145]], [1.15, [10, 0, -160]], [1.35, [10, 0, -140]], [1.85, [8, 0, -135]], [2.4, [0, 0, 0]]]);
  a.rot("head", [[0, [0, 0, 0]], [0.3, [-12, 0, 0]], [0.65, [16, 0, 0]], [1.0, [10, 0, 0]], [1.6, [8, 0, 0]], [2.4, [0, 0, 0]]]);
  a.rot("right_leg", [[0, [0, 0, 0]], [0.45, [0, 0, 0]], [0.7, [-14, 0, 6]], [0.95, [0, 0, 0]], [2.4, [0, 0, 0]]]);
  a.rot("left_leg", [[0, [0, 0, 0]], [0.45, [0, 0, 0]], [0.7, [10, 0, -6]], [0.95, [0, 0, 0]], [2.4, [0, 0, 0]]]);
  // confetti: several small bursts in different directions so it fans out on every client
  const dirs = [[0.045, 0.06, 0], [-0.045, 0.06, 0], [0, 0.065, 0.045], [0, 0.065, -0.045], [0.032, 0.07, 0.032], [-0.032, 0.07, -0.032]];
  const names = Object.keys(CONFETTI);
  dirs.forEach((v, i) =>
    a.particle(0.62 + (i % 2) * 0.03, {
      sprite: `confetti_${names[i % names.length]}`, locator: "above", count: 4, lifetime: 1.5, size: 0.09,
      spread: [0.2, 0.08, 0.2], velocity: v, gravity: 0.005, emissive: false,
    }));
  a.particle(1.15, { sprite: "confetti_teal", locator: "above", count: 5, lifetime: 1.1, size: 0.08, spread: [0.4, 0.1, 0.4], velocity: [0, 0.04, 0], gravity: 0.005 });
  a.sound(0.6, "minecraft:entity.firework_rocket.blast", { volume: 0.5, pitch: 1.3 });
  a.sound(0.75, "minecraft:entity.firework_rocket.twinkle", { volume: 0.6, pitch: 1.2 });
  return b;
}
