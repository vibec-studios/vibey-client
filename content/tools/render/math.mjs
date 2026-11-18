// 4x4 matrices (row-major, column vectors: p' = M · p).
export const I = () => [1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];

export function mul(a, b) {
  const o = new Array(16);
  for (let r = 0; r < 4; r++)
    for (let c = 0; c < 4; c++) {
      let s = 0;
      for (let k = 0; k < 4; k++) s += a[r * 4 + k] * b[k * 4 + c];
      o[r * 4 + c] = s;
    }
  return o;
}

export const mulAll = (...ms) => ms.reduce((a, b) => mul(a, b));

export const T = (x, y, z) => [1, 0, 0, x, 0, 1, 0, y, 0, 0, 1, z, 0, 0, 0, 1];
export const S = (x, y = x, z = x) => [x, 0, 0, 0, 0, y, 0, 0, 0, 0, z, 0, 0, 0, 0, 1];
export function Rx(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [1, 0, 0, 0, 0, c, -s, 0, 0, s, c, 0, 0, 0, 0, 1];
}
export function Ry(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [c, 0, s, 0, 0, 1, 0, 0, -s, 0, c, 0, 0, 0, 0, 1];
}
export function Rz(a) {
  const c = Math.cos(a), s = Math.sin(a);
  return [c, -s, 0, 0, s, c, 0, 0, 0, 0, 1, 0, 0, 0, 0, 1];
}
/** Vanilla ModelPart order: rotationZYX(z, y, x) = Rz · Ry · Rx. Angles in radians. */
export const Rzyx = (x, y, z) => mul(mul(Rz(z), Ry(y)), Rx(x));

export function apply(m, p) {
  const [x, y, z] = p;
  return [m[0] * x + m[1] * y + m[2] * z + m[3], m[4] * x + m[5] * y + m[6] * z + m[7], m[8] * x + m[9] * y + m[10] * z + m[11]];
}
export function applyDir(m, p) {
  const [x, y, z] = p;
  return [m[0] * x + m[1] * y + m[2] * z, m[4] * x + m[5] * y + m[6] * z, m[8] * x + m[9] * y + m[10] * z];
}

export const dot = (a, b) => a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
export const cross = (a, b) => [a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]];
export const sub = (a, b) => [a[0] - b[0], a[1] - b[1], a[2] - b[2]];
export const add = (a, b) => [a[0] + b[0], a[1] + b[1], a[2] + b[2]];
export const scl = (a, s) => [a[0] * s, a[1] * s, a[2] * s];
export function norm(a) {
  const l = Math.hypot(a[0], a[1], a[2]) || 1;
  return [a[0] / l, a[1] / l, a[2] / l];
}
export const deg = (d) => (d * Math.PI) / 180;
