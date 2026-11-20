import { render, osc, noise, lowpass, honk } from "../../_synth.mjs";

function chirp(t, t0, len, f0, f1, o) {
  const u = t - t0;
  if (u < 0 || u > len) return 0;
  const env = Math.min(1, u * 300) * Math.exp(-Math.max(0, u - len * 0.4) * 22);
  return Math.sin(o(t, f0 + (f1 - f0) * Math.sin((Math.PI / 2) * (u / len)))) * env;
}

export const SOUNDS = {
  "sounds/squeak": () => {
    const a = osc(), b = osc(), c = osc();
    return render(0.42, (t) => chirp(t, 0, 0.12, 1500, 2300, a) + chirp(t, 0.13, 0.1, 1700, 2600, b) * 0.9 + chirp(t, 0.25, 0.14, 2000, 1500, c) * 0.8);
  },
  "sounds/peep": () => {
    const a = osc(), v = osc();
    return render(0.2, (t) => Math.sin(a(t, 1900 + 500 * Math.min(1, t / 0.08) + 40 * Math.sin(v(t, 30)))) * Math.min(1, t * 250) * Math.exp(-t * 14));
  },
  "sounds/bray": () => {
    const a = [...Array(6)].map(() => osc()), b = [...Array(6)].map(() => osc()), c = [...Array(6)].map(() => osc());
    const n = noise(5), lp = lowpass(0.3);
    return render(0.9, (t) => honk(t, 0, 0.16, 620, 700, a) + honk(t, 0.22, 0.16, 640, 720, b) + honk(t, 0.46, 0.38, 700, 520, c) + lp(n()) * 0.05);
  },
  "sounds/coo": () => {
    const o = osc(), n = noise(9), lp = lowpass(0.04);
    return render(1.6, (t) => {
      const breath = Math.sin(Math.PI * Math.min(1, t / 1.5)) ** 2;
      return (0.5 * Math.sin(o(t, 520 + 60 * Math.sin(t * 3))) * (t < 0.7 ? 1 : 0.2) + lp(n()) * 2) * breath;
    }, 0.5);
  },
};
