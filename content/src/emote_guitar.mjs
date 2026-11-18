// Guitar Jam – a Vibe guitar appears and you jam a little riff (loop, 120 bpm).
import { emote, aim, SHOULDER, C, shade } from "./_common.mjs";

const P = [0, 15, -3.5]; // guitar rotation centre (in front of the belly)
const TILT = 48; // neck towards the left hand

export default function () {
  const b = emote("guitar", {
    name: { en: "Guitar Jam", de: "Gitarrensolo" },
    description: { en: "Jam a little riff on the Vibe guitar. Move to stop.", de: "Spiel ein kleines Riff auf der Vibe-Gitarre. Bewegen zum Beenden." },
    tags: ["music", "props", "loop", "sound", "starter"],
    rarity: "rare",
  }, { loop: true });
  const g = b.bone("guitar", { attach: "body", pivot: P, rotation: [0, 0, TILT] });
  const wood = "#3b2a24", fret = "#2a1d19", str = "#d9dde3";
  const zf = P[2] - 1; // front face z of the body (−4.5 … −2.5)
  // lower bout
  g.box([-4, P[1] - 7, zf - 1], [8, 6, 2], {
    north: (img) => img.noisy(C.brand, 0.03, 170).border(C.teal).row(3, shade(C.brand, 0.6), 2, 5).col(3, str, 0, 2).col(4, str, 0, 2),
    sides: (img) => img.noisy(C.brandDeep, 0.03, 171).row(0, C.teal),
    south: (img) => img.noisy(C.brandDark, 0.03, 172),
    up: (img) => img.noisy(C.brandDeep, 0.03, 173),
    down: (img) => img.noisy(C.brandDeep, 0.03, 174),
  });
  // upper bout with sound hole
  g.box([-3, P[1] - 1, zf - 1], [6, 4, 2], {
    north: (img) => img.noisy(C.brand, 0.03, 175).col(0, C.teal).col(5, C.teal).row(0, C.teal).rect(2, 1, 2, 2, C.brandNight).col(2, str, 3, 3).col(3, str, 3, 3),
    sides: (img) => img.noisy(C.brandDeep, 0.03, 176),
    south: (img) => img.noisy(C.brandDark, 0.03, 177),
    up: (img) => img.noisy(C.brandDeep, 0.03, 178),
    down: (img) => img.noisy(C.brandDeep, 0.03, 179),
  });
  // neck + headstock
  g.box([-1, P[1] + 3, zf - 0.7], [2, 11, 1], {
    north: (img) => { img.noisy(fret, 0.03, 180).col(0, str).col(1, str); for (let y = 1; y < 11; y += 3) img.row(y, "#8e95a0"); },
    sides: (img) => img.noisy(wood, 0.03, 181),
    south: (img) => img.noisy(wood, 0.03, 182),
    up: (img) => img.noisy(wood, 0.03, 183),
    down: (img) => img.noisy(wood, 0.03, 184),
  });
  g.box([-1.5, P[1] + 14, zf - 0.7], [3, 3, 1], {
    north: (img) => img.noisy(C.brand, 0.03, 185).px(C.tealLight, [[0, 0], [2, 0], [0, 2], [2, 2]]),
    all: (img) => img.noisy(C.brandDeep, 0.03, 186),
  });
  b.locator("sound_hole", "guitar", [0, P[1] - 1, zf - 2.5]);

  // arms: left hand on the neck, right hand strumming over the sound hole
  const rad = (TILT * Math.PI) / 180;
  const neck = [P[0] + Math.sin(rad) * 10, P[1] + Math.cos(rad) * 10, zf - 1.5];
  const strum = [P[0] - Math.sin(rad) * 1, P[1] + Math.cos(rad) * 0.5 - 1, zf - 2.5];
  const L = aim(SHOULDER.left, neck);
  const R = aim(SHOULDER.right, strum);
  const a = b.anim("main", 2.0, true);
  const strumKeys = [];
  for (let i = 0; i <= 8; i++) strumKeys.push([i * 0.25, [R[0] + (i % 2 ? -9 : 7), R[1] + (i % 2 ? 4 : 0), 0]]);
  a.rot("right_arm", strumKeys);
  a.rot("left_arm", [[0, L], [0.5, [L[0] + 3, L[1] - 3, 0]], [1.0, L], [1.5, [L[0] + 3, L[1] + 3, 0]], [2.0, L]]);
  a.rot("head", [[0, [-10, 8, 0]], [0.25, [0, 8, 4]], [0.5, [-10, 8, 0]], [0.75, [0, 8, -4]], [1.0, [-10, 8, 0]], [1.25, [0, 8, 4]], [1.5, [-10, 8, 0]], [1.75, [0, 8, -4]], [2.0, [-10, 8, 0]]]);
  a.rot("root", [[0, [0, 0, 0]], [0.5, [0, 0, 3]], [1.0, [0, 0, 0]], [1.5, [0, 0, -3]], [2.0, [0, 0, 0]]]);
  a.rot("right_leg", [[0, [0, 0, 0]], [0.25, [12, 0, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]], [1.25, [12, 0, 0]], [1.5, [0, 0, 0]], [2.0, [0, 0, 0]]]);
  // riff on the note block guitar, pitch = 2^((n-12)/12)
  const riff = [7, 11, 14, 11, 9, 12, 16, 14];
  riff.forEach((n, i) => a.sound(i * 0.25, "minecraft:block.note_block.guitar", { volume: 0.7, pitch: +Math.pow(2, (n - 12) / 12).toFixed(3) }));
  for (const t of [0, 0.5, 1.0, 1.5]) a.particle(t + 0.05, { particle: "minecraft:note", locator: "sound_hole", count: 1, spread: [0.2, 0.1, 0.1], velocity: [0, 0.05, 0] });
  return b;
}
