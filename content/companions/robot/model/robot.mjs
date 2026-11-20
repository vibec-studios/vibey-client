import { cosmetic, K } from "../../../src/polar-pals/_polar.mjs";
import { R, VARIANTS } from "./_palette.mjs";
import { body, head, lamp, beam, umbrella, handProps } from "./_parts.mjs";
import { idle, walk, sprint, hover, sit, powerDown, lowPower, held } from "./_anims_base.mjs";
import { beep, wave, joy, scan, umbrellaPop, umbrellaHold, shake, glitch, solarOpen, shiver, fret, tilt } from "./_anims_layers.mjs";
import { polish, gauge, vent, carryTorch, flagWave } from "./_anims_chores.mjs";
import { spin, jumpBoost, salute, blinkPattern, fold, dance } from "./_anims_tricks.mjs";

export const FACE_PROPS = ["face_happy", "face_heart", "face_error", "face_sleep", "face_scan", "face_wow", "face_sad", "face_cool"];
export const PROP_BONES = [...FACE_PROPS, "bulb_happy", "bulb_low", "bulb_alert", "lamp", "lamp_glow", "lamp_pool", "beam", "umbrella", "torch", "flag"];

const ANIMS = [idle, walk, sprint, hover, sit, powerDown, lowPower, held, beep, wave, joy, scan, umbrellaPop, umbrellaHold, shake, glitch,
  solarOpen, shiver, fret, tilt, polish, gauge, vent, carryTorch, flagWave, spin, jumpBoost, salute, blinkPattern, fold, dance];

const LAYERS = ["beep", "wave", "joy", "scan", "umbrella_pop", "umbrella_hold", "shake", "solar_open", "shiver", "fret", "tilt", "polish", "gauge", "vent", "carry_torch", "flag_wave"];
const SHOWS = {
  scan: ["beam", "face_scan"], umbrella_pop: ["umbrella"], umbrella_hold: ["umbrella"], carry_torch: ["torch", "lamp", "lamp_glow", "lamp_pool"],
  flag_wave: ["flag", "face_happy", "bulb_happy"], joy: ["face_happy", "bulb_happy"], glitch: ["face_error", "bulb_alert"], power_down: ["face_sleep"],
  low_power: ["face_sleep", "bulb_low"], fret: ["face_sad", "bulb_alert"], dance: ["face_happy"], vent: ["face_cool"], held: ["face_wow"], wave: ["face_heart"],
};
const PANELS = ["idle", "walk", "sprint", "hover", "sit", "power_down", "held", "joy", "wave", "scan", "umbrella_pop", "solar_open", "glitch", "carry_torch",
  "flag_wave", "vent", "fret", "polish", "gauge", "spin", "jump_boost", "salute", "fold", "dance"];
const TIMES = { umbrella_pop: 0.85, solar_open: 1.55, scan: 1.0, fold: 1.4, jump_boost: 1.1, salute: 0.8, spin: 0.5, polish: 1.0, gauge: 1.0 };

function build() {
  const b = cosmetic("robot", {
    name: { en: "Robot", de: "Roboter" },
    description: { en: "Little robot companion model (companio robot pack).", de: "Kleines Roboter-Modell für den Companion (companio-Roboterpaket)." },
    tags: ["companion", "robot"],
    rarity: "epic",
    cosmetic: { slot: "pet", pet: { follow: "ground", scale: 0.72 } },
  });
  b.meta.id = "companio:robot";
  b.texture("main", { hd: K });
  body(b);
  head(b);
  lamp(b);
  beam(b);
  umbrella(b);
  handProps(b);
  b.locator("heart", "head", [0, 26, -2]);
  b.locator("eye", "head", [0, 16, -5.2]);
  b.locator("lamp", "head", [0, 20, -6]);
  b.locator("antenna", "antenna", [0, 24, 0]);
  b.locator("jet", "body", [0, 3, 4.5]);
  b.locator("chest", "body", [0, 8, -3.8]);
  b.locator("feet", "bot", [0, 0.3, 0]);
  b.locator("flame", "torch_flame", [5, 14, 0]);
  b.locator("umbrella", "umbrella_top", [0, 27, 3]);
  b.locator("hand_r", "arm_r", [5, 4.5, 0]);
  for (const make of ANIMS) make(b);
  const panel = (n) => ({
    label: n, ownFit: true, player: false, cam: { yaw: n === "solar_open" || n === "jump_boost" ? 150 : 32, pitch: n === "solar_open" ? 38 : 14 }, petScale: 0.72, time: TIMES[n] ?? 0.6,
    hideBones: PROP_BONES.filter((p) => !(SHOWS[n] ?? []).includes(p)), fitParticles: false,
    anims: LAYERS.includes(n) ? [{ name: "idle", t: 0.6 }, { name: n, t: TIMES[n] ?? 0.6 }] : [{ name: n, t: TIMES[n] ?? 0.6 }],
  });
  b.preview = {
    panels: () => PANELS.map(panel),
    sheet: { cols: 6 },
    thumb: { cam: { yaw: 28, pitch: 12 }, player: false, petScale: 1, time: 0.3, anims: [{ name: "idle", t: 0.3 }], hideBones: PROP_BONES.filter((p) => p !== "face_happy" && p !== "bulb_happy") },
  };
  return b;
}

export default build;

export function textures() {
  const out = {};
  for (const [name, colors] of Object.entries(VARIANTS)) {
    const saved = { ...R };
    Object.assign(R, colors);
    try {
      out[`robot_${name}.png`] = Buffer.from(build().build().json.textures[0].png, "base64");
    } finally {
      Object.assign(R, saved);
    }
  }
  return out;
}
