import buildSeal from "../../../src/polar-pals/pet_seal.mjs";
import { fur, rim, each, stroke, ellipse, mix, rgba, P } from "../../../src/polar-pals/_polar.mjs";
import { idle, sit, swim, swimIdle, held, yawn } from "./_anims_base.mjs";
import { clap, bark, shiver, shake, lookUp, wave } from "./_anims_layers.mjs";
import { slide, trickBall, roll, beg } from "./_anims_tricks.mjs";

const solid = (top, bottom, seed) => ({ all: (img) => rim(fur(img, { top, bottom: bottom ?? top, seed, strands: 0.0, grain: 0.015 }), 0.14, 0.8) });

function rainHat(b) {
  const hat = b.bone("rain_hat", { parent: "head", pivot: [0, 17, -10], rotation: [-6, 0, 4] });
  const yellow = "#ffd23f", deep = "#e8a91c";
  hat.box([-4, 17, -13], [8, 3, 7], {
    up: (img) => rim(fur(img, { top: yellow, bottom: deep, seed: 80, strands: 0, grain: 0.02 }), 0.12, 0.8),
    sides: (img) => {
      rim(fur(img, { top: yellow, bottom: deep, seed: 81, strands: 0, grain: 0.02 }), 0.12, 0.8);
      each(img, (u, v) => (v > 2.1 && v < 2.7 ? rgba("#c9781a") : null));
      return img;
    },
    down: (img) => fur(img, { top: deep, seed: 82 }),
  });
  hat.box([-6, 16.4, -15], [12, 1, 12], {
    up: (img) => rim(fur(img, { top: yellow, bottom: mix(yellow, deep, 0.5), seed: 83, strands: 0, grain: 0.02 }), 0.18, 1.2),
    down: (img) => rim(fur(img, { top: deep, bottom: "#c9781a", seed: 84, strands: 0 }), 0.12, 0.8),
    sides: (img) => fur(img, { top: deep, seed: 85 }),
  });
  hat.box([-5, 13.4, -3.4], [10, 3, 1], solid(yellow, deep, 86));
}

function beachBall(b) {
  const ball = b.bone("ball", { parent: "head", pivot: [0, 14.5, -16] });
  const stripes = ["#ff5a5f", "#ffffff", "#3fa9f5", "#ffffff", "#ffd23f", "#ffffff"];
  const paint = (seed) => (img) => {
    const w = img.width / 4;
    each(img, (u) => rgba(stripes[Math.floor((u / w) * 6) % 6]));
    return rim(img, 0.1, 0.7);
  };
  const top = (img) => {
    each(img, () => rgba("#ffffff"));
    ellipse(img, img.width / 8, img.height / 8, 0.9, 0.9, "#ff5a5f");
    return rim(img, 0.08, 0.6);
  };
  ball.box([-3, 11.5, -19], [6, 6, 6], { sides: paint(90), up: top, down: top }, {});
  ball.box([-2, 11.2, -18], [4, 1, 4], { all: top });
  ball.box([-2, 17.0, -18], [4, 1, 4], { all: top });
}

function scarf(b) {
  const s = b.bone("scarf", { parent: "body", pivot: [0, 6, -6] });
  const red = "#e2475a", white = "#fbf3ef";
  const knit = (img) => {
    each(img, (u, v) => rgba(Math.floor(u / 1.5) % 2 ? red : white));
    each(img, (u, v) => ((u * 3 + v * 2) % 1 < 0.18 ? [0, 0, 0, 22] : null));
    return rim(img, 0.12, 0.6);
  };
  s.box([-7, 5, -8], [14, 3, 4], { all: knit }, { inflate: 0.25 });
  s.box([3, 0.5, -8.6], [3, 5, 1], { all: knit }, { rotation: [8, 0, -10], pivot: [4.5, 5.5, -8.2] });
}

export default build;

function build() {
  const b = buildSeal();
  b.meta.id = "companio:seal";
  b.meta.name = { en: "Seal", de: "Robbe" };
  b.meta.description = { en: "Seal companion model (companio seal pack).", de: "Robben-Modell für den Companion (companio-Robbenpaket)." };
  b.meta.tags = ["companion", "seal"];
  const show = b.anims.get("idle");
  show.name = "idle_show";
  b.anims.delete("idle");
  b.anims.set("idle_show", show);
  rainHat(b);
  beachBall(b);
  scarf(b);
  b.locator("ball", "ball", [0, 14.5, -16]);
  b.locator("hat", "rain_hat", [0, 21, -10]);
  for (const make of [idle, sit, swim, swimIdle, held, yawn, clap, bark, shiver, shake, lookUp, wave, slide, trickBall, roll, beg]) make(b);
  b.preview = {
    panels: () => [
      ...["idle", "sit", "swim", "swim_idle", "held", "clap", "bark", "shiver", "shake", "slide", "trick_ball", "roll", "beg", "wave", "look_up", "yawn"].map((n) => ({
        label: n, ownFit: true, player: false, cam: { yaw: 35, pitch: 14 }, petScale: 0.85,
        anims: [{ name: n === "clap" || n === "bark" || n === "shiver" || n === "wave" || n === "look_up" || n === "yawn" ? "idle" : n, t: 0.6 }, ...(["clap", "bark", "shiver", "wave", "look_up", "yawn"].includes(n) ? [{ name: n, t: 0.45 }] : [])],
        time: 0.6,
      })),
    ],
    sheet: { cols: 4 },
  };
  return b;
}

const RECOLOR = {
  arctic: { sealTop: "#e9eef4", seal: "#f3f6f9", sealSide: "#eef2f6", sealLow: "#fbfcfd", sealDark: "#b8c3cf", flipper: "#d5dde6", flipperDark: "#aab6c3", spot: "#dfe6ee" },
  harbor: { sealTop: "#6e6152", seal: "#857766", sealSide: "#7b6e5e", sealLow: "#a39584", sealDark: "#4e4337", flipper: "#5f5446", flipperDark: "#463c31", spot: "#5a4e40", belly: "#d8cdbf", bellyShade: "#c2b5a5" },
};

export function textures() {
  const out = {};
  for (const [name, colors] of Object.entries(RECOLOR)) {
    const saved = { ...P };
    Object.assign(P, colors);
    try {
      const png = Buffer.from(build().build().json.textures[0].png, "base64");
      out[`seal_${name}.png`] = png;
    } finally {
      Object.assign(P, saved);
    }
  }
  return out;
}
