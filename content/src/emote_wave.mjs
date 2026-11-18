// Wave – a friendly overhead wave with the right arm.
import { emote } from "./_common.mjs";

export default function () {
  const b = emote("wave", {
    name: { en: "Wave", de: "Winken" },
    description: { en: "Say hi! A big friendly wave.", de: "Sag hallo! Ein großes, freundliches Winken." },
    tags: ["greeting", "friendly", "starter"],
  });
  const a = b.anim("main", 2.2);
  a.rot("right_arm", [
    [0, [0, 0, 0]], [0.35, [10, 0, 150]],
    [0.6, [10, 0, 128]], [0.85, [10, 0, 158]], [1.1, [10, 0, 128]], [1.35, [10, 0, 158]], [1.6, [10, 0, 135]],
    [1.95, [0, 0, 8]], [2.2, [0, 0, 0]],
  ]);
  a.rot("left_arm", [[0, [0, 0, 0]], [0.4, [-6, 0, -6]], [1.7, [-6, 0, -6]], [2.2, [0, 0, 0]]]);
  a.rot("head", [[0, [0, 0, 0]], [0.4, [6, -10, -6]], [0.85, [8, -12, -8]], [1.35, [4, -8, -4]], [1.8, [2, -4, 0]], [2.2, [0, 0, 0]]]);
  a.rot("body", [[0, [0, 0, 0]], [0.4, [0, -4, -2]], [1.7, [0, -4, -2]], [2.2, [0, 0, 0]]]);
  return b;
}
