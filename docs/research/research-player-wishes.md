# Minecraft Java: what players want from PvP/QoL clients and client-side Fabric mods

Research date: 2026-11-19. Method: WebSearch (standard + extended), web-fetch of Trustpilot review pages, GitHub READMEs, and Lunar/Essential changelogs.

## 0. Coverage caveats (read first)

- **No Reddit data.** old.reddit.com is blocked for the fetch tool ("unable to fetch"), and the redlib mirror did not resolve (DNS failure). Every WebSearch for Reddit returned zero Reddit threads. All Reddit-sourced claims are therefore absent. Reddit is the biggest gap in this report.
- **No Hypixel / Minecraft forum sentiment** beyond a few Badlion forum threads (titles and snippets only, not read).
- **Frequency signals** below come from my own tallies of Trustpilot reviews (Lunar: 40 of 113 reviews read, 28 at 1 star; Badlion: 20 of 28 read, 15 at 1 star), from blog and SEO listicles (low signal, often affiliate), and from a 0.5-star moddex.gg review set for Essential (tiny sample). Treat counts as rough, not statistical.
- **Gaps in evidence:** no survey data on feature usage (FPS boost, keystrokes, etc.) was found. "Most loved features" is inferred from what Trustpilot praise and mod listings mention, not from usage data.
- **Unverified claims from blogs, not confirmed:** Feather relaunched as "Dawn" (dawn.gg) in 2026; Lunar acquired Badlion in 2026. Trustpilot reviews do mention "Lunar bought Badlion / ruined it" (~4 Lunar, ~2 Badlion), so the takeover is a real user perception even if the exact deal is unverified.
- Download counts for Modrinth mods were not reliably attributable (collection pages mixed numbers). Sodium ~64M and Iris ~62M appeared, but attribution is uncertain. Do not cite them as fact.

---

## 1. Most loved features (ranked)

Ranking reflects how often features appear in praise and in listicle "why use it" text. Praise is a minority of reviews in every source, so these are weak signals.

1. **FPS / performance on modest hardware** — Feather is repeatedly praised as the light option; Badlion Lite "maximizing FPS"; Badlion reviewers praise "higher FPS than Lunar." Signal: strong in blogs (every client listicle), ~3 Lunar and ~3 Badlion Trustpilot praise mentions.
   - https://cyberpost.co/what-is-the-best-fps-client-for-minecraft/
   - https://www.craftmc.net/minecraft-clients/for/low-end-pcs
   - https://www.trustpilot.com/review/client.badlion.net
2. **Built-in mods / one-click setup (no manual mod installing)** — Lunar's "built-in mods, profiles, shaders, easy to use": ~6 of 40 Lunar reviews. Also cited: "server-approved mods" (listicle). Signal: medium, consistent across the Lunar sample.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://champbop.com/best-minecraft-clients/
3. **Minimap + waypoints as a bundled QoL pair** — Xaero's Minimap is the most-cited example; waypoints are the usual companion. Signal: medium (roundups); a counter-signal exists (see §2, lag complaints).
   - https://sportskeeda.com/minecraft/5-best-client-side-mods-minecraft
   - https://github.com/coredex-source/CList-plus (the repo resolves to coredex-source/Gazetteer, a CList fork)
4. **Rewind / replay-like features** — Lunar "rewind feature" praised in ~3 reviews. The Replay Mod is the community standard for recording. Signal: weak-medium.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://sportskeeda.com/minecraft/6-mods-part-minecraft (roundup context)
5. **Customisable HUD (move, scale, toggle elements)** — Appears as a feature in mod listings (SpotiHUD visual editor; py54 crunch "every line independently draggable"; MusiCraft drag-and-drop). Signal: weak as a stated wish, but many mods built around it.
   - https://modrinth.com/mod/spotihud
   - https://modrinth.com/mod/py54-crunch
