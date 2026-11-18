'use strict';
const zlib = require('zlib');

/** Minimal RGBA PNG encoder (no dependencies). pixel(x, y) -> [r, g, b, a]. */
function makePng(w, h, pixel = () => [128, 128, 128, 255]) {
	const raw = Buffer.alloc((w * 4 + 1) * h);
	for (let y = 0; y < h; y++) {
		raw[y * (w * 4 + 1)] = 0;
		for (let x = 0; x < w; x++) Buffer.from(pixel(x, y)).copy(raw, y * (w * 4 + 1) + 1 + x * 4);
	}
	const chunk = (type, data) => {
		const len = Buffer.alloc(4);
		len.writeUInt32BE(data.length);
		const td = Buffer.concat([Buffer.from(type, 'ascii'), data]);
		const crc = Buffer.alloc(4);
		crc.writeUInt32BE(zlib.crc32(td) >>> 0);
		return Buffer.concat([len, td, crc]);
	};
	const ihdr = Buffer.alloc(13);
	ihdr.writeUInt32BE(w, 0);
	ihdr.writeUInt32BE(h, 4);
	ihdr[8] = 8;
	ihdr[9] = 6;
	return Buffer.concat([
		Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]),
		chunk('IHDR', ihdr),
		chunk('IDAT', zlib.deflateSync(raw)),
		chunk('IEND', Buffer.alloc(0)),
	]);
}
/** Not a playable sound, but a structurally valid Ogg page header ("OggS") – enough for format checks. */
function fakeOgg(size = 64) {
	const b = Buffer.alloc(size, 1);
	b.write('OggS', 0, 'ascii');
	return b;
}
const pngB64 = (w, h, px) => makePng(w, h, px).toString('base64');

const faces = (uvs, tex) => {
	const o = {};
	['north', 'east', 'south', 'west', 'up', 'down'].forEach((f) => (o[f] = { uv: uvs[f] || [0, 0, 1, 1], texture: uvs[f] === null ? null : tex }));
	return o;
};

function cosmeticSnapshot() {
	return {
		kind: 'cosmetic',
		meta: {
			id: 'test:seal_hat', name: { en: 'Seal Hat', de: 'Robbenhut' }, description: { en: 'A hat.', de: '' },
			author: 'Vibe Craft Studios', tags: ['seal', 'test'], rarity: 'rare', slot: 'hat', hideWithArmor: ['head'],
			pet: { follow: '', scale: 1 }, capeTexture: '', shieldTexture: '',
			emote: {}, sounds: [{ id: 'pop', ogg: fakeOgg().toString('base64') }],
			sprites: [{ id: 'sparkle', texture: 'main', uv: [0, 48, 8, 56] }],
		},
		textures: [
			{ id: 'main', uvWidth: 64, uvHeight: 64, png: pngB64(64, 64), frameCount: 1, frameTime: 1, frames: [], isDefault: true },
			{ id: 'glow', uvWidth: 16, uvHeight: 16, png: pngB64(16, 64), frameCount: 4, frameTime: 2, frames: [0, 1, 2, 3, 2, 1], isDefault: false },
		],
		nodes: [
			{
				type: 'group', name: 'hat', origin: [0, 24, 0], rotation: [0, 0, 0], attach: 'head', emissive: false, translucent: false,
				children: [
					{ type: 'cube', name: 'brim', from: [-5, 32, -5], to: [5, 33, 5], inflate: 0, mirror: false, origin: [0, 0, 0], rotation: [0, 0, 0], boxUv: true, uvOffset: [0, 0], faces: faces({}, 'main') },
					{
						type: 'group', name: 'top', origin: [0, 33, 0], rotation: [0, 10, 0], emissive: true, translucent: false,
						children: [
							{ type: 'cube', name: 'crown', from: [-3, 33, -3], to: [3, 37, 3], inflate: 0.25, mirror: false, origin: [0, 35, 0], rotation: [0, 0, 22.5], boxUv: false, faces: faces({ north: [0, 0, 6, 4], south: [6, 0, 12, 4], east: null, west: [0, 4, 6, 8], up: [6, 6, 0, 0], down: [12, 0, 6, 6] }, 'glow') },
							{ type: 'locator', name: 'tip', position: [0, 38, 0] },
						],
					},
				],
			},
		],
		animations: [
			{
				name: 'idle', length: 2, loop: 'loop',
				bones: {
					top: {
						rotation: [
							{ time: 0, interpolation: 'catmullrom', points: [{ x: '0', y: '0', z: '0' }] },
							{ time: 1, interpolation: 'catmullrom', points: [{ x: 0, y: '45', z: 0 }] },
							{ time: 2, interpolation: 'linear', points: [{ x: 0, y: 0, z: 0 }] },
						],
						scale: [{ time: 0.5, interpolation: 'step', points: [{ x: 1, y: 1, z: 1 }, { x: '1.2', y: 1.2, z: 1.2 }] }],
					},
				},
				effects: {
					particle: [{ time: 0.5, points: [{ effect: 'heart', locator: 'tip', vibey_count: '3', vibey_spread: '0.2', vibey_velocity: '0 0.05 0' }, { vibey_sprite: 'sparkle', locator: 'tip', vibey_count: '4', vibey_lifetime: '0.8', vibey_size: '0.15', vibey_gravity: '-0.01', vibey_emissive: true }] }],
					sound: [{ time: 0, points: [{ effect: 'minecraft:entity.player.levelup', vibey_volume: '1', vibey_pitch: '1.2' }] }, { time: 1, points: [{ effect: 'pop' }] }],
					timeline: [{ time: 1.2, points: [{ vibey_bone: 'top', vibey_visible: false }] }],
				},
			},
		],
	};
}

