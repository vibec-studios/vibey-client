// Facepalm – hand to the face, a slow head shake and a sigh.
import { emote, aim, SHOULDER } from "./_common.mjs";

export default function () {
  const b = emote("facepalm", {
    name: { en: "Facepalm", de: "Facepalm" },
    description: { en: "When words are not enough.", de: "Wenn Worte nicht mehr reichen." },
    tags: ["reaction", "funny", "starter"],
  });
  const face = aim(SHOULDER.right, [0.5, 27, -8]);
  const press = [face[0] + 4, face[1] + 3, 0];
  const a = b.anim("main", 2.8);
  a.rot("right_arm", [[0, [0, 0, 0]], [0.3, [face[0] - 30, face[1] - 10, 0]], [0.45, face], [0.55, press], [2.1, press], [2.45, [25, 10, 0]], [2.8, [0, 0, 0]]]);
  a.rot("head", [[0, [0, 0, 0]], [0.45, [-8, 0, 0]], [0.6, [-16, 0, 0]], [1.0, [-16, 12, 0]], [1.4, [-16, -12, 0]], [1.8, [-16, 8, 0]], [2.1, [-12, 0, 0]], [2.5, [-4, 0, 0]], [2.8, [0, 0, 0]]]);
  a.rot("left_arm", [[0, [0, 0, 0]], [0.5, [-12, 0, -22]], [2.2, [-12, 0, -22]], [2.8, [0, 0, 0]]]);
  a.rot("root", [[0, [0, 0, 0]], [0.6, [-4, 0, 0]], [2.2, [-4, 0, 0]], [2.8, [0, 0, 0]]]);
  a.sound(0.52, "minecraft:entity.player.attack.weak", { volume: 0.6, pitch: 1.4 });
  a.sound(1.0, "minecraft:entity.villager.no", { volume: 0.5, pitch: 1.15 });
  return b;
}
