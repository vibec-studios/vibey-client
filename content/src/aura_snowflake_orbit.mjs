// Snowflake Orbit – glowing pixel snowflakes circling you, shedding tiny flakes.
import { C, cosmetic } from "./_common.mjs";

const FLAKE = ["...w...", ".w.w.w.", "..www..", "wwwcwww", "..www..", ".w.w.w.", "...w..."];
const FLAKES = [
  // [angle°, height, radius]
  [0, 7, 13],
  [72, 23, 12],
  [144, 13, 14],
  [216, 28, 12],
  [288, 18, 14],
];

export default function () {
  const b = cosmetic("snowflake_orbit", {
    name: { en: "Snowflake Orbit", de: "Schneeflocken-Orbit" },
    description: {
      en: "Five glowing snowflakes circle around you and drop tiny flakes.",
      de: "Fünf leuchtende Schneeflocken kreisen um dich und lassen kleine Flocken rieseln.",
    },
    tags: ["aura", "snow", "winter", "glow", "starter"],
    rarity: "epic",
    cosmetic: { slot: "aura" },
  });
  const tex = b.texture("main");
  const paintFlake = (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, FLAKE, { w: [240, 250, 255, 235], c: C.teal });
  const orbit = b.bone("orbit", { attach: "world", pivot: [0, 0, 0], emissive: true, translucent: true });
  FLAKES.forEach(([deg, h, r], i) => {
    const a = (deg * Math.PI) / 180;
    const x = +(Math.cos(a) * r).toFixed(2), z = +(Math.sin(a) * r).toFixed(2);
    const flake = b.bone(`flake_${i}`, { parent: orbit.name, pivot: [x, h, z], emissive: true, translucent: true });
    // two crossed planes so the flake reads from every angle
    flake.box([x - 3.5, h - 3.5, z], [7, 7, 0], { north: paintFlake, south: paintFlake });
    flake.box([x, h - 3.5, z - 3.5], [0, 7, 7], { east: paintFlake, west: paintFlake });
    b.locator(`flake_${i}`, flake.name, [x, h - 3, z]);
  });
  const dot = tex.region(3, 3, (img) => img.fill([0, 0, 0, 0]).stamp(0, 0, [".w.", "wWw", ".w."], { w: [200, 235, 255, 230], W: "#ffffff" }));
  b.sprite("snow", "main", dot);

  const idle = b.anim("idle", 8, true);
  idle.rot("orbit", [[0, [0, 0, 0]], [8, [0, -360, 0]]], "linear");
  FLAKES.forEach((_, i) => {
    const ph = i * 0.8;
    idle.rot(`flake_${i}`, [[0, [0, 0, 0]], [8, [0, 720, 0]]], "linear");
    idle.pos(`flake_${i}`, [[0, [0, 0, 0]], [2 + (ph % 2), [0, 1.5, 0]], [6 - (ph % 2), [0, -1.5, 0]], [8, [0, 0, 0]]]);
  });
  for (let k = 0; k < 16; k++)
    idle.particle(k * 0.5 + 0.1, {
      sprite: "snow", locator: `flake_${k % FLAKES.length}`, count: 1, lifetime: 1.6, size: 0.09,
      spread: [0.1, 0.05, 0.1], velocity: [0, -0.015, 0], gravity: 0.0005, emissive: true,
    });
  return b;
}
