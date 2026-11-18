// Vibe Dance – a looping four-beat groove (120 bpm): sway, hop and alternating disco arms.
import { emote } from "./_common.mjs";

export default function () {
  const b = emote("vibe_dance", {
    name: { en: "Vibe Dance", de: "Vibe-Tanz" },
    description: { en: "A looping groove at 120 bpm. Move to stop.", de: "Ein Groove in Dauerschleife mit 120 bpm. Bewegen zum Beenden." },
    tags: ["dance", "loop", "music", "starter"],
  }, { loop: true });
  const a = b.anim("main", 2.0, true);
  // beats at 0 / 0.5 / 1.0 / 1.5, hops on the off-beats
  a.pos("root", [[0, [0, 0, 0]], [0.25, [0.6, 1.1, 0]], [0.5, [1.2, 0, 0]], [0.75, [0.6, 1.1, 0]], [1.0, [0, 0, 0]], [1.25, [-0.6, 1.1, 0]], [1.5, [-1.2, 0, 0]], [1.75, [-0.6, 1.1, 0]], [2.0, [0, 0, 0]]]);
  a.rot("root", [[0, [0, 0, 0]], [0.5, [0, 12, -4]], [1.0, [0, 0, 0]], [1.5, [0, -12, 4]], [2.0, [0, 0, 0]]]);
  a.rot("right_arm", [[0, [0, 0, 18]], [0.5, [10, 0, 155]], [1.0, [0, 0, 18]], [1.5, [70, 20, 0]], [2.0, [0, 0, 18]]]);
  a.rot("left_arm", [[0, [70, -20, 0]], [0.5, [0, 0, -18]], [1.0, [10, 0, -155]], [1.5, [0, 0, -18]], [2.0, [70, -20, 0]]]);
  a.rot("head", [[0, [-6, 0, 0]], [0.25, [5, 0, 3]], [0.5, [-6, 8, 0]], [0.75, [5, 0, -3]], [1.0, [-6, 0, 0]], [1.25, [5, 0, 3]], [1.5, [-6, -8, 0]], [1.75, [5, 0, -3]], [2.0, [-6, 0, 0]]]);
  a.rot("right_leg", [[0, [0, 0, 0]], [0.25, [22, 0, 4]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]], [1.25, [-8, 0, 0]], [1.5, [0, 0, 0]], [2.0, [0, 0, 0]]]);
  a.rot("left_leg", [[0, [0, 0, 0]], [0.25, [-8, 0, 0]], [0.5, [0, 0, 0]], [1.0, [0, 0, 0]], [1.25, [22, 0, -4]], [1.5, [0, 0, 0]], [2.0, [0, 0, 0]]]);
  return b;
}
