// Generates src/main/resources/assets/vibeyclient/textures/font/icons.png: the bitmap glyphs of the
// "vibeyclient:icons" font (nametag / tab list icons). Original pixel art, 8x8 cells, glyphs drawn in rows 0..6
// (ascent 7 like the vanilla font). Run: node tools/gen-nametag-icons.mjs [--preview out.png]
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { deflateSync, crc32 } from 'node:zlib';

const C = {
  '.': null,
  B: 0x8f9aa6, D: 0x7d8894, S: 0xe8ecf0, E: 0x141418, N: 0x2a2a30, K: 0xff9db8, W: 0xffffff,
  P: 0x8b6cff, Q: 0x6a4fe0, R: 0xff5470, r: 0xd93a58, H: 0xffc2cf, T: 0x3fe0c5, t: 0x26b39c,
  Y: 0xf4d6a0, G: 0x2a2a30, X: 0xffb547,
};

// U+E000 seal (Vibey Client), U+E001 staff, U+E002 supporter, U+E003 creator
const GLYPHS = [
  [ // seal face: grey head, dark eyes, pale snout with nose, pink cheeks
    '.DDDDD.',
    'DBBBBBD',
    'BEBBBEB',
    'BBBBBBB',
    'KSSNSSK',
    '.SSSSS.',
    '..BBB..',
  ],
  [ // staff: purple shield with a white star
    'PPPPPPP',
    'PPPWPPP',
    'PWWWWWP',
    'QPWWWPQ',
    '.QWPWQ.',
    '..QQQ..',
    '...Q...',
  ],
  [ // supporter: heart
    '.RR.RR.',
    'RHRRRRR',
    'RRRRRRR',
    'rRRRRRr',
    '.rRRRr.',
    '..rRr..',
    '...r...',
  ],
  [ // creator: pencil
    '.....XX',
    '....TXX',
    '...TTt.',
    '..TTt..',
    '.TTt...',
    'YTt....',
    'GY.....',
  ],
];

const CELL = 8;
function image(scale = 1) {
  const w = GLYPHS.length * CELL * scale, h = CELL * scale;
  const px = new Uint8Array(w * h * 4);
  GLYPHS.forEach((g, gi) => g.forEach((row, y) => [...row].forEach((ch, x) => {
    if (!(ch in C)) throw new Error(`unknown color ${ch}`);
    const c = C[ch];
    if (c == null) return;
    for (let sy = 0; sy < scale; sy++) for (let sx = 0; sx < scale; sx++) {
      const i = (((y * scale + sy) * w) + (gi * CELL + x) * scale + sx) * 4;
      px[i] = c >> 16 & 255; px[i + 1] = c >> 8 & 255; px[i + 2] = c & 255; px[i + 3] = 255;
    }
  })));
  return { w, h, px };
}

function png({ w, h, px }) {
  const chunk = (type, data) => {
    const len = Buffer.alloc(4); len.writeUInt32BE(data.length);
    const td = Buffer.concat([Buffer.from(type), data]);
    const crc = Buffer.alloc(4); crc.writeUInt32BE(crc32(td) >>> 0);
    return Buffer.concat([len, td, crc]);
  };
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(w, 0); ihdr.writeUInt32BE(h, 4); ihdr[8] = 8; ihdr[9] = 6;
  const raw = Buffer.alloc((w * 4 + 1) * h);
  for (let y = 0; y < h; y++) Buffer.from(px.buffer, y * w * 4, w * 4).copy(raw, y * (w * 4 + 1) + 1);
  return Buffer.concat([Buffer.from([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), chunk('IHDR', ihdr),
    chunk('IDAT', deflateSync(raw, { level: 9 })), chunk('IEND', Buffer.alloc(0))]);
}

const root = resolve(dirname(new URL(import.meta.url).pathname.replace(/^\/(\w:)/, '$1')), '..');
const out = resolve(root, 'src/main/resources/assets/vibeyclient/textures/font/icons.png');
mkdirSync(dirname(out), { recursive: true });
writeFileSync(out, png(image(1)));
console.log('wrote', out);
const pi = process.argv.indexOf('--preview');
if (pi > 0) { writeFileSync(process.argv[pi + 1], png(image(16))); console.log('preview', process.argv[pi + 1]); }