6. **Shaders / visual packs in-client** — Lunar praise mentions shaders in "built-in" list; Sodium + Iris is the common Fabric route ("best experience for casual players" per one blog). Signal: weak-medium.
   - https://modrinth.com/mod/sodium (see Iris pages on Modrinth)
   - https://www.trustpilot.com/review/lunarclient.com
7. **Friends, chat, voice** — Lunar's built-in voice chat and Badlion's friends system are listed as selling points. Signal: weak (listicles only).
   - https://saashub.com/compare-feather-client-vs-lunar-client
   - https://alternativeto.net/software/lunar-client/?p=2
8. **Cosmetics / emotes (for a subset of players)** — Heavily marketed but polarising (see §2). Essential's changelog shows ongoing emote and particle work. Signal: strong among a niche, negative among many.
   - https://essential.gg/changelog
9. **Accessibility options** — Little explicit demand, but players ask for larger GUI scale (Minecraft feedback post) and built-in colour-blind modes. See §3.
   - https://feedback.minecraft.net/hc/en-us/community/posts/360075153891-Further-Accessibility
   - https://feedback.minecraft.net/hc/en-us/community/posts/44860776599821-Colorblind-option-for-Minecraft

---

## 2. Biggest complaints about clients (ranked by Trustpilot frequency)

Lunar sample: 40 reviews (28 at 1 star). Badlion sample: 20 reviews (15 at 1 star). Counts are my tallies; one review can hit several themes.

1. **Crashes, freezes, won't launch** — Lunar ~12/40; Badlion ~8/20. Quotes: "Crashes all the time and has a memory leak" (Lunar, 1-star). Top complaint for both. Signal: very strong.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://www.trustpilot.com/review/client.badlion.net
2. **Login / Microsoft sign-in / repeated re-auth** — Lunar ~8/40; Badlion ~6/20. Signal: strong. Ties to store/account problems.
   - https://www.trustpilot.com/review/lunarclient.com?page=2
3. **Performance of the launcher itself (slow start, 5+ min launch, FPS drops below vanilla+Sodium)** — Lunar ~10/40; Badlion ~5/20. Quote from a reviewer: "lower FPS on Lunar than on vanilla with Sodium." Signal: strong.
   - https://ca.trustpilot.com/review/lunarclient.com?page=2
4. **Ads and upsells in the launcher** — Lunar ~5/40 for ads (in-game, YouTube, "pay to remove"); plus Badlion 2. Lunar's own support page confirms bottom-right launcher ads, tailored by cookies. A community tool, NoMoreLunarPlus, exists to strip ads, Lunar+ banners, store buttons, chat-limit upsells, and telemetry, and to block self-updates. Signal: strong for a vocal minority; the existence of the removal tool is itself a signal.
   - https://github.com/Zyhloh/NoMoreLunarPlus
   - https://support.lunarclient.com/support/solutions/articles/60001600509-reporting-ads-on-lunar-client
5. **Own mods overridden or broken** — Lunar ~6/40: "own mods won't load, mods deleted or re-added, forced built-in mods." One reviewer: Lunar replaced a newer Sodium with an older one. Quote: "don't EVER try to use lunar to launch mods." Signal: strong for power users; directly relevant to a Fabric-first client.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://www.trustpilot.com/review/lunarclient.com?page=2
6. **Cosmetics shop: pushy, overpriced, soft-lock** — Lunar ~4/40. A tutorial soft-lock and failed purchases are cited. Lunar's status update says PayPal suspended the account, the store moved to Xsolla, and only Emotes and Wings are sold now, with Cloaks and Unbans with no return date. Signal: medium.
   - https://www.lunarclient.com/news/client-status-update
   - https://ca.trustpilot.com/review/lunarclient.com
7. **Bloat, cluttered menus, too much text/UI** — Lunar ~3/40 explicit; the bloat theme also appears in a custom modpack page criticising Lunar and Badlion. Quote: "endless menus" (Trustpilot). Signal: medium but under-counted, since menus also appear inside the ads and store complaints.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://www.badlion.net/forum/thread/319303
8. **Ownership / "ruined by the merger"** — Lunar ~4/40, Badlion ~2/20. Quote: a takeover perception, not a specific bug. Signal: medium, perception-level.
   - https://www.trustpilot.com/review/lunarclient.com
   - https://www.trustpilot.com/review/client.badlion.net
