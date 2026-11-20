# Research: animated names/ranks, in-world 3D UI, HUD editor UX

Scope: ideas only. No code or assets taken from any client. Searches run 2026-11-19 via web search (standard + one extended). Confidence is marked per item. Reddit/YouTube reaction searches returned nothing usable, so player-reaction claims are flagged as unverified.

---

## 1. Name styles, badges, rank prefixes

### 1a. NoRisk Client (NRC / NRC+)
- **Official detail is thin.** Search surfaced only NoRisk's invite page, which says NRC+ includes "extended nametag features" alongside shop discounts and monthly cosmetics. Source: https://norisk.gg/blog/invite-friends (confidence: low on specifics).
- **Third-party README** (unofficial, secondhand) lists nametag options: change the nametag icon (VIP crown, Gold Donator crown, default NoRisk logo via a command), nametag formatting, and a Spotify option that shows the current song in the nametag. Source: https://github.com/mulareal/norisk-cosmetics-unlocker (the unlocker is a third-party cosmetics-unlocking mod; I did not evaluate or recommend it, and its terms status is unchecked).
- **Release-note mentions** of "extra nametag text effects next to NoRisk's own" (v1.0.35). Source: https://github.com/mulareal/norisk-cosmetics-unlocker/releases/tag/v1.0.35
- **Video transcript** (German, secondhand) describes custom nametag effects reached via the NRC+ menu: rainbow coloring, blinking, and shake/wave animation. Source: https://sozai.app/transcript/norisk-client-release-transcript/ (confidence: low, unofficial summary).
- Player reactions: not found.

