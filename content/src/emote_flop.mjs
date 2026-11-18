// Seal Flop – belly-flop onto the ground and slap your flippers like a happy seal.
import { emote } from "./_common.mjs";

export default function () {
  const b = emote("seal_flop", {
    name: { en: "Seal Flop", de: "Robben-Plumps" },
    description: { en: "Flop down like a seal and slap your flippers. Arf!", de: "Lass dich fallen wie eine Robbe und klatsch mit den Flossen. Arf!" },
    tags: ["seal", "funny", "sound", "starter"],
    rarity: "rare",
  });
  const a = b.anim("main", 3.4);
  // whole body: anticipation, dive, lie on the belly, bounce with the slaps, get back up
  a.rot("root", [[0, [0, 0, 0]], [0.35, [8, 0, 0]], [0.7, [-55, 0, 0]], [0.95, [-90, 0, 0]], [2.55, [-90, 0, 0]], [2.95, [-35, 0, 0]], [3.4, [0, 0, 0]]]);
  a.pos("root", [
    [0, [0, 0, 0]], [0.35, [0, 0, 0.5]], [0.7, [0, 4, -2]], [0.95, [0, 2, -3]],
    [1.2, [0, 2.8, -3]], [1.35, [0, 2, -3]], [1.6, [0, 2.8, -3]], [1.75, [0, 2, -3]], [2.0, [0, 2.8, -3]], [2.15, [0, 2, -3]],
    [2.55, [0, 2, -3]], [2.95, [0, 1, -1]], [3.4, [0, 0, 0]],
  ]);
  // flippers: arms along the body, slapping the ground
  const slap = (s) => [
    [0, [0, 0, 0]], [0.35, [-40, 0, s * 10]], [0.7, [-10, 0, s * 35]], [0.95, [0, 0, s * 25]],
    [1.15, [-50, 0, s * 45]], [1.35, [0, 0, s * 40]], [1.55, [-50, 0, s * 45]], [1.75, [0, 0, s * 40]], [1.95, [-50, 0, s * 45]], [2.15, [0, 0, s * 40]],
    [2.55, [-10, 0, s * 20]], [2.95, [-20, 0, s * 8]], [3.4, [0, 0, 0]],
  ];
  a.rot("right_arm", slap(1));
  a.rot("left_arm", slap(-1));
  // tail: legs together, lifted and wiggling
  const tail = (s) => [[0, [0, 0, 0]], [0.95, [-10, 0, 0]], [1.25, [-28, 0, s * 4]], [1.5, [-20, 0, -s * 4]], [1.75, [-28, 0, s * 4]], [2.0, [-20, 0, -s * 4]], [2.3, [-25, 0, 0]], [2.6, [-12, 0, 0]], [3.4, [0, 0, 0]]];
  a.rot("right_leg", tail(1));
  a.rot("left_leg", tail(-1));
  a.rot("head", [[0, [0, 0, 0]], [0.35, [-10, 0, 0]], [0.95, [55, 0, 0]], [1.4, [62, 12, 0]], [1.9, [62, -12, 0]], [2.4, [60, 0, 0]], [2.95, [20, 0, 0]], [3.4, [0, 0, 0]]]);
  a.sound(0.95, "minecraft:entity.player.small_fall", { volume: 0.8, pitch: 0.9 });
  for (const t of [1.35, 1.75, 2.15]) a.sound(t, "minecraft:entity.cod.flop", { volume: 0.9, pitch: 0.9 });
  a.sound(1.55, "minecraft:entity.dolphin.ambient", { volume: 0.6, pitch: 1.3 });
  return b;
}
