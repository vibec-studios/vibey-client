# Chat module (`chat`)

Features 17–19 of [FEATURES.md](FEATURES.md): a better vanilla chat. Fabric 1.21.11 only for now
(`gradle/modules.gradle`). Everything is client side, nothing is sent anywhere, every part can be switched off.

> See also: [MODULES.md](MODULES.md) (module API) · [HUD.md](HUD.md) (the chat window is a HUD element) ·
> [NAMETAGS.md](NAMETAGS.md) (rank icons in chat) · [SOCIAL.md](SOCIAL.md) (streamer mode hides names in chat)

## What the player gets

| Feature | How it works |
|---|---|
| Timestamps | `[14:05]` before every line (clock: 24 h, 24 h with seconds, 12 h). Wall clock time of arrival. |
| History | Up to 2000 lines (default 500, vanilla 100) for scrolling and search. *Keep on leave* keeps the chat when you leave a world or server. |
| Search | **Ctrl+F** in the open chat (or the *Search chat* key, unbound by default). The input line becomes the search field, the chat shows only matching messages with the hits underlined, a small bar above the input shows *n found* and ✕. Esc or Ctrl+F ends it and gives back what you were typing. |
| Copy a line | Hover a line in the open chat: the line lights up and a copy icon appears at its right end. Click = the whole message (all wrapped lines, without the timestamp) goes to the clipboard, a short *Copied* toast confirms. |
| Mention highlight | Your name (and *Extra words*, comma separated) as a whole word, case-insensitive: the words turn yellow + bold and the line gets the yellow chat tag on the left (hover: *Mentions you*). Your own messages never count. |
| Mention + DM toasts | A mention or a private message while the chat is closed shows a vui toast (top of the toast stack, 6 s, pauses while hovered). DM toast: sender as title and a **Reply** button that opens the chat with `/msg <name> `; mention toast: **Open**. Optional ping sound. At most one toast per 1.5 s. |
| Collapse repeats | The same line again within 60 s replaces the previous one and gets `×2`, `×3` … (up to 999). Only the first one can toast. |
| Filters | Lines matching a filter are never shown (not even logged in the chat). A filter is a plain word/phrase (case-insensitive *contains*) or a `/regex/`. Editor: module page → **Filters** (vui screen `vui/chat/filters.vui`). |
| Chat window | The chat is the HUD element `chat.window` (label *Chat*): drag it in the HUD editor, wheel resizes it, hide it there. The open chat (typing, scrolling, clicking links) stays in the same place. *Move chat* off = vanilla position. |

DM detection: vanilla `/msg` (`commands.message.display.incoming`, sender and text from the translation arguments)
and the common plugin formats (`[Name -> me] …`, `Name whispers to you: …`, `From Name: …`, German variants), see
`core/DmPatterns`.

## Code map

| Package | Job |
|---|---|
| `module/chat/core` (pure, JUnit) | `Timestamps`/`TimeStyle`, `MentionMatcher` + `SenderPrefix` (own messages), `SpamCollapse`, `ChatEntry`, `FilterRule`/`ChatFilters`, `SearchQuery`, `Ranges`, `DmPatterns`/`DirectMessage`, `HistorySize`, `Words` |
| `module/chat/hook` | `ChatHooks` (entry points of the mixins), `IncomingPipeline` (filter → collapse → DM/mention → toasts), `ChatIndex` (identity maps `GuiMessage → ChatEntry`, `Line → GuiMessage`, pruned to the live lists), `ChatRefresh` (re-layout on setting/privacy changes), `DmDetector`, `Incoming` |
| `module/chat/line` | `LineDecor` (the displayed component: names redacted, mention + search restyled, timestamp prefix, `×n` suffix), `Mentions`, `MentionTag`, `Redact`, `LineStyles` |
| `module/chat/notify` | `ChatNotifier`, `ChatToasts` (`VuiMc.toasts()`), `ChatSound` |
| `module/chat/screen` | `SearchMode`/`SearchState`/`SearchController` (overlay `vui/chat/search.vui`), `CopyButton`, `ChatGeometry` + `LineHit` (which line is under the mouse, also when the chat was moved), `ChatKeys` |
| `module/chat/window` | `ChatWindow` (HUD element + transform), `ChatTransform`, `ChatSize`, `ChatPreview` (what the HUD editor draws) |
| `module/chat/filters` | `FilterStore` (patterns in `config/vibeyclient/chat.json` → `_data.filters`), `FilterScreens`, `FilterController`, `FilterRow` |
| `client/text` (shared, all targets) | `StyledText` + `TextEdit`: replace or restyle character ranges of a `Component` and keep every other style (click/hover events, fonts such as the rank icon font) |

