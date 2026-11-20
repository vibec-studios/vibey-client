import { cosmetic } from "../../../src/_common.mjs";
import { fur, rim, each, ellipse, rgba } from "../../../src/polar-pals/_polar.mjs";

const Z0 = [0, 0, 0];
const metal = (top, bottom, seed) => ({ all: (img) => rim(fur(img, { top, bottom, seed, strands: 0, grain: 0.03 }), 0.16, 0.9) });

function face(img) {
  rim(fur(img, { top: "#3a4356", bottom: "#2a3142", seed: 9, strands: 0, grain: 0.02 }), 0.12, 0.8);
  const w = img.width / 4;
  ellipse(img, w * 0.3, 2.6, 1.1, 1.1, "#7ff7ff");
  ellipse(img, w * 0.7, 2.6, 1.1, 1.1, "#7ff7ff");
  each(img, (u, v) => (v > 4.3 && v < 4.8 && u > w * 0.35 && u < w * 0.65 ? rgba("#7ff7ff") : null));
  return img;
}

export default function build() {
  const b = cosmetic("robot", {
    name: { en: "Robot", de: "Roboter" },
    description: { en: "Robot companion model (first version).", de: "Roboter-Modell (erste Version)." },
    tags: ["companion", "robot"],
    rarity: "common",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 1 } },
  });
  b.meta.id = "companio:robot";
  b.texture("main", { hd: 4 });
  b.bone("bot", { attach: "world", pivot: [0, 0, 0] });
  const body = b.bone("body", { parent: "bot", pivot: [0, 3, 0] });
  body.box([-4, 3, -3], [8, 7, 6], metal("#c9d3e3", "#8f9bb0", 1));
  body.box([-2, 5, -3.5], [4, 3, 1], metal("#5a6578", "#3f4859", 2));
  const head = b.bone("head", { parent: "body", pivot: [0, 10, 0] });
  head.box([-4, 10, -4], [8, 6, 8], { north: face, sides: metal("#d7e0ee", "#9aa6bb", 3).all, up: metal("#e5ecf6", "#c9d3e3", 4).all, down: metal("#8f9bb0", "#8f9bb0", 5).all });
  const antenna = b.bone("antenna", { parent: "head", pivot: [0, 16, 0] });
  antenna.box([-0.5, 16, -0.5], [1, 3, 1], metal("#8f9bb0", "#5a6578", 6));
  const lamp = b.bone("lamp", { parent: "antenna", pivot: [0, 19, 0], emissive: true });
  lamp.box([-1, 19, -1], [2, 2, 2], { all: (img) => rim(fur(img, { top: "#ffe37a", bottom: "#ffb547", seed: 7, strands: 0 }), 0.1, 0.6) });
  for (const [name, x] of [["arm_r", 4], ["arm_l", -5]]) {
    const arm = b.bone(name, { parent: "body", pivot: [x + 0.5, 9, 0] });
    arm.box([x, 4, -1], [1, 5, 2], metal("#b4bfd1", "#7f8ba0", 8));
  }
  for (const [name, x] of [["wheel_r", 1.5], ["wheel_l", -3.5]]) {
    const w = b.bone(name, { parent: "bot", pivot: [x + 1, 1.5, 0] });
    w.box([x, 0, -1.5], [2, 3, 3], metal("#3f4859", "#2a3142", 9));
  }
  b.locator("lamp", "lamp", [0, 21, 0]);
  b.locator("heart", "head", [0, 18, -2]);
  const idle = b.anim("idle", 3, true);
  idle.pos("body", [[0, Z0], [1.5, [0, 0.4, 0]], [3, Z0]]);
  idle.rot("head", [[0, Z0], [1.0, [0, 12, 0]], [2.0, [0, -12, 0]], [3, Z0]]);
  idle.rot("antenna", [[0, [0, 0, -6]], [1.5, [0, 0, 6]], [3, [0, 0, -6]]]);
  const walk = b.anim("walk", 0.5, true);
  walk.pos("body", [[0, Z0], [0.25, [0, 0.6, 0]], [0.5, Z0]]);
  walk.rot("wheel_r", [[0, Z0, "linear"], [0.5, [-360, 0, 0], "linear"]], "linear");
  walk.rot("wheel_l", [[0, Z0, "linear"], [0.5, [-360, 0, 0], "linear"]], "linear");
  walk.rot("arm_r", [[0, [20, 0, 0]], [0.25, [-20, 0, 0]], [0.5, [20, 0, 0]]]);
  walk.rot("arm_l", [[0, [-20, 0, 0]], [0.25, [20, 0, 0]], [0.5, [-20, 0, 0]]]);
  const spin = b.anim("spin", 1.2, false);
  spin.rot("bot", [[0, Z0, "linear"], [1.2, [0, 360, 0], "linear"]], "linear");
  spin.pos("bot", [[0, Z0], [0.6, [0, 3, 0]], [1.2, Z0]]);
  const wave = b.anim("wave", 1.2, false);
  wave.rot("arm_r", [[0, Z0], [0.2, [0, 0, 150]], [0.45, [0, 0, 120]], [0.7, [0, 0, 150]], [0.95, [0, 0, 120]], [1.2, Z0]]);
  b.preview = { thumb: { cam: { yaw: 35, pitch: 14 }, player: false, petScale: 1, time: 0.5, anims: [{ name: "idle", t: 0.5 }] } };
  return b;
}