function emoteSnapshot() {
	const P = { root: [0, 0, 0], head: [0, 24, 0], body: [0, 24, 0], right_arm: [-5, 22, 0], left_arm: [5, 22, 0], right_leg: [-1.9, 12, 0], left_leg: [1.9, 12, 0] };
	const part = (name, children = []) => ({ type: 'group', name, origin: P[name], rotation: [0, 0, 0], attach: '', children });
	return {
		kind: 'emote',
		meta: {
			id: 'test:wave', name: { en: 'Wave', de: 'Winken' }, description: {}, author: 'Vibe Craft Studios', tags: ['greeting'], rarity: 'common',
			emote: { animation: 'main', loop: false, thirdPerson: true, stopOnMove: true, hideHeldItems: false }, sounds: [], sprites: [],
		},
		textures: [{ id: 'props', uvWidth: 32, uvHeight: 32, png: pngB64(32, 32), frameCount: 1, frameTime: 1, frames: [], isDefault: true }],
		nodes: [
			part('root', [
				part('head'),
				part('body'),
				part('right_arm', [
					{ type: 'group', name: 'flag', origin: [-6, 12, 0], rotation: [0, 0, 0], emissive: false, translucent: true, children: [
						{ type: 'cube', name: 'pole', from: [-6.5, 2, -0.5], to: [-5.5, 14, 0.5], inflate: 0, mirror: false, origin: [0, 0, 0], rotation: [0, 0, 0], boxUv: true, uvOffset: [0, 0], faces: faces({}, 'props') },
					] },
					{ type: 'cube', name: 'glove', from: [-8.5, 11.5, -2.5], to: [-3.5, 14.5, 2.5], inflate: 0, mirror: true, origin: [0, 0, 0], rotation: [0, 0, 0], boxUv: true, uvOffset: [8, 0], faces: faces({}, 'props') },
					{ type: 'locator', name: 'hand', position: [-6, 10, 0] },
				]),
				part('left_arm'),
				part('right_leg'),
				part('left_leg'),
			]),
		],
		animations: [
			{
				name: 'main', length: 1.5, loop: 'once',
				bones: {
					right_arm: { rotation: [{ time: 0, interpolation: 'linear', points: [{ x: 0, y: 0, z: 0 }] }, { time: 0.5, interpolation: 'linear', points: [{ x: 0, y: 0, z: 150 }] }, { time: 1.5, interpolation: 'linear', points: [{ x: 0, y: 0, z: 0 }] }] },
					root: { position: [{ time: 0, interpolation: 'step', points: [{ x: 0, y: 0, z: 0 }] }, { time: 0.75, interpolation: 'linear', points: [{ x: 0, y: 2, z: 0 }] }] },
					flag: { scale: [{ time: 0, interpolation: 'linear', points: [{ x: 1, y: 1, z: 1 }] }] },
				},
				effects: { particle: [{ time: 0.5, points: [{ effect: 'minecraft:note', locator: 'hand', vibey_count: '2' }] }], sound: [], timeline: [{ time: 1.4, points: [{ vibey_bone: 'flag', vibey_visible: false }] }] },
			},
			{ name: 'loop_variant', length: 1, loop: 'hold', bones: {}, effects: {} },
		],
	};
}

module.exports = { makePng, pngB64, fakeOgg, cosmeticSnapshot, emoteSnapshot };