### Hooks (`vibeyclient.chat.mixins.json`)

All in `ChatComponent`, `ChatScreen` and `Gui`; **no** `ChatListener` mixin, so the nametags module's
`ChatListenerMixin` (sender name icons, [NAMETAGS.md](NAMETAGS.md)) stays the only one there and its icons reach us
inside the component.

| Mixin | Where | What |
|---|---|---|
| `ChatComponentMixin` | `addMessage(Component, MessageSignature, GuiMessageTag)` HEAD | `IncomingPipeline`: drop (filter), collapse (removes the previous message and its lines), mention tag (re-enters `addMessage` with the tag) |
| | `addMessageToDisplayQueue` HEAD | binds the pending `ChatEntry`, hides non-matching messages while searching |
| | `GuiMessage.splitLines` (WrapOperation) | lays out the decorated component instead of the raw one |
| | `List.addFirst` (ModifyArg) | remembers which `GuiMessage` each `Line` belongs to (copy, collapse) |
| | constant `100` in both queues | history size |
| | `ChatGraphicsAccess.updatePose` (ModifyArg) in the private `render` | moves/scales the whole chat by the HUD placement – the same pose is used for drawing, hover and `captureClickableText`, so links stay clickable |
| | `render(GuiGraphics…)` HEAD | hides the unfocused chat while the HUD editor is open or the player hid it there |
| | `tick` HEAD | `ChatRefresh`, index pruning |
| `ChatScreenMixin` | `init`, `keyPressed`, `onEdited`, `mouseClicked`, `render`, `removed` | search mode, copy button |
| `GuiMixin` | `onDisconnected` → `clearMessages` | *Keep on leave* |
| `ChatComponentAccessor` | | `allMessages`, `trimmedMessages`, `chatScrollbarPos`, `refreshTrimmedMessages` |

Decoration happens when a message is laid out (arrival, resize, refresh), never per frame. A refresh re-splits all
kept messages (≤ 2000), which happens on setting changes, search typing and streamer mode toggles.

### Chat window geometry

Vanilla draws the chat with `scale(chatScale)` + `translate(4, 0)`, its bottom line at `guiHeight - 40`. The HUD
element has the size of the unfocused chat (`(ceil(width/scale) + 12) × scale` by `lines × lineHeight × scale`) and
the default placement `BOTTOM_LEFT, 0, -40`, so the default box is exactly the vanilla chat. A placed box `(x, y, w, h,
s)` maps the vanilla chat with `translate(x, y + h) · scale(s) · translate(0, -(guiHeight - 40))` in front of the
vanilla pose; `ChatGeometry` inverts it for the copy button. The open chat grows upwards from the box's bottom edge.

## Memory bounds

- `allMessages`/`trimmedMessages`: the history setting (100–2000).
- `ChatIndex`: identity maps, pruned every 100 ticks to the live lists (+32 / +64 slack).
- Filters: ≤ 64 rules, ≤ 120 chars each, matched against the first 1024 characters of a line.
- Search: ≤ 8 terms, runs over the kept history only.

## Settings

Module page sections *Lines* (timestamps, clock, copy button, move chat, history, keep on leave), *Mentions*
(highlight, extra words, mention toast, message toast, ping sound), *Filters* (collapse repeats, hide patterns) and the
**Filters** button; key *Search chat* (unbound). No server rules: nothing here is an advantage.

## Tests

- JUnit: `MentionMatcherTest`, `ChatCoreTest` (timestamps, collapse, filters, search, DM patterns, history),
  `NameRedactorTest` (privacy) – `./gradlew :fabric-1.21.11:test --tests "dev.henny.vibeyclient.module.chat.*"`.
- Gametest `ChatGameTest` (`-Dvibeyclient.test.only=ChatGameTest`, ~1.5 min): timestamps on/off, mention (tag, toast,
  own messages), DM toast + Reply, collapse ×4, filter + editor, Ctrl+F search + Esc, copy button + clipboard, streamer
  mode (chat aliases, tab list, default skins, F7), chat moved and shrunk in the HUD editor and copy on the moved
  chat, masked address field. Screenshots: `build/review/chat/`.

## Not done yet

- Middle-drag of the chat and of the toast stack: the current HUD editor moves with the left button; middle-drag
  comes with the vui HUD editor. The vui `ToastStack` has no movable position yet.
- Other targets: the mixins target the 1.21.6+ chat renderer (`ChatGraphicsAccess`); 1.21.1–1.21.5 and 26.x need
  their own render hooks.
