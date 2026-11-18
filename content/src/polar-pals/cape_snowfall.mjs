// Polar Pals · Snowfall Cape – an HD (4×) animated cape: the seal pup and the penguin sit on their ice floe under a
// rippling aurora while snow falls; the penguin waves now and then and the seal blinks. 8 frames, seamless loop.
// Lining: purple fleece with teal snowflakes. The elytra get the same night sky.
import { K, P, cosmetic, each, ellipse, stroke, blush, mix, rgba, hash, vnoise } from "./_polar.mjs";

const FRAMES = 8;

/** HD view of a UV rectangle of the 64×32 cape layout. */
const V = (img, x, y, w, h) => img.view(x * K, y * K, w * K, h * K);

/** Night sky with aurora, stars and falling snow into a view (coordinates in texture pixels of that view). */
function sky(img, frame, o = {}) {
  const Wp = img.width, Hp = img.height;
  const ph = (frame / FRAMES) * Math.PI * 2;
  for (let y = 0; y < Hp; y++)
    for (let x = 0; x < Wp; x++) {
      const t = y / Hp;
      let c = mix("#0a0d24", "#2a1d66", Math.min(1, t * 1.3));
      // aurora ribbon: a wavy band with vertical rays
      const cy = Hp * (o.band ?? 0.28) + Math.sin(x * 0.16 + ph) * Hp * 0.05 + Math.sin(x * 0.05 - ph) * Hp * 0.03;
      const d = (cy - y) / (Hp * 0.22); // > 0 above the band hem
      if (d > -0.08 && d < 1) {
        const ray = vnoise(x * 0.35 - frame * 0.8, d * 2, 5) * 0.7 + 0.3;
        const k = (d < 0 ? 1 + d / 0.08 : 1 - d) * ray;
        let a = mix(P.auroraG, P.auroraT, Math.min(1, Math.max(0, d) * 2));
        a = mix(a, P.auroraP, Math.max(0, d - 0.4) / 0.6);
        c = mix(c, a, Math.max(0, Math.min(0.9, k * 0.95)));
      }
      // stars (twinkle with the frame)
      const h = hash(x, y, 3);
      if (h > 0.985 && y < Hp * 0.6) c = mix(c, "#ffffff", 0.35 + 0.65 * (0.5 + 0.5 * Math.sin(ph * 2 + h * 40)));
      img.set(x, y, c);
    }
  return img;
}

/** Falling snow, periodic every 16 px vertically, shifted 2 px per frame (seamless over 8 frames). */
function snow(img, frame, seed = 9) {
  const Wp = img.width, Hp = img.height;
  for (let y = 0; y < Hp; y++)
    for (let x = 0; x < Wp; x++) {
      const yy = (((y - frame * 2) % 16) + 16) % 16;
      const h = hash(x, yy, seed);
      if (h > 0.98) img.blend(x, y, "#ffffff", 0.95);
      else if (h > 0.973) img.blend(x, y, "#dce9ff", 0.55);
    }
  return img;
}

/** The scene on the outer cape face (40×64 px). */
function scene(img, frame) {
  sky(img, frame);
  const Wp = img.width, Hp = img.height;
  // distant snowy peaks
  for (let x = 0; x < Wp; x++) {
    const top = 42 + Math.abs(((x + 6) % 18) - 9) * 0.9 + Math.sin(x * 0.7) * 0.6;
    for (let y = Math.floor(top); y < Hp; y++) img.set(x, y, mix("#232c58", "#161d40", (y - top) / 12));
    for (let y = Math.floor(top); y < top + 1.5; y++) img.blend(x, y, "#8ea4dd", 0.5);
  }
  // soft moonlight glow behind the pals so they read against the night
  blush(img, 19, 42, 17, 8, "#a9bcff", 0.32, 1);
  // water + ice floe
  for (let y = 52; y < Hp; y++) for (let x = 0; x < Wp; x++) img.set(x, y, mix("#16244d", "#0c1430", (y - 52) / 12));
  for (let x = 0; x < Wp; x++) if (hash(x, frame, 4) > 0.8) img.blend(x, 54 + (x % 3), "#7fa8e8", 0.5);
  for (let y = 0; y < 8; y++)
    for (let x = 2; x < 38; x++) {
      const inside = Math.abs(x - 20) < 17 - (y > 5 ? (y - 5) * 2 : 0) - (y < 1 ? 1 : 0);
      if (!inside) continue;
      img.set(x, 49 + y, y < 2 ? mix("#ffffff", P.snow, y / 2) : y < 5 ? mix(P.snowShade, P.snowDeep, (y - 2) / 3) : mix(P.iceMid, P.iceDeep, (y - 5) / 3));
    }
  // seal pup (left): lying, head raised
  const sealC = mix(P.seal, P.sealTop, 0.3);
  ellipse(img, 13, 46.5, 7.7, 3.9, "#3c4560", 1, 1);
  ellipse(img, 9, 42.5, 4.9, 4.7, "#3c4560", 1, 1);
  ellipse(img, 13, 46.5, 7, 3.2, sealC, 1, 1);
  ellipse(img, 13, 48, 5.5, 1.4, P.belly, 1, 1);
  ellipse(img, 9, 42.5, 4.2, 4, sealC, 1, 1);
  ellipse(img, 20, 47.5, 2.4, 1.2, P.flipperDark, 1, 1);
  const blink = frame === 5;
  for (const ex of [7.3, 10.7]) {
    if (blink) img.set(Math.round(ex), 42, P.eye).set(Math.round(ex) - 1, 42, P.eye);
    else {
      ellipse(img, ex, 41.7, 1.05, 1.25, P.eye, 1, 1);
      img.set(Math.round(ex) - 1, 41, "#ffffff");
    }
  }
  ellipse(img, 9, 44.3, 1.6, 1.0, P.muzzle, 1, 1);
  img.set(9, 43, P.nose).set(8, 43, P.nose);
  img.blend(6, 44, P.blush, 0.8).blend(12, 44, P.blush, 0.8);
  // penguin (right): standing on the floe
  ellipse(img, 28, 43, 4.3, 6.3, "#7d8fc4", 0.8, 1);
  ellipse(img, 28, 37.3, 3.7, 3.6, "#7d8fc4", 0.8, 1);
  ellipse(img, 28, 43, 3.6, 5.6, P.pengBack, 1, 1);
  ellipse(img, 28, 44.2, 2.4, 4.1, P.pengBelly, 1, 1);
  ellipse(img, 28, 37.3, 3, 2.9, P.pengBack, 1, 1);
  ellipse(img, 28, 37.9, 2.1, 1.9, P.pengBelly, 1, 1);
  img.set(27, 37, P.eye).set(29, 37, P.eye);
  img.set(28, 38, P.beak).set(28, 39, P.beakDark);
  img.blend(26, 39, P.blush, 0.7).blend(30, 39, P.blush, 0.7);
  for (let x = 25; x <= 31; x++) img.set(x, 40, x % 2 ? P.teal : P.brand);
  img.set(30, 41, P.teal).set(30, 42, P.brand);
  img.set(26, 48, P.feet).set(27, 48, P.feet).set(29, 48, P.feet).set(30, 48, P.feet);
  // left flipper rests, right flipper waves in frames 2–5
  stroke(img, [[24.8, 41], [24.2, 45]], 1.1, P.pengBack, 1, 1);
  const wave = [0, 0, 1, 2, 1, 2, 0, 0][frame];
  const tips = [[32.2, 45], [33.8, 39.5], [33.2, 37.2]];
  stroke(img, [[31.3, 41], tips[wave]], 1.9, "#7d8fc4", 0.8, 1);
  stroke(img, [[31.3, 41], tips[wave]], 1.1, P.pengBack, 1, 1);
  if (wave) for (const [dx, dy] of [[1.6, -1.5], [2.2, -0.2]]) img.blend(Math.round(tips[wave][0] + dx), Math.round(tips[wave][1] + dy), P.tealLight, 0.8);
  snow(img, frame);
  return img;
}

