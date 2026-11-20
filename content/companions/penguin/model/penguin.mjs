import buildPenguin from "../../../src/polar-pals/pet_penguin.mjs";
import { fur, rim, each, rgba } from "../../../src/polar-pals/_polar.mjs";

const Z0 = [0, 0, 0];

function umbrella(b) {
  const u = b.bone("umbrella", { parent: "wing_r", pivot: [7, 8, -1] });
  const red = "#ff5a5f", white = "#fff5f0", stick = "#5b4636";
  u.box([6.5, 8, -1.5], [1, 22, 1], { all: (img) => fur(img, { top: stick, seed: 3, strands: 0 }) });
  u.box([-1, 30, -8], [16, 2, 14], {
    up: (img) => rim(each(img, (x, y) => rgba(Math.floor((x + y) / 2) % 2 ? red : white)), 0.12, 1),
    sides: (img) => fur(img, { top: red, seed: 4, strands: 0 }),
    down: (img) => fur(img, { top: "#c94a4e", seed: 5, strands: 0 }),
  });
  u.box([1, 32, -6], [12, 1, 10], { all: (img) => rim(each(img, (x) => rgba(Math.floor(x / 2) % 2 ? red : white)), 0.1, 0.8) });
}

export default function build() {
  const b = buildPenguin();
  b.meta.id = "companio:penguin";
  b.meta.name = { en: "Penguin", de: "Pinguin" };
  b.meta.tags = ["companion", "penguin"];
  umbrella(b);
  const hold = b.anim("umbrella_hold", 2, true);
  hold.rot("wing_r", [[0, [0, 0, 70]], [1, [4, 0, 72]], [2, [0, 0, 70]]]);
  hold.rot("head", [[0, [8, 0, 0]], [2, [8, 0, 0]]]);
  const flap = b.anim("flap", 0.8, false);
  flap.rot("wing_r", [[0, Z0], [0.2, [0, 0, 60]], [0.4, [0, 0, 10]], [0.6, [0, 0, 60]], [0.8, Z0]]);
  flap.rot("wing_l", [[0, Z0], [0.2, [0, 0, -60]], [0.4, [0, 0, -10]], [0.6, [0, 0, -60]], [0.8, Z0]]);
  return b;
}
