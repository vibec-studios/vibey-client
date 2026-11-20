import { render, osc, noise, lowpass, honk } from "../../_synth.mjs";

export const SOUNDS = {
  "sounds/bark": () => {
    const a = [...Array(6)].map(() => osc()), b = [...Array(6)].map(() => osc());
    const n = noise(3), lp = lowpass(0.25);
    return render(0.62, (t) => honk(t, 0.0, 0.2, 330, 250, a) + honk(t, 0.27, 0.24, 360, 240, b) + lp(n()) * 0.08 * (t < 0.55 ? 1 : 0));
  },
  "sounds/snore": () => {
    const n = noise(7), lp = lowpass(0.02), o = osc();
    return render(1.8, (t) => {
      const breath = Math.sin(Math.PI * Math.min(1, t / 1.7)) ** 2;
      return (lp(n()) * 3 + 0.15 * o(t, 70 + 8 * Math.sin(t * 9))) * breath;
    }, 0.6);
  },
  "sounds/chirp": () => {
    const o1 = osc(), o2 = osc();
    return render(0.34, (t) => {
      const a = t < 0.14 ? Math.sin(o1(t, 900 + 700 * (t / 0.14))) * Math.min(1, t * 200) * Math.exp(-t * 6) : 0;
      const u = t - 0.16;
      const b = u > 0 ? Math.sin(o2(t, 1200 + 900 * (u / 0.16))) * Math.min(1, u * 200) * Math.exp(-u * 10) : 0;
      return a + b;
    });
  },
};
