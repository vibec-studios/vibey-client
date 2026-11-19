// Generates the bitmap glyphs of the "vibeyclient:icons" font (seal, rank and badge icons before player names).
// Original pixel art: 7x7 art in 8x8 cells, rows 0..6 (ascent 7 like the vanilla font), 16 cells per row, row n
// holds U+E0n0..U+E0nF. Writes textures/font/icons.png, font/icons.json and docs/rank-icons.json (the same pixels
// for the backend admin UI and the website). See docs/NAMETAGS.md.
// Run: node tools/gen-nametag-icons.mjs [--preview out.png] [--export-json <file>]
import { writeFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { deflateSync, crc32 } from 'node:zlib';

const C = {
  '.': null,
  B: 0x8f9aa6, D: 0x7d8894, S: 0xe8ecf0, E: 0x141418, N: 0x2a2a30, K: 0xff9db8, k: 0xd9708f, W: 0xffffff,
  L: 0xc8ccd2, P: 0x8b6cff, Q: 0x6a4fe0, R: 0xff5470, r: 0xd93a58, H: 0xffc2cf, T: 0x3fe0c5, t: 0x26b39c,
  Y: 0xf4d6a0, y: 0xd9b77a, G: 0x2a2a30, X: 0xffb547, x: 0xd98f1f, O: 0xff8a3d, A: 0x4aa8ff, a: 0x2f7fd6,
};

const GLYPHS = [
  { cp: 0xe000, id: 'seal', rows: ['.DDDDD.', 'DBBBBBD', 'BEBBBEB', 'BBBBBBB', 'KSSNSSK', '.SSSSS.', '..BBB..'] },
  { cp: 0xe001, id: 'staff', rows: ['PPPPPPP', 'PPPWPPP', 'PWWWWWP', 'QPWWWPQ', '.QWPWQ.', '..QQQ..', '...Q...'] },
  { cp: 0xe002, id: 'heart', rows: ['.RR.RR.', 'RHRRRRR', 'RRRRRRR', 'rRRRRRr', '.rRRRr.', '..rRr..', '...r...'] },
  { cp: 0xe003, id: 'pencil', rows: ['.....XX', '....TXX', '...TTt.', '..TTt..', '.TTt...', 'YTt....', 'GY.....'] },
  { cp: 0xe010, id: 'shield-crown', rows: ['XXXXXXX', 'XWXWXWX', 'XWWWWWX', 'xWWWWWx', '.xXXXx.', '..xxx..', '...x...'] },
  { cp: 0xe011, id: 'shield', rows: ['RRRRRRR', 'RRRWRRR', 'RWWWWWR', 'rRRWRRr', '.rRWRr.', '..rrr..', '...r...'] },
  { cp: 0xe012, id: 'shield-hammer', rows: ['TTTTTTT', 'TWWWWWT', 'TWWWWWT', 'tTTWTTt', '.tTWTt.', '..ttt..', '...t...'] },
  { cp: 0xe013, id: 'shield-code', rows: ['PPPPPPP', 'PWPPPPP', 'PPWPPPP', 'QWPWWWQ', '.QPPPQ.', '..QQQ..', '...Q...'] },
  { cp: 0xe014, id: 'star', rows: ['...Y...', '..YYY..', 'YYYYYYY', '.YYYYY.', '..YyY..', '.YY.YY.', '.y...y.'] },
  { cp: 0xe015, id: 'flask', rows: ['.BBBBB.', '..BSB..', '..BSB..', '.BSSSB.', 'BTTTTTB', 'BtTTTtB', '.BBBBB.'] },
  { cp: 0xe016, id: 'crown', rows: ['.......', 'K..K..K', 'KK.K.KK', 'KKKKKKK', 'KWKWKWK', 'kkkkkkk', '.......'] },
  { cp: 0xe017, id: 'check', rows: ['......T', '.....TT', 'T...TT.', 'TT.TT..', '.TTT...', '..T....', '.......'] },
  { cp: 0xe018, id: 'gem', rows: ['.AAAAA.', 'AWAAAaA', 'aaaaaaa', '.aAAAa.', '..aAa..', '...a...', '.......'] },
  { cp: 0xe019, id: 'flame', rows: ['...O...', '..OO...', '.OOXO..', '.OXXOO.', 'OXXYXO.', 'OXYYXO.', '.OOOO..'] },
  { cp: 0xe02f, id: 'generic', tint: true, rows: ['...W...', '..WWW..', '.WWWWL.', 'WWWWLLL', '.WWLLL.', '..LLL..', '...L...'] },
];

const RANKS = {
  owner: 'shield-crown', admin: 'shield', mod: 'shield-hammer', dev: 'shield-code', partner: 'star',
  creator: 'pencil', supporter: 'crown', beta: 'flask', staff: 'staff',
};

const CELL = 8, PER_ROW = 16, ROWS = 3;

function cellOf(cp) {
  const i = cp - 0xe000;
  return { col: i % PER_ROW, row: Math.floor(i / PER_ROW) };
}

function image(scale = 1) {
  const w = PER_ROW * CELL * scale, h = ROWS * CELL * scale;
  const px = new Uint8Array(w * h * 4);
  for (const g of GLYPHS) {
    if (g.rows.length !== 7 || g.rows.some((r) => r.length !== 7)) throw new Error(`${g.id}: art must be 7x7`);
    const { col, row } = cellOf(g.cp);
    g.rows.forEach((line, y) => [...line].forEach((ch, x) => {
      if (!(ch in C)) throw new Error(`${g.id}: unknown color ${ch}`);
      const c = C[ch];
      if (c == null) return;
      for (let sy = 0; sy < scale; sy++) for (let sx = 0; sx < scale; sx++) {
        const i = (((row * CELL + y) * scale + sy) * w + (col * CELL + x) * scale + sx) * 4;
        px[i] = c >> 16 & 255; px[i + 1] = c >> 8 & 255; px[i + 2] = c & 255; px[i + 3] = 255;
      }
    }));
  }
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

function fontJson() {
  const rows = [];
  for (let r = 0; r < ROWS; r++) {
    let s = '';
    for (let c = 0; c < PER_ROW; c++) {
      const cp = 0xe000 + r * PER_ROW + c;
      s += GLYPHS.some((g) => g.cp === cp) ? String.fromCharCode(cp) : '\u0000';
    }
    rows.push(s);
  }
  const font = {
    providers: [
      { type: 'bitmap', file: 'vibeyclient:font/icons.png', ascent: 7, height: 8, chars: rows },
      { type: 'reference', id: 'minecraft:default' },
    ],
  };
  return JSON.stringify(font, null, 2).replace(/[\u0000-]/g, (ch) => `\\u${ch.charCodeAt(0).toString(16).padStart(4, '0')}`) + '\n';
}

function exportJson() {
  const used = new Set(GLYPHS.flatMap((g) => [...g.rows.join('')]));
  const palette = Object.fromEntries(Object.entries(C).filter(([k, v]) => v != null && used.has(k))
    .map(([k, v]) => [k, `#${v.toString(16).padStart(6, '0')}`]));
  const icons = Object.fromEntries(GLYPHS.map((g) => [g.id, {
    codepoint: `U+${g.cp.toString(16).toUpperCase()}`, rows: g.rows, ...(g.tint ? { tint: true } : {}),
  }]));
  return JSON.stringify({ version: 1, size: 7, cell: CELL, transparent: '.', palette, icons, ranks: RANKS }, null, 2) + '\n';
}

const root = resolve(dirname(new URL(import.meta.url).pathname.replace(/^\/(\w:)/, '$1')), '..');
const assets = resolve(root, 'src/main/resources/assets/vibeyclient');
const write = (file, data) => { mkdirSync(dirname(file), { recursive: true }); writeFileSync(file, data); console.log('wrote', file); };
write(resolve(assets, 'textures/font/icons.png'), png(image(1)));
write(resolve(assets, 'font/icons.json'), fontJson());
const ei = process.argv.indexOf('--export-json');
write(ei > 0 ? resolve(process.argv[ei + 1]) : resolve(root, 'docs/rank-icons.json'), exportJson());
const pi = process.argv.indexOf('--preview');
if (pi > 0) write(resolve(process.argv[pi + 1]), png(image(16)));
