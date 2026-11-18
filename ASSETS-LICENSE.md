# Assets – all rights reserved

Copyright 2026 Vibe Craft Studios. All rights reserved.

The following files are **not** covered by the Apache License 2.0 of the code:

**Logo and branding**

- `assets/icon.png`, `assets/icon.svg` (the seal logo; also copied into every jar as `assets/vibeyclient/icon.png`)
- the seal mascot as drawn or modelled anywhere in this repository, including the procedural seal in
  `src/main/java/dev/henny/vibeyclient/ui/SealModel.java` / `ui/Seal.java` (the renderer code is Apache-2.0, the
  character it depicts is ours; see [TRADEMARKS.md](TRADEMARKS.md)) and the seal items of the starter set

**Starter cosmetics and emotes (the starter set)**

- `content/src/**` – the sources of every starter cosmetic and emote (models, textures, animations and effects
  described as code; the item designs are assets, not reusable code)
- `content/dist/**` – the built `.vibey` bundles, `index.json`, `thumbnails/`, `previews/` and the Blockbench
  textures in `content/dist/blockbench/`; the same bundles and thumbnails go into every jar as
  `assets/vibeyclient/builtin/**`
- `content/sounds/**` – the sounds of the starter set
- `content/audit/**` – the test bundles of the audit suite

**Mod resources** (`src/main/resources/assets/vibeyclient/`)

- `textures/font/icons.png` – the nametag/badge glyphs (the seal icon and the badges), drawn by
  `tools/gen-nametag-icons.mjs` (code: Apache-2.0)
- `textures/lyrics/sky/**` and `textures/lyrics/sky/thumb/**` – the lyrics skies and their thumbnails, except the
  files listed under *Exceptions*
- `lyrics/demo/pixel_skies.png` – the cover of the demo song
- the 9 × 9 pixel icons defined in `src/main/java/dev/henny/vibeyclient/ui/Icons.java` (the code that draws them is
  Apache-2.0, the icon designs are assets)

**Blockbench plugin**

- the reference player (the grey mannequin model, its colours `REF_COLORS` and the skin painted by
  `paintReferenceSkin()`) in `blockbench/vibey.js`. The plugin **code** is Apache-2.0; the reference player it
  draws is an asset.

**Documentation and online content**

- `docs/screenshots/**`
- cosmetic models, textures, animations, sounds and thumbnails loaded from the Vibe services (the cosmetics catalog)

You may view them in this repository and use them as part of unmodified official builds. You may **not** use them in
forks, other projects, merchandise or marketing, and you may not redistribute them on their own, without written
permission from Vibe Craft Studios. Forks must remove or replace them (a fork that keeps the cosmetics engine needs
its own content and its own catalog).

## Exceptions

- **Published earlier under MIT.** These files were published in
  [vibey-lyrics](https://github.com/vibec-studios/vibey-lyrics) under the MIT License (see
  `licenses/vibey-lyrics-MIT.txt`) and remain available under it:
  `src/main/resources/assets/vibeyclient/textures/lyrics/sky/{aurora,candy,crimson,dreamy,galaxy,golden,moonlight,nebula,saturn,streak,sunset,synthwave}.png`
  (+ their `.png.mcmeta`) and the demo song `src/main/resources/assets/vibeyclient/lyrics/demo/pixel_skies.ogg` /
  `pixel_skies.lrc`. Our names and the seal stay protected by [TRADEMARKS.md](TRADEMARKS.md) either way.
- **Third-party files keep their own licenses:** the fonts in `src/main/resources/assets/vibeyclient/font/lyrics/`
  (SIL Open Font License 1.1 / Apache 2.0, texts in `licenses/lyrics/fonts/`) and the libraries listed in
  [NOTICE](NOTICE).
- Language files (`lang/`, `i18n/`), JSON definitions without artwork (e.g. `font/*.json`, mixin configs) and the
  build/test tools (`content/tools/`, `tools/`, `blockbench/test/`) are code and fall under the Apache License 2.0.