9. **Bans and anticheat (Badlion)** — Badlion has announced discontinuing its anticheat service (BAC; decommission date "June 19th", year unverified). Forum threads report lingering or unseen bans, crashes on the "Enabling Badlion Anticheat" screen, and a claim (one user's reading) that the BAC connects to servers and shares data. Signal: medium on Badlion forums; I did not find Reddit ban sentiment.
   - https://www.badlion.net/forum/thread/348298/last-post
   - https://forums.lokamc.com/threads/dont-use-badlion-client-and-dont-allow-it-here.4708
10. **Refunds and support** — Badlion ~2/20 ("Refusal to do refunds"); Lunar has no explicit support mentions in the sample, and its profile "Hasn't replied to negative reviews." Signal: medium.
    - https://www.trustpilot.com/review/client.badlion.net
    - https://www.trustpilot.com/review/lunarclient.com
11. **Privacy / telemetry / data fears** — Lunar 3/40 (hacked accounts or data fears). Essential shipped an opt-out for non-functional telemetry in Dec 2025, which implies pressure. NoMoreLunarPlus also disables telemetry, Sentry, and ad-targeting email hashing. Signal: medium, rising.
    - https://essential.gg/changelog
    - https://github.com/Zyhloh/NoMoreLunarPlus
12. **Corrupted worlds, lost items, leftover files** — Lunar 3/40 (lost worlds, items, .minecraft corruption); 2/40 uninstall leftovers. Quote: "Huge building project got deleted." Signal: medium, high severity.
    - https://www.trustpilot.com/review/lunarclient.com?page=2
13. **Forced / repeated updates** — Lunar 1/40. Signal: weak, but the removal tool blocks self-updates, so it matters to a subset.
14. **Cosmetic clutter in the mod (Essential)** — Moddex reviews (0.5 stars, tiny sample): "bloated micro-transaction shop," "If you want cosmetics… download a cosmetics mod," and a NeoForge mod "Essential Shop Remover" that disables the shop, cosmetics, and emotes. Signal: weak (small sample), but the existence of a remover is a signal.
    - https://moddex.gg/mod/essential-mod/reviews/4037
    - https://moddex.gg/mod/essential-mod/reviews/1779

**Fabric / new-version compatibility (client-mod side):**
15. **Mods lag behind new Minecraft versions** — Fabric's own blog repeatedly asks players to be patient and not pester mod authors; unobfuscated versions (26.x) mean loader and Loom update fast, but every mod touching MC code must recompile. Signal: strong and structural, though sourced from Fabric's own posts rather than player complaints.
    - https://fabricmc.net/2026/03/14/261.html
    - https://fabricmc.net/2025/12/05/12111.html

---

## 3. Gaps: wishes nobody fulfils well yet

Ranked by how clearly the evidence points to a gap. "Inferred" means the gap is implied by complaints or by fragmented mod listings, not by an explicit player wish list (none was reachable).

1. **One coherent in-world HUD editor across mods (inferred, strong).** Every HUD mod ships its own editor (SpotiHUD, py54 crunch, MusiCraft). Players must learn a different layout tool per mod. No single shared editor was found. Relevant to the Vibey HUD editor direction.
   - https://modrinth.com/mod/spotihud
   - https://modrinth.com/mod/py54-crunch
   - https://www.curseforge.com/minecraft/mc-mods/musimenu
2. **Media, music and video in-world that is not Windows-only and not local-only (inferred, strong).** HUD Music Control and SpotiHUD read the Windows media session; Media HUD plays local files only; a music player mod handles URLs and search but is a separate install. Gaps: cross-platform, in-world playback, and a single UI across Spotify, built-in music, and local files.
   - https://modrinth.com/mod/spotihud
   - https://www.curseforge.com/minecraft/mc-mods/hud-music-control
   - https://modrinth.com/mod/media-hud
3. **Lightweight waypoints without the minimap cost (medium, explicit).** The CList-plus author built their mod because waypoint mods "always have built-in minimap which lagged my game even when it was off." Strong, specific gap: waypoints decoupled from minimap rendering.
   - https://github.com/coredex-source/CList-plus
4. **Zero-ads, zero-upsell, telemetry-off launcher (medium-strong, explicit via removal tool).** NoMoreLunarPlus exists only because the official launcher has ads and upsells. A clean option is a clear gap.
   - https://github.com/Zyhloh/NoMoreLunarPlus
5. **Own-mods-respected client (medium-strong).** Lunar overrides user mods, deletes them, and forces older builds. Players want a launcher that respects their mod set and their Fabric versions. Quote: "Going back to the normal launcher is practically impossible for me now" (a 5-star review that still reads as lock-in frustration).
   - https://www.trustpilot.com/review/lunarclient.com
6. **Replay / recording that is simple and built in (medium).** Lunar's rewind is praised; the Replay Mod is complex and aged. The gap is a simple, in-client rewind plus a clip export.
   - https://www.trustpilot.com/review/lunarclient.com
7. **Accessibility that combines colour-blind, text, and UI scale in one place (medium).** Colour-blind mods exist (ColorBlindness Client, Runic's Colorblind Aid, Colorblind+), but they are separate. Players ask Mojang for a larger GUI scale and built-in colour-blind modes. No single combined mod was found.
   - https://www.curseforge.com/minecraft/mc-mods/colorblindness-client
   - https://feedback.minecraft.net/hc/en-us/community/posts/44860776599821-Colorblind-option-for-Minecraft
   - https://www.craftmc.net/minecraft-clients/for/accessibility
8. **Fair, transparent cosmetics (medium).** The shop-with-soft-lock pattern and the Lunar store changes suggest a gap for cosmetics that are optional, cheap, and clearly priced. Essential's remover mod shows demand for turning the layer off entirely.
   - https://www.lunarclient.com/news/client-status-update
   - https://moddex.gg/mod/essential-mod/reviews/4037
9. **Fast new-version support (medium, structural).** Fabric's unobfuscated build helps loader and Loom, but mods lag. A client that ships compatible builds fast, or a clear compatibility matrix, is a gap. Vibey could differentiate by documenting per-version status.
   - https://fabricmc.net/2026/03/14/261.html
10. **Social features that do not need a shop (inferred, weak).** Friends and voice are praised but tied to the shop and Lunar+ tiers (chat slot upsells, best-friend limits, per NoMoreLunarPlus). Gap: free social tier.
    - https://github.com/Zyhloh/NoMoreLunarPlus
11. **Debug HUD that is readable (weak).** BetterF3 is the human-readable debug HUD mod; it shows demand for a clearer F3 replacement, but it is a niche.
    - https://www.curseforge.com/minecraft/mc-mods/blind-accessibility/install

---

## 4. What makes a menu or UI feel good vs annoying

Sources are the review quotes and the GitHub/README motivations. Few are direct praise of UI design; most are complaints. Use these as design guardrails.

**Annoying (quotes and themes):**
- "endless menus" (Lunar Trustpilot) — navigation depth is a complaint.
- Ads on nearly every screen ("ads on nearly every screen, along with ads for the cosmetics") — ad slots inside the UI, not only the launcher edge.
- Upsell gates inside functional features: chat member limits, best-friend limits, radio "Upgrade to Premium" button (NoMoreLunarPlus list).
- Launcher that overrides user state: "replacing a newer Sodium version with an older one."
- Cluttered launch: "5+ minutes" launch.
- Not really uninstalling: "leaves 90% of its shitty files behind" (Trustpilot).
- Author's view on heavy waypoint UI: "built-in minimap which lagged my game even when it was off."

**Good (inferred from praise and listicles):**
- "Easy to use," "built-in mods, profiles" (Lunar praise) — one-screen setup.
- Lightweight defaults ("Feather is the lightweight option," "more stable on low-end") — fewer panels, less text.
- Drag-and-drop HUD editing with visual scaling (SpotiHUD: "visual editor for dragging the HUD anywhere on screen and scaling it"; MusiCraft drag-and-drop; py54 crunch per-line drag).
- Hotkey-driven controls with customisable binds (HUD Music Control's "menu opened with M by default," with auto-hide).
- Minimal text, no shop prompts on the main path.

**Design implications for a Vibey-style menu:**
- No upsell or ad surfaces in the main screen flow.
- One drag-to-place HUD editor with scale and snap, shared across features.
- Respect user mods and versions; never overwrite them silently.
- Settings grouped, with a clear "off" for cosmetics and telemetry.
- Hotkey to open, auto-hide option, and no forced launcher update.

Sources for this section:
- https://www.trustpilot.com/review/lunarclient.com
- https://www.trustpilot.com/review/client.badlion.net
- https://github.com/Zyhloh/NoMoreLunarPlus
- https://modrinth.com/mod/spotihud
- https://www.curseforge.com/minecraft/mc-mods/hud-music-control
- https://github.com/coredex-source/CList-plus

---

## 5. Recommended next steps (for the parent agent)

1. **Reddit is unreachable from this environment.** A human or a different fetch route (pasted export, or a Reddit search via a browser tool) is needed to confirm §1 and §2 against r/Minecraft, r/MinecraftPvP, r/fabricmc, r/LunarClient, and r/badlion.
2. Verify the "Dawn" relaunch and the Badlion/Lunar merger before quoting them anywhere.
3. Consider a short user-facing survey (Discord or a GitHub discussion) to rank features, since no usage data was found.

## 6. Source index (all URLs used)

- https://www.trustpilot.com/review/lunarclient.com (and ?page=2; ca. / pl. / jp. variants)
- https://www.trustpilot.com/review/client.badlion.net
- https://github.com/Zyhloh/NoMoreLunarPlus
- https://github.com/coredex-source/CList-plus (resolves to coredex-source/Gazetteer)
- https://www.lunarclient.com/news/client-status-update
- https://support.lunarclient.com/support/solutions/articles/60001600509-reporting-ads-on-lunar-client
- https://essential.gg/changelog
- https://moddex.gg/mod/essential-mod/reviews/1779 and /4037
- https://modrinth.com/mod/spotihud
- https://modrinth.com/mod/py54-crunch
- https://modrinth.com/mod/media-hud
- https://www.curseforge.com/minecraft/mc-mods/hud-music-control
- https://www.curseforge.com/minecraft/mc-mods/colorblindness-client
- https://www.curseforge.com/minecraft/mc-mods/blind-accessibility/install
- https://www.badlion.net/forum/thread/348298/last-post
- https://www.badlion.net/forum/thread/319303
- https://forums.lokamc.com/threads/dont-use-badlion-client-and-dont-allow-it-here.4708
- https://feedback.minecraft.net/hc/en-us/community/posts/360075153891-Further-Accessibility
- https://feedback.minecraft.net/hc/en-us/community/posts/44860776599821-Colorblind-option-for-Minecraft
- https://fabricmc.net/2026/03/14/261.html
- https://fabricmc.net/2025/12/05/12111.html
- https://cyberpost.co/what-is-the-best-fps-client-for-minecraft/
- https://www.craftmc.net/minecraft-clients/for/low-end-pcs
- https://www.craftmc.net/minecraft-clients/for/accessibility
- https://champbop.com/best-minecraft-clients/
- https://sportskeeda.com/minecraft/5-best-client-side-mods-minecraft
- https://sportskeeda.com/minecraft/6-mods-part-minecraft
- https://saashub.com/compare-feather-client-vs-lunar-client
- https://alternativeto.net/software/lunar-client/?p=2
