// Sit – sit down and chill, leaning back on your hands (loop).
import { emote } from "./_common.mjs";

export default function () {
  const b = emote("sit", {
    name: { en: "Sit", de: "Hinsetzen" },
    description: { en: "Take a seat and chill for a while. Move to get up.", de: "Setz dich hin und entspann dich. Bewegen zum Aufstehen." },
    tags: ["chill", "loop", "starter"],
    preview: { cam: { yaw: 55, pitch: 14 } },
  }, { loop: true });
  const a = b.anim("main", 4.0, true);
  a.pos("root", [[0, [0, -10, 0]], [4, [0, -10, 0]]]);
  a.rot("right_leg", [[0, [88, -6, 0]], [2, [86, -9, 0]], [4, [88, -6, 0]]]);
  a.rot("left_leg", [[0, [88, 6, 0]], [2, [86, 9, 0]], [4, [88, 6, 0]]]);
  a.rot("right_arm", [[0, [-32, 0, 12]], [2, [-30, 0, 13]], [4, [-32, 0, 12]]]);
  a.rot("left_arm", [[0, [-32, 0, -12]], [2, [-30, 0, -13]], [4, [-32, 0, -12]]]);
  a.rot("body", [[0, [-3, 0, 0]], [2, [-4, 0, 0]], [4, [-3, 0, 0]]]);
  a.rot("head", [[0, [6, 0, 0]], [0.9, [8, 22, 0]], [1.7, [8, 22, 0]], [2.4, [2, -16, 3]], [3.2, [4, -10, 0]], [4, [6, 0, 0]]]);
  return b;
}
