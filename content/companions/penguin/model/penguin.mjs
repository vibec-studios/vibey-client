import buildPolar from "../../../src/polar-pals/pet_penguin.mjs";
import { P } from "../../../src/polar-pals/_polar.mjs";
import { umbrella, sunglasses, fan, snowball, partyHat, jugglers, crest, sweat } from "./_props.mjs";
import { idle, sit, swim, swimIdle, held, sleep } from "./_anims_base.mjs";
import { umbrellaOpen, umbrellaTwirl, umbrellaClose, lookUp, shiver, fanning, snowPlay } from "./_anims_weather.mjs";
import { flap, slip, catchFish, worried, celebrate, preen, dance, land } from "./_anims_fun.mjs";
import { spin, bow, wave, juggle } from "./_anims_tricks.mjs";

const BASE = { umbA: "#ff5a5f", umbB: "#fff5f0", crest: null, crestDark: null };
for (const [k, v] of Object.entries(BASE)) if (!(k in P)) P[k] = v;

export const PROPS = ["umbrella", "fish", "fish2", "fish3", "scarf", "shades", "fan", "snowball", "party_hat", "sweat", "z1", "z2", "z3"];

export default build;

function build() {
  const b = buildPolar();
  b.meta.id = "companio:penguin";
  b.meta.name = { en: "Penguin", de: "Pinguin" };
  b.meta.description = { en: "Penguin companion model (companio penguin pack).", de: "Pinguin-Modell für den Companion (companio-Pinguinpaket)." };
  b.meta.tags = ["companion", "penguin"];
  const show = b.anims.get("idle");
  show.name = "idle_show";
  b.anims.delete("idle");
  b.anims.set("idle_show", show);
  umbrella(b);
  sunglasses(b);
  fan(b);
  snowball(b);
  partyHat(b);
  jugglers(b);
  crest(b);
  sweat(b);
  const makers = [idle, sit, swim, swimIdle, held, sleep, umbrellaOpen, umbrellaTwirl, umbrellaClose, lookUp, shiver, fanning, snowPlay,
    flap, slip, catchFish, worried, celebrate, preen, dance, land, spin, bow, wave, juggle];
  for (const make of makers) make(b);
  b.preview = preview();
  return b;
}

function preview() {
  const shows = {
    umbrella_open: ["umbrella"], umbrella_close: ["umbrella"], umbrella_twirl: ["umbrella"], fanning: ["fan", "shades", "sweat"],
    snow_play: ["snowball"], catch_fish: ["fish"], juggle: ["fish", "fish2", "fish3"], celebrate: ["party_hat"],
    shiver: ["scarf"], sleep: ["z1", "z2", "z3"], idle_show: ["fish"],
  };
  const times = { umbrella_open: 1.3, umbrella_close: 0.9, umbrella_twirl: 0.8, snow_play: 1.5, catch_fish: 1.3, juggle: 1.0, celebrate: 1.3, slip: 0.9, spin: 0.7, bow: 1.0, wave: 0.6, flap: 0.3, preen: 1.2, sleep: 1.0, idle_show: 7.6 };
  const layers = ["umbrella_open", "umbrella_twirl", "umbrella_close", "look_up", "fanning", "flap", "preen", "land"];
  const names = ["idle", "walk", "sprint", "sit", "swim", "swim_idle", "held", "sleep", "umbrella_open", "umbrella_close", "fanning", "shiver",
    "snow_play", "flap", "slip", "catch_fish", "worried", "celebrate", "preen", "dance", "spin", "bow", "wave", "juggle"];
  return {
    panels: () => names.map((n) => ({
      label: n, ownFit: true, player: false, cam: { yaw: n === "swim" || n === "sprint" ? 70 : 32, pitch: 12 }, petScale: 0.85, time: 0.6,
      hideBones: PROPS.filter((p) => !(shows[n] ?? []).includes(p)),
      anims: layers.includes(n) ? [{ name: "idle", t: 0.6 }, { name: n, t: times[n] ?? 0.6 }] : [{ name: n, t: times[n] ?? 0.6 }],
    })),
    sheet: { cols: 6 },
    thumb: { cam: { yaw: 28, pitch: 10 }, player: false, petScale: 1, time: 0.6, anims: [{ name: "idle", t: 0.6 }], hideBones: PROPS },
  };
}

const RECOLOR = {
  emperor: { pengBack: "#262c3a", pengBack2: "#36405a", pengBackLight: "#4a5676", pengBelly: "#fbf7ec", pengBellyShade: "#f3dc8e", blush: "#ffb02e", beak: "#3a3340", beakDark: "#ff9d2e", umbA: "#3f8cff", umbB: "#f2f7ff" },
  rockhopper: { pengBack: "#1e2230", pengBack2: "#2b3044", pengBackLight: "#3a4058", beak: "#ff6a3d", beakDark: "#d9442a", feet: "#ffa3b5", feetDark: "#e07b90", eyeIris: "#a3202f", crest: "#ffd23f", crestDark: "#f2a81d", umbA: "#ffd23f", umbB: "#2b3044" },
};

export function textures() {
  const out = {};
  for (const [name, colors] of Object.entries(RECOLOR)) {
    const saved = { ...P };
    Object.assign(P, colors);
    try {
      out[`penguin_${name}.png`] = Buffer.from(build().build().json.textures[0].png, "base64");
    } finally {
      for (const k of Object.keys(P)) if (!(k in saved)) delete P[k];
      Object.assign(P, saved);
    }
  }
  return out;
}