### 1b. Comparable features
- **LabyMod:** Name colors are a rank signal, not a shop cosmetic. Official support page: white = LabyMod 3 user (visible only in LabyMod 4), blue = LabyMod 4 user, yellow = LabyMod Plus, green = translator, turquoise = Cosmetic Creator (next to name) or QA (above name), purple = LabyMod staff. Source: https://www.labymod.net/en/support/article/5.what-is-the-meaning-of-the-different-color-of-the-icons. LabyMod 4 cosmetics and emotes are shop items visible to all users. Source: https://www.craftmc.net/minecraft-clients/labymod (confidence: medium). No official animated-gradient nametag found.
- **Lunar Client:** No official rank icons in tab/nametag found. Built-in "Lunar logo" icon marks players currently on Lunar; toggled via Tab Editor ("Show Tab Icon") and NameTags ("Show Nametag Icon"). Logos hidden on cracked servers. Source: https://support.lunarclient.com/support/solutions/articles/60000714532-lunar-client-logos-not-appearing and https://www.lunarclient.com/news/the-best-quality-of-life-mods-on-lunar-client. Tier Tagger mod shows gamemode tier icons. Source: https://www.lunarclient.com/news/tier-tagger-mod-on-lunar-client-everything-you-need-to-know
- **Essential:** Cosmetics (100 new in 1.3.0), gifting cosmetics to friends. No badge/nametag system detail found. Source: https://essential.gg/changelog (confidence: low on badges).
- **Feather / Badlion:** Nothing specific on name styles found.
- **Animated name mods (Fabric, third-party, not clients):** NickPaints (animated multi-gradient, rainbow preset, cloud sync so other NickPaints users see it), Naam Tag (inline tags &#wave &#float &#shake &#pulse &#flicker &#glow &#chroma), Colored Nicknames (solid/gradient/rainbow, group sorting). Sources: https://moddex.gg/mod/nickpaints , https://modrinth.com/mod/naam-tag , https://modrinth.com/mod/colored-nicknames

### 1c. Hypixel-style rank prefixes
- Official: ranks VIP, VIP+, MVP, MVP+, MVP++ (subscription upgrade); staff roles include moderation, QA, game dev, player support. Source: https://support.hypixel.net/hc/en-us/articles/360019646559-How-to-Obtain-the-Available-Ranks-on-Hypixel
- Wiki-listed rank names: Default, VIP, VIP+, MVP, MVP+, MVP++, Youtube, Pig, Mojang, Events, GM, Admin, Owner, MCP. Source: https://hypixelskyblock.minecraft.wiki/w/Template:Rn
- Colors (third-party config, unverified against Hypixel): Owner/Admin §c red, Mod §2 dark green, Helper §9 blue, Build Team §3 aqua, MVP §b, MVP+ §b with red "+", VIP+ §a with gold "+". Source: https://www.curseforge.com/minecraft (UniqueRanks config, "Hypixel" preset: dev.curseforge.com/projects/uniqueranks). Treat as reference only.
- Pattern worth noting: a bracketed prefix where the "+" is a different color from the base is the common "premium tier" signal.

### 1d. Common rank names and icon conventions (across servers and clients)
- Staff: Owner, Admin, Mod/Moderator, Helper, Dev/Developer, Builder/Build Team, QA.
- Creator tier: Creator / Partner / Media / YouTube / Streamer.
- Supporter tier: Supporter, VIP, VIP+, MVP, Donator (Gold Donator in NRC).
- Special: Beta Tester, Translator, Pig/Mojang/Events (Hypixel-specific).
- Icon conventions: crowns = paid/premium supporter (NRC); colored name = role (LabyMod); small logo/icon = client-in-use (Lunar, LabyMod 4 blue). Keep icons semantic (crown = donor, shield = staff, pen = translator) and avoid two roles sharing one icon.

### 1e. Ideas for our client
1. Separate three signals and never mix them: **identity** (client in use), **role** (staff/translator/creator), **supporter tier** (paid). LabyMod's color-coded-by-meaning approach is the clearest precedent.
2. Animated styles: keep to a small, named set (static gradient, wave, rainbow, shimmer). Animation should be per-style parameters (speed, direction), with a "static" fallback.
3. Show animated names in nametag and tab list first; chat is noisy and makes animation distracting. Offer a toggle to disable animation (NickPaints and Naam Tag both do this, a clear user-friendly default).
4. Gate by entitlement server-side, render client-side. Other players only see effects if they have the client (NickPaints syncs this way; Lunar logo same idea).
5. Respect readability: cap the animation amplitude; test against dark and light backgrounds and with shadows on.

---

## 2. In-world 3D UI and interaction

- **Vivecraft (VR):** Own wrist/hand HUD placement exists, but official detail was not retrieved. ViveInterface cuts HUD sections and places them on hands, head, or in world; panels stay live; opened via Vivecraft's radial menu. Source: https://www.curseforge.com/minecraft/mc-mods/viveinterface and https://www.vivecraft.org/how-to-play/ (confidence: medium).
- **VR design guidance (Meta):** Avoid menus that follow the wrist, because the other hand pokes a moving target. Spawn-from-wrist is acceptable only if the menu then stays fixed in world space. Source: https://developers.meta.com/vr/design/hands-ui-best-practices/ . This transfers directly to any "world-space panel": fix it when it opens.
- **WebDisplays:** Forks and remasters exist (1.21.1 NeoForge fork requires MCEF; a 1/16-deep "thin" screen block mounts on walls, ceilings, floors). Source: https://www.curseforge.com/minecraft/mc-mods/webdisplays-1-21-1-fork and https://github.com/montoyo/webdisplays . Move/resize UX details were not found.
- **DigitalScreen / AirWebDisplay (separate mods):** Move, resize, rotate; aspect-ratio and transform locking. Sources: https://modrinth.com/mod/digitalscreen , https://www.curseforge.com/minecraft/mc-mods/airwebdisplay . Locking is the fix people generally want after accidental drags (inferred, not verified in reviews).
- **Hologram Panel:** Only in-3D-level panel found; supports hover info and some direct interaction; resize handle unconfirmed. Source: https://modrinth.com (search result; no stable URL retrieved).
- **Create goggles:** Overlay shows only while worn in the head slot, not always-on. Create: Cyber Goggles offers toggles per readout. Factory HUD Goggles pins cards as HUD elements that can be moved, resized, made transparent, and given notes; bindings stored on the item, not the player. Sources: https://www.curseforge.com/minecraft/mc-mods/create-cyber-goggles , https://www.curseforge.com/minecraft/mc-mods/create-curio-goggles/install (confidence: medium). Idea: tie world-UI visibility to an equipped item, which gives the player an explicit on/off.
- **DecentHolograms:** Hologram pages can run click actions (left/right) and stack actions in order. Limitations section exists in the README; contents not retrieved. Sources: https://wiki.decentholograms.eu/features/actions/ , https://github.com/KinczeK/DecentHolograms . FancyHolograms: no native click support; workaround is an invisible interaction entity beside the hologram. Source: https://docs.fancyinnovations.com/fancyholograms/tutorials/clickable-holograms
- **Accidental-click problems (what annoys):** Mods exist just to stop misclicks: Prevent (per-category toggle for accidental right-click use), Interaction Disable, Hold Your Buttons (confirm a mass craft), No Misclicks At All (off-hand). Sources: https://www.curseforge.com/minecraft/mc-mods/prevent , https://www.curseforge.com/minecraft/mc-mods/no-misclicks-at-all , https://moddex.gg/mod/hold-your-buttons . Also vanilla bugs: Mojira MC-268291 (buttons clicked accidentally when switching by click). Source: https://mojira.dev/MC-268291 . Radial menus that fail under a held key: Effortless Building issue. Source: https://bitbucket.org/Requios/effortless-building/issues/99/1144-radial-menu-unusable
- **Legibility at distance:** no source retrieved; general 3D UI practice: scale text with distance or cap world size; use a backing plate.

### 2a. What works / what annoys (synthesis; mostly inferred, flagged)
Works:
- Fix a panel in world when opened; no following the player's view or hand (Meta guidance).
- Explicit enter/exit (key or item-equipped mode) rather than always-on panels (Create goggles precedent).
- Lock toggles for position and size (AirWebDisplay, DigitalScreen).
- Snap and keep aspect ratio by default.
Annoys:
- Accidental clicks on world panels when walking through them or aiming past them. Mitigation: require a modifier or dedicated "interact" mode before clicks reach the panel; show hover highlight first.
- Resize handles too small to grab at distance.
- Text unreadable at distance; no size clamp.
- Z-fighting when panel is flush with blocks (offset slightly; not sourced, general practice).

### 2b. Ideas for our client
1. World panels default to locked; "Edit" mode unlocks drag/resize with visible handles, auto-locks on exit.
2. Click passes through to the world unless the cursor is over a panel in interact mode.
3. Minimum on-screen size for handles and text: clamp scale so text stays legible; show a size readout while resizing.
4. Keep a single "reset to default" per panel (WebDisplays forks and Factory HUD both store per-item/per-panel state).

---

## 3. HUD editing UX (Lunar, Feather, Badlion, LabyMod)

- **Lunar Client (official):** HUD layout editor got mod snapping, multi-select (Ctrl + left-click), per-mod scaling boxes, undo/redo (Ctrl+Z / Ctrl+Y), arrow-key nudging, and a Movement Helper panel bottom-left listing shortcuts. Source: https://www.lunarclient.com/news/patch-notes-4 (no date on page). Later fix: center snap wasn't snapping; mods snapped to hidden mods; edge placement was hard. Fixed in a later patch. Source: https://lunarclient.com/news/patch-notes-17 . Reading: the editor needed more than one pass to get snapping right; users clearly wanted edge and center snap to behave predictably.
- **Lunar Keystrokes (third-party):** grid snapping with visible alignment guides. Source: https://modrinth.com/mod/lunar-keystrokes/version/3 (confidence: low on details).
- **HUD Mover (Fabric, third-party):** snap to grid, adjustable grid size, alignment guides, presets (Vanilla, Compact). Source: https://modrinth.com/mod/hud-mover
- **SuperHud7:** snap guides, save/load/delete presets. Source: https://www.curseforge.com/minecraft/customization/superhud7
- **PixelPanel (Fabric 1.21.11):** click select, drag move, scroll to scale, corner drag to resize. Source: https://modrinth.com/mod/pixelpanel (from search summary; confidence: medium).
- **Magic HUD:** selection frame with corner resize handle per module. Source: https://modrinth.com/mod/magic-hud
- **FCHUD (NeoForge 1.21.1):** corner handles that keep proportions. Source: https://www.curseforge.com/minecraft/mc-mods/fchud
- **Lamina:** per-element scale Auto, 1x, 2x, 3x, 4x (discrete steps, not free). Source: https://www.curseforge.com/minecraft/mc-mods/lamina
- **Lunar Hud (third-party):** drag-and-drop editor opened with Right Shift. Source: https://www.curseforge.com/minecraft/mc-mods/lunar-hud
- **Feather / Badlion:** official HUD editor detail and Reddit opinions not found. Badlion forum threads on HUD changes exist (rounded corners for HUD mods, arranging HUD for ArmorStatus). Source: https://www.badlion.net/forum/thread/207705 , https://www.badlion.net/forum/thread/208735 (confidence: low).
- Preference patterns visible in sources (inferred): snapping with visible guides and edge/center snap; keyboard nudging; undo; discrete scale steps are easier to reason about than free drag scale; presets per profile.
- Per-server layouts: no source found for any of the four clients.

### 3a. HUD design recommendations for our client
1. Snap: edge + center + other-element alignment, with guides drawn while dragging. Snap to screen edges even when a hidden element is nearby (hidden elements must not capture snaps, per Lunar's fix).
2. Multi-select with Ctrl+click, plus arrow-key nudging (1 px, Shift = 10 px).
3. Undo/redo from day one (Ctrl+Z / Ctrl+Y).
4. Scale: wheel over a selected element + corner handles; show current scale %. Offer snap steps (100%, 75%, 125%).
5. Presets: named layouts (Vanilla-like, Compact, PvP, Recording), switchable from the menu.
6. Per-server or per-game-mode layout bindings: unsourced idea, but a natural extension of presets; keep it opt-in.
7. Movement Helper / shortcut legend always visible in the editor corner, as Lunar does.

---

## Gaps and next steps
- NoRisk official nametag specs: check norisk.gg docs or in-client NRC+ settings directly. Not reachable via search.
- Reddit / YouTube reaction threads: searches returned no relevant results. Try r/lunarclient, r/labymod, r/NoRiskClient and Badlion forum searches with direct site queries if this matters.
- Vivecraft built-in wrist HUD and WebDisplays move/resize usability: need the mod docs or wiki.
- DecentHolograms "Minecraft Limitations" section: fetch https://github.com/DecentSoftware-eu/DecentHolograms README.
- Nothing here was copied from any client; all items are design ideas.
