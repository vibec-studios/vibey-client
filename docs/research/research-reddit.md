# Research: what Minecraft Java players want / like / dislike in clients and client-side mods

Source: Arctic Shift (public Reddit archive), fetched 2026-11-19/20.
Raw JSON: `C:\Users\henny\AppData\Local\Temp\claude\5bdf51a44ffb156a\ed0810fd-975d-45a7-9340-88d47fb4de7a\scratchpad\reddit-raw\`
Scripts + hit extract: `...\scratchpad\reddit-scripts\` (`fetch.py`, `extract.py`, `hits.json`)

## 0. How much to trust this (read first)

- **Thin sample.** 43 queries planned, 21 returned data. Arctic Shift's text search (`body=` / `title=`) returned `422 Timeout. Maybe slow down a bit` on most attempts, including with small limits and narrow date windows. Plain subreddit listings worked. Result: ~768 comments/posts scanned, of which ~618 matched some keyword.
- **Coverage gaps:** `r/LunarClient` returned 0 rows for every query (likely not archived or near-empty in this mirror). `r/badlion` returned 0 for "feature". LabyMod and NoRisk: zero matching rows. Feather: only a handful of real client discussions. Most evidence comes from r/Minecraft, r/feedthebeast, r/hypixel, r/fabricmc.
- **Counts are noisy.** "Essential" matches ordinary English ("non-essential"). "fabric"/"vanilla"/"craft" match modding in general. Where a regex count looks inflated, I say so and use a strict count (client name + tone) instead.
- **Date window:** the script asked for `after=2024-01-01`. Several results are from late 2025 to 2026.
- **Scores** are Reddit upvote scores at archive time. A few threads (Essential ads, Tupenter) dominate the sums.

Treat the ranking below as a directional read. It is good enough to show themes, not to size them.

## 1. Ranked themes

Format: rank. theme. **Support** (strict count where available; regex count in brackets). Summed score. Verdict (wish / loved / complaint / UI feeling).

### 1. Trust and monetization complaints about big clients (Lunar, Essential) — COMPLAINT, strongest signal
- Support: Lunar, negative-tone rows: **101** (score 179). Essential-as-client-name, negative-tone rows: **13** (score 75). Strongest single thread: the decocraft/Essential ad-injection thread (top comments scored 44 and 34).
- Themes inside it: ads that "weren't there before", a "spyware"/telemetry claim (tracking which servers you join), AI-generated ad art, cosmetics paywall, "vibecoded" installer.
- Quotes:
  - "don't use lunar spyware (literally tracks every server you play on), unoptomized bloated mess, with advertisements that weren't there before" — https://reddit.com/r/hypixel/comments/1uxzket/hypixel_bed_wars_is_so_ass/oxv9jpk/
  - "lunar client just wants to sell some shitty cosmetics. i don't see why anyone would use it over fabric with some preferred mods." (score 10) — https://reddit.com/r/Minecraft/comments/1xvxocv/quitting_luner_client_after_5_years_gg/19sm7kumr7dfl/
  - "The modrinth page for the mod decocraft ... states it 'Contains advertising: Essential Mod'" (score 34) — https://reddit.com/r/feedthebeast/comments/1xkfl54/just_started_my_game_and_saw_this_essential_mod_ad/19rxwgjlrzrgi/

### 2. Launchers and clients that break — login, crash, installer — COMPLAINT
- Support: **60** distinct rows [regex, includes some false positives] (score 124). Client-specific examples: Lunar login, Lunar crash after update, Feather launcher, Feather needing a manual re-install after an update.
- Quotes:
  - "I cannot log in my minecraft in the launcher, in lunar ... it says ... 'Unknow error in adding the minecraft account'" (score 21) — https://reddit.com/r/Minecraft/comments/1tvj1pm/i_cannot_log_in_my_minecraft_in_the_launcher_in/
  - "lunar is a shitty client lmao, they can't even get a basic installer running now" — https://reddit.com/r/hypixel/comments/1xv97qe/lunar_client_keeps_on_crashing_every_time_there/19sku2s4qjqio/
  - "feather launcher was saying I don't have a 'java account' when I obviously do" — https://reddit.com/r/Minecraft/comments/1uzyevo/feather_launcher_was_saying_i_dont_have_a_java/
  - Feather recovery guide after an update: https://reddit.com/r/fabricmc/comments/1vj1zpn/how_to_get_feather_client_back_after_dawn_update/

### 3. Hypixel / PvP client meta: Badlion closure, bans, minimaps — UI FEELING + COMPLAINT
- Support: Badlion, 38 rows (score 119), almost all in r/hypixel. Hypixel-PvP-client chatter overall: **70** rows (score 328) [regex].
- Themes: Badlion shutting down pushed PvP players to Hypixel and made it "sweatier"; Badlion now reads as owned by / merged into Lunar; people ask whether a minimap is bannable.
- Quotes:
  - "everyone started sweating their ass off on hypixel when badlion and mcc closed, as a result the server is more sweaty" (score 15) — https://reddit.com/r/hypixel/comments/1x2r091/why_do_you_think_hypixel_fell_off/pf4nqeb/
  - "Badlion will probably end up being merged into Lunar at some point anyway" — https://reddit.com/r/hypixel/comments/1upnrbb/badlion_vs_lunar_in_2026/ow1hea5/
  - "All mods are considered 'use at your own risk' if it gives you an advantage, even slight" (on minimaps being bannable) — https://reddit.com/r/hypixel/comments/1tzd0vp/why_are_minimaps_bannable/oqa6t79/

### 4. Players want a vanilla-plus stack they control (Prism / Fabric / Modrinth) instead of a bundled client — WISH / UI FEELING
- Support: spot-checked **~20** comments and posts. Regex count (207, score 4465) is inflated by generic "fabric"/"vanilla"/"mod" matches; do not quote that number.
- Quotes:
  - "I left lunar client to play on prism. It's literally the best one. It's simple and launches very fast in seconds. You can add mods from multiple sources." — https://reddit.com/r/Minecraft/comments/1xucy9x/returning_player_here_should_i_use_a_client/19sj2fzwg2m9w/
  - "Lunar Client is literally just a launcher that is packaged with a performance/QoL modpack. Ergo it does nothing exclusive that Prism Launcher with a modpack ... already does." — https://reddit.com/r/Minecraft/comments/1xvxocv/quitting_luner_client_after_5_years_gg/19sm8y5ac7mpu/
  - "Only big 2 prepackaged clients are lunar and badlion. Otherwise you would use forge and would have to get your own reputable mods from modrinth" — https://reddit.com/r/hypixel/comments/1xynhmb/which_client_for_bedwars_for_a_returning_player/19ssjtvxsqxq8/

### 5. Small, single-purpose client-side QoL mods get the most upvotes — LOVED
- Support: the highest-scoring relevant items are *showcase posts* by mod authors, not complaints. Examples: Tupenter (resend/command helper; post score 178 on r/feedthebeast, 25 on r/Minecraft), craft-from-reachable-chests (85), Chat Spellcheck (27), Healthify RPG health bars (30), a Fabric utility client with zoom/freelook/HUD (3), Yogi Essentials with HUD/waypoints/PvP tools (2).
- Quotes:
  - "I made a client-side command-helper mod that lets you resend with a key ... always having to use the T + up + enter combo" — https://reddit.com/r/feedthebeast/comments/1wjrpsb/i_made_a_clientside_commandhelper_mod_that_lets/
  - "Healthify: a client-side RPG health bar mod with damage numbers ... works in any modpack or on any server" — https://reddit.com/r/feedthebeast/comments/1xlfq1w/healthify_a_clientside_rpg_health_bar_mod_with/
  - "I made an open-source utility client for Fabric 1.21.1 — zoom, fullbright, freelook, custom hand/hit animations, and a movable HUD. No cheats, code is public." — https://reddit.com/r/fabricmc/comments/1uytijf/i_made_an_opensource_utility_client_for_fabric/

### 6. Bloat and feature creep in client mods — COMPLAINT
- Support: 22 rows (score 566, but ~500 of that is one Essential-bloat thread). Mostly about Essential's "non-essential" catalog and about vanilla content updates ("useless bloat mobs").
- Quote: "For a mod called Essential they have a lot of non-essential bullshit." — https://reddit.com/r/feedthebeast/comments/1xz829a/reverie_aka_the_people_behind_reconsole_and_a/19st747yqown5/
- Note: the "bloat" hits about vanilla content updates (r/Minecraft, low score) are a different complaint. Do not merge them.

### 7. Minimap, waypoints, HUD — LOVED, but a bannable-on-PvP-servers caveat
- Support: 50 rows (score 47) [regex]. Xaero's Minimap is the referenced default; "customizable HUD / waypoints" appears in several showcases. Low-scoring overall. Players mention it as a given.
- Quotes:
  - "well, you do have xaero's minimap mod. if you got the world map mod too..." — https://reddit.com/r/Minecraft/comments/1xya8rk/my_nether_spawn_sucks_what_do_i_do/19sr5tprgqt2c/
  - "I made a client-side Fabric mod with customizable HUDs, waypoints, PvP tools, optimizations and fixes" — https://reddit.com/r/fabricmc/comments/1w2qdqa/i_made_a_clientside_fabric_mod_with_customizable/

### 8. Replay / recording / clips — WISH (features missing from clients)
- Support: only 14 rows (score 11). Small, but a clear point: Lunar dropped the Replay Mod; players still want built-in recording.
- Quotes:
  - "Lunar Client hides Replay Mod? I use lunar client and the newest version does not have built in replay mod." — https://reddit.com/r/Minecraft/comments/1i7phol/lunar_client_hides_replay_mod/
  - Request for a full-session recording: https://reddit.com/r/fabricmc/comments/1m3958v/is_it_possible_to_record_and_entire_40_minute/p4skblv/ ("Lunar Client Rewind Mod." reply)

### 9. Cosmetic wishes — WISH (mostly vanilla-side, not clients)
- Support: 66 rows [regex] (score 237), inflated by "cosmetic" in non-client contexts. Real client-cosmetic wishes are few.
- Quote: "id randomly love if music discs could be equipped as cosmetics for these throwables" — https://reddit.com/r/Minecraft/comments/1xwgssk/minecraft_new_throwing_weapon_is_op/19sprpdko0e1v/

### 10. Explicit "I want / should add" requests — WISH (mixed targets)
- Support: 136 rows [regex, many false positives] (score 194). Most are vanilla content wishes or "is there a mod for X?" questions, not client complaints.
- Quote: "There should be a map or compass like item that is progression based like needing diamonds to craft or something that you can equip in an armor slot to get a minimap." — https://reddit.com/r/Minecraft/comments/1xzlwp6/tell_me_your_biggest_minecraft_hot_take/19su1g7ty1ijp/

### 11. Performance and FPS — UI FEELING (judged by what players use)
- Support: 58 rows [regex] (score 79). Badlion is praised for performance and motion blur in one 2026 comparison post; another player says Badlion "performance" is better for them, but "roughly the same" for most.
- Quotes:
  - "I like the motionblur and it fixed the glitch..." (Badlion vs Lunar in 2026) — https://reddit.com/r/hypixel/comments/1upnrbb/badlion_vs_lunar_in_2026/
  - "I myself get much better performance on badlion, but for most it's roughly the same." — https://reddit.com/r/hypixel/comments/1sagcl8/badlion_vs_luna_client_whats_the_difference/odvhl1z/

## 2. UI feelings (summary)

- **Lunar:** widely used for Hypixel/PvP, but now read as bloated, ad-laden and untrustworthy; the "spyware" and "sell cosmetics" framing appears repeatedly. Defenders say it is "a performance/QoL modpack" in a launcher.
- **Essential:** strong dislike for ads in partner mods and for "non-essential" catalog bloat; defenders say the author chose the ads, not Essential.
- **Badlion:** nostalgia and praise for PvP feel and performance; closure is seen as a loss; now tied to Lunar.
- **Feather:** mentioned with launcher bugs and update breakage; little positive or negative sentiment in this sample.
- **Prism / vanilla-plus:** the default recommendation for returning players. "simple", "launches fast", "add mods from multiple sources".
- **Small QoL mods (chat, resend, craft, HUD):** the most upvoted content in the sample. Authors get warm reception.

## 3. Feature requests nobody serves well

1. **An ad-free, telemetry-free client with a usable mod menu.** The closest thing is a fabricmc project saying: "the goal of this project is to enable absolutely all players to play from the client at the Lunar level, but without mc premium, the main task now is to code a perfect mod menu" — https://reddit.com/r/fabricmc/comments/1vqlpsl/looking_for_a_minecraft_client_developer_12111/p4kphxf/
2. **Replay / recording built into a client.** Lunar dropped the Replay Mod; no clear replacement appears in the sample.
3. **Launchers that survive updates.** Lunar installer and login breakage; Feather needed a manual fix after an update.
4. **A clear, published policy on which HUD tools (minimap, waypoints) are allowed on PvP servers.** Players ask whether minimaps are bannable and get "use at your own risk" answers.
5. **Transparent ad policy for mods that bundle partner ads.** The decocraft/Essential thread shows players want to know before installing.
6. **Progression-based map/compass items in vanilla** (requested as a vanilla wish, but it overlaps with what client minimaps provide).
7. **Music-disc or item cosmetics** for throwables — a small, specific cosmetic wish.

## 4. Strongly supported vs. weakly supported

- Strongly supported (multiple independent threads, consistent tone): trust/ads/telemetry complaints about Lunar and Essential; launcher and login breakage; "prefer Prism/Fabric with my own mods"; small QoL mods are liked.
- Moderately supported: Badlion closure and Hypixel PvP meta; minimap bannability questions; replay-mod removal.
- Weakly supported (few rows, or dominated by one thread): bloat as a general client complaint; cosmetic wishes; performance comparisons; LabyMod and NoRisk (zero rows in this sample — no finding).

## 5. Gaps and next steps

- Re-run the text searches for `r/LunarClient`, `r/badlion`, `r/labymod`, `r/NoRiskClient` once Arctic Shift text search is healthy. Those subreddits returned nothing here.
- The "PvP-ish" and "MinecraftBuddies"/"technicalminecraft"/"ModdedMinecraft" subs were not covered.
- Consider a month-by-month plain-listing scan of r/Minecraft and r/feedthebeast with local keyword filtering, if text search stays broken. Plain listings work, but at 100 items per request and a 3 s spacing it is slow.
- The regex counts in section 1 are indicative only. Any number that will be quoted externally should be re-counted with a stricter, hand-checked filter.