function lining(img, seed = 3) {
  const Wp = img.width, Hp = img.height;
  for (let y = 0; y < Hp; y++)
    for (let x = 0; x < Wp; x++) {
      const n = vnoise(x * 0.4, y * 0.4, seed) - 0.5;
      img.set(x, y, mix(P.brandDeep, P.brandDark, 0.4 + n * 0.5 + (hash(x, y, seed) - 0.5) * 0.12));
    }
  // little teal snowflakes on a diagonal grid
  for (let cy = 6; cy < Hp; cy += 16)
    for (let cx = ((cy / 16) % 2 ? 4 : 12); cx < Wp; cx += 16) {
      for (let k = 0; k < 3; k++) {
        const a = (k * Math.PI) / 3;
        stroke(img, [[cx - Math.cos(a) * 3, cy - Math.sin(a) * 3], [cx + Math.cos(a) * 3, cy + Math.sin(a) * 3]], 0.9, P.teal, 0.85, 1);
      }
      img.set(cx, cy, P.tealLight);
    }
  return img;
}

export default function () {
  const b = cosmetic("polar_snowfall_cape", {
    name: { en: "Polar Pals · Snowfall Cape", de: "Polar Pals · Schneefall-Umhang" },
    description: {
      en: "HD animated cape: the seal pup and the penguin sit on their ice floe under a rippling aurora while snow falls. The penguin waves, the seal blinks. Purple fleece lining with teal snowflakes.",
      de: "Animierter HD-Umhang: Robbenbaby und Pinguin sitzen auf ihrer Eisscholle unter wogendem Polarlicht, während Schnee fällt. Der Pinguin winkt, die Robbe blinzelt. Lila Fleecefutter mit türkisen Schneeflocken.",
    },
    tags: ["polar-pals", "cape", "aurora", "seal", "penguin", "animated"],
    rarity: "rare",
    cosmetic: { slot: "cape", cape: { texture: "cape" } },
    preview: {
      panels: (p) => [
        { ...p[0], label: "back · frame 0", time: 0 },
        { ...p[0], label: "back · frame 3 (wave)", time: 0.6 },
        { ...p[1], label: "front" },
        { label: "texture (frame 0)", texture: "cape", textureScale: 4 },
      ],
      sheet: { cols: 4, panelW: 290, panelH: 330 },
    },
  });
  b.texture("cape", { width: 64, height: 32, hd: K, frames: FRAMES, frameTime: 3 }).paint((img, frame) => {
    img.fill([0, 0, 0, 0]);
    scene(V(img, 1, 1, 10, 16), frame);
    lining(V(img, 12, 1, 10, 16));
    // edges: top/bottom/sides of the cape
    for (const [x, y, w, h] of [[1, 0, 10, 1], [11, 0, 10, 1], [0, 1, 1, 16], [11, 1, 1, 16]]) lining(V(img, x, y, w, h), 5);
    // elytra (vanilla uses the cape texture): the whole elytra block (22,0)–(46,22) gets the night sky, faces and
    // edges alike – in game the gliding elytra's visible faces sample more than the two big 10×20 faces
    const e = V(img, 22, 0, 24, 22);
    sky(e, frame, { band: 0.3 });
    snow(e, frame, 13);
  });
  return b;
}
