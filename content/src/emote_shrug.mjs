// Shrug – shoulders up, palms out, head tilt.
import { emote } from "./_common.mjs";

export default function () {
  const b = emote("shrug", {
    name: { en: "Shrug", de: "Schulterzucken" },
    description: { en: "Who knows? Not me.", de: "Wer weiß das schon? Ich nicht." },
    tags: ["reaction", "starter"],
  });
  const a = b.anim("main", 1.8);
  const up = [[0, [0, 0, 0]], [0.25, [0, 1, 0]], [1.2, [0, 1, 0]], [1.5, [0, 0, 0]], [1.8, [0, 0, 0]]];
  a.pos("right_arm", up).pos("left_arm", up);
  a.rot("right_arm", [[0, [0, 0, 0]], [0.25, [38, -12, 32]], [0.9, [36, -12, 30]], [1.25, [36, -12, 30]], [1.8, [0, 0, 0]]]);
  a.rot("left_arm", [[0, [0, 0, 0]], [0.25, [38, 12, -32]], [0.9, [36, 12, -30]], [1.25, [36, 12, -30]], [1.8, [0, 0, 0]]]);
  a.rot("head", [[0, [0, 0, 0]], [0.3, [6, 0, 14]], [0.9, [8, 4, 16]], [1.3, [4, 0, 10]], [1.8, [0, 0, 0]]]);
  return b;
}
