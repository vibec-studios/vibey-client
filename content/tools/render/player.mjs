// Neutral reference player for previews: vanilla part layout (spec §5 pivots), ORIGINAL simple skin.
import { Img, mix, shade } from "../canvas.mjs";
import { boxUV } from "../vibey.mjs";

/** Player parts in spec (Blockbench) coordinates. */
export const PLAYER_PARTS = [
  { name: "head", pivot: [0, 24, 0], from: [-4, 24, -4], to: [4, 32, 4], uv: [0, 0] },
  { name: "body", pivot: [0, 24, 0], from: [-4, 12, -2], to: [4, 24, 2], uv: [16, 16] },
  { name: "right_arm", pivot: [-5, 22, 0], from: [-8, 12, -2], to: [-4, 24, 2], uv: [40, 16] },
  { name: "left_arm", pivot: [5, 22, 0], from: [4, 12, -2], to: [8, 24, 2], uv: [32, 48] },
  { name: "right_leg", pivot: [-1.9, 12, 0], from: [-3.9, 0, -2], to: [0.1, 12, 2], uv: [0, 16] },
  { name: "left_leg", pivot: [1.9, 12, 0], from: [-0.1, 0, -2], to: [3.9, 12, 2], uv: [16, 48] },
];

const P = {
  skin: "#e2b28c",
  skinShade: "#cf9c78",
  hair: "#3b3f4c",
  hairHi: "#4a5060",
  eyeW: "#f4f4f5",
  eye: "#2c3140",
  mouth: "#b07a62",
  hoodie: "#566074",
  hoodieDark: "#4a5366",
  hoodieHi: "#66718a",
  cord: "#d9dbe2",
  pants: "#2f3442",
  pantsDark: "#272b37",
  shoe: "#1d1f27",
  sole: "#e9e9ee",
};

function faces(img, u, v, w, h, d) {
  const L = boxUV(u, v, w, h, d);
  const o = {};
  for (const [k, [u1, v1, u2, v2]] of Object.entries(L)) o[k] = img.view(u1, v1, u2 - u1, v2 - v1);
  return o;
}

export function playerSkin() {
  const img = new Img(64, 64);
  // ---------------------------------------------------------------- head
  const H = faces(img, 0, 0, 8, 8, 8);
  for (const k of ["north", "south", "east", "west"]) H[k].noisy(P.skin, 0.015, 11 + k.length);
  H.up.noisy(P.hair, 0.03, 3);
  H.down.noisy(P.skinShade, 0.015, 4);
  // hair: back fully, sides top 3 rows + back column, front fringe
  H.south.noisy(P.hair, 0.03, 5, 0, 0, 8, 6);
  for (const k of ["east", "west"]) {
    H[k].noisy(P.hair, 0.03, 6, 0, 0, 8, 3);
    const back = k === "west" ? 0 : 7; // west: u=0 at back (+Z); east: u=max at back
    H[k].col(back, P.hair, 0, 5).col(k === "west" ? 1 : 6, P.hair, 0, 4);
  }
  H.north.noisy(P.hair, 0.03, 7, 0, 0, 8, 2).px(P.hair, [[0, 2], [7, 2], [5, 2], [6, 2]]);
  H.north.px(P.hairHi, [[2, 0], [3, 0]]);
  H.north.px(P.eyeW, [[1, 4], [6, 4]]).px(P.eye, [[2, 4], [5, 4]]);
  H.north.px(P.skinShade, [[3, 5], [4, 5]]).px(P.mouth, [[3, 6], [4, 6]]);
  // ---------------------------------------------------------------- body (hoodie)
  const B = faces(img, 16, 16, 8, 12, 4);
  for (const k of ["north", "south", "east", "west"]) B[k].noisy(P.hoodie, 0.02, 20 + k.length);
  B.up.noisy(P.hoodie, 0.02, 21);
  B.down.noisy(P.hoodieDark, 0.02, 22);
  B.north.row(11, P.hoodieDark).row(10, P.hoodieDark);
  B.north.px(P.hoodieDark, [[3, 0], [4, 0], [3, 1], [4, 1]]); // neck
  B.north.px(P.hoodieHi, [[2, 0], [5, 0], [2, 1], [5, 1]]);
  B.north.rect(2, 6, 4, 3, P.hoodieDark).row(6, P.hoodieHi, 2, 5); // pocket
  B.south.row(11, P.hoodieDark).row(10, P.hoodieDark).rect(1, 0, 6, 3, P.hoodieHi); // hood
  for (const k of ["east", "west"]) B[k].row(11, P.hoodieDark).row(10, P.hoodieDark);
  // ---------------------------------------------------------------- arms
  for (const [u, v] of [[40, 16], [32, 48]]) {
    const A = faces(img, u, v, 4, 12, 4);
    for (const k of ["north", "south", "east", "west"]) {
      A[k].noisy(P.hoodie, 0.02, u + v + k.length);
      A[k].row(8, P.hoodieDark).row(9, P.hoodieDark);
      A[k].noisy(P.skin, 0.015, u + v + 1, 0, 10, 4, 2);
    }
    A.up.noisy(P.hoodie, 0.02, u + 2);
    A.down.noisy(P.skinShade, 0.015, u + 3);
  }
  // ---------------------------------------------------------------- legs
  for (const [u, v] of [[0, 16], [16, 48]]) {
    const L = faces(img, u, v, 4, 12, 4);
    for (const k of ["north", "south", "east", "west"]) {
      L[k].noisy(P.pants, 0.02, u + v + k.length);
      L[k].rect(0, 9, 4, 2, P.shoe).row(11, P.sole);
    }
    L.north.col(u === 0 ? 3 : 0, P.pantsDark, 0, 8);
    L.up.noisy(P.pants, 0.02, u + 4);
    L.down.noisy(shade(P.shoe, 0.9), 0.02, u + 5);
  }
  return img;
}

export const PLAYER_PALETTE = P;
export { mix };
