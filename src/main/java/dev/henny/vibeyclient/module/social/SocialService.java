package dev.henny.vibeyclient.module.social;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.backend.BackendClient;
import dev.henny.vibeyclient.backend.J;
import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.ChatMember;
import dev.henny.vibeyclient.module.social.model.Model.Device;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.module.social.model.Model.FriendRequest;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.model.Model.Invite;
import dev.henny.vibeyclient.module.social.model.Model.Me;
import dev.henny.vibeyclient.module.social.model.Model.Message;
import dev.henny.vibeyclient.module.social.model.Model.Notification;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.SearchResult;
import dev.henny.vibeyclient.module.social.model.Model.User;
import dev.henny.vibeyclient.ui.toast.Heads;
import dev.henny.vibeyclient.ui.toast.ToastStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

import static dev.henny.vibeyclient.backend.BackendClient.enc;

/**
 * The social logic: loads the state after {@code ready}, applies every gateway event to {@link SocialState}, runs
 * the user's actions against the REST API and raises toasts. Client thread, except the HTTP calls themselves.
 */
public final class SocialService {
    /** Runs continuations on the client thread. */
    public static final Executor MAIN = Tasks::onMain;

    private final SocialModule module;
    private final BackendClient backend;
    public final SocialState state = new SocialState();
    private final List<AutoCloseable> handlers = new ArrayList<>();
    private long readyAt;
    private String visibleChat;
    private long visibleUntil;
    private final Map<String, Long> lastTyping = new HashMap<>();

    public SocialService(SocialModule module, BackendClient backend) {
        this.module = module;
        this.backend = backend;
    }

    public BackendClient backend() {
        return backend;
    }

    public boolean ready() {
        return backend.isReady() && state.me != null;
    }

    // ============================================================================================ wiring

    public void install() {
        on(BackendClient.EV_READY, this::onReady);
        on(BackendClient.EV_STATE, d -> {
            String s = J.str(d, "state");
            if ("LOGGED_OUT".equals(s) || "ERROR".equals(s)) {
                state.clear();
            }
            state.changed();
        });
        on("presence.update", d -> {
            Presence p = Presence.from(d);
            Presence before = state.presenceOf(p.userId());
            state.applyPresence(p);
            if (before != null && !before.online() && p.online() && System.currentTimeMillis() - readyAt > 8_000
                    && module.toastFriendOnline.on()) {
                Friend f = state.friends.get(p.userId());
                if (f != null) toast(module.tr("toast.online", f.user().name()), activityText(p), f.user().id(),
                        () -> module.openSidebar(SidebarRoute.profile(f.user().id())), "online:" + f.user().id());
            }
        });
        on("friend.request", d -> {
            FriendRequest r = FriendRequest.from(d);
            remember(r.from());
            remember(r.to());
            state.requests.put(r.id(), r);
            state.changed();
            if (r.incoming() && r.from() != null && module.toastRequests.on() && !module.dnd()) {
                ToastStack.show(new Notify.Toast(module.tr("toast.request"), r.from().name(), Notify.Kind.SOCIAL, r.from().id(),
                                () -> module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.REQUESTS))),
                        List.of(new ToastStack.Button(module.tr("action.accept"), true, () -> acceptRequest(r.id())),
                                new ToastStack.Button(module.tr("action.decline"), false, () -> declineRequest(r.id()))),
                        0, "req:" + r.id());
            }
        });
        on("friend.request.removed", d -> {
            state.requests.remove(J.str(d, "id"));
            ToastStack.dismiss("req:" + J.str(d, "id"));
            state.changed();
        });
        on("friend.added", d -> {
            Friend f = Friend.from(d);
            if (f == null) return;
            remember(f.user());
            boolean wasOutgoing = false;
            for (var it = state.requests.values().iterator(); it.hasNext(); ) {
                FriendRequest r = it.next();
                if (r.other() != null && r.other().id().equals(f.user().id())) {
                    wasOutgoing |= !r.incoming();
                    it.remove();
                    ToastStack.dismiss("req:" + r.id());
                }
            }
            state.friends.put(f.user().id(), f);
            state.changed();
            if (wasOutgoing && module.toastRequests.on()) {
                toast(module.tr("toast.accepted", f.user().name()), null, f.user().id(),
                        () -> module.openSidebar(SidebarRoute.profile(f.user().id())), null);
            }
        });
        on("friend.removed", d -> {
            state.friends.remove(J.str(d, "userId"));
            state.changed();
        });
        on("user.blocked", d -> {
            String id = J.str(d, "userId");
            state.friends.remove(id);
            state.requests.values().removeIf(r -> r.other() != null && r.other().id().equals(id));
            loadBlocks();
            state.changed();
        });
        on("user.unblocked", d -> {
            state.blocked.remove(J.str(d, "userId"));
            state.changed();
        });
        on("user.updated", d -> {
            User u = User.from(d);
            remember(u);
            state.applyUser(u);
        });
        on("me.updated", d -> {
            state.me = Me.from(d);
            if (state.me != null) remember(state.me.user());
            state.changed();
        });
        on("chat.created", this::putChat);
        on("chat.updated", this::putChat);
        on("chat.removed", d -> {
            String id = J.str(d, "chatId");
            state.chats.remove(id);
            state.histories.remove(id);
            state.changed();
        });
        on("chat.read", d -> state.applyChatRead(J.str(d, "chatId", ""), J.str(d, "userId", ""), J.str(d, "messageId")));
        on("message.created", d -> onMessage(Message.from(d), true));
        on("message.updated", d -> onMessage(Message.from(d), false));
        on("message.deleted", d -> state.applyMessageDeleted(J.str(d, "chatId", ""), J.str(d, "messageId", "")));
        on("typing", d -> {
            String chat = J.str(d, "chatId");
            String user = J.str(d, "userId");
            if (chat == null || user == null) return;
            long until = System.currentTimeMillis() + 8_000;
            state.typing.computeIfAbsent(chat, k -> new HashMap<>()).put(user, until);
            state.changed();
        });
        on("invite.received", d -> {
            Invite i = Invite.from(d);
            remember(i.from());
            state.invites.put(i.id(), i);
            state.changed();
            if (i.open() && i.from() != null && !i.from().id().equals(state.selfId()) && module.toastInvites.on() && !module.dnd()) {
                showInviteToast(i);
            }
        });
        on("invite.updated", d -> {
            Invite i = Invite.from(d);
            state.invites.put(i.id(), i);
            if (!i.open()) ToastStack.dismiss("inv:" + i.id());
            state.changed();
        });
        on("hosting.update", d -> applyHost(HostSession.from(d)));
        on("hosting.ended", d -> {
            String id = J.str(d, "id");
            state.friendHosts.remove(id);
            if (state.myHost != null && state.myHost.id().equals(id)) state.myHost = null;
            state.changed();
        });
        on("notification.created", d -> {
            Notification n = Notification.from(d);
            state.notifications.add(0, n);
            if (n.unread()) state.notificationsUnread++;
            state.changed();
            // friend requests and invites already have their own toasts
            boolean covered = n.type().startsWith("friend_") || n.type().equals("invite");
            if (!covered && !module.dnd() && module.toastSystem.on()) {
                toast(notificationTitle(n), n.body(), "!", () -> module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.INBOX)), "notif:" + n.id());
            }
        });
        on("settings.updated", d -> {
            applyFeatures(J.obj(d, "features"));
            state.motd = J.str(d, "motd");
            state.changed();
        });
    }

    public void uninstall() {
        for (AutoCloseable c : handlers) {
            try {
                c.close();
            } catch (Exception ignored) {
            }
        }
        handlers.clear();
    }

    private void on(String type, Consumer<JsonObject> h) {
        handlers.add(backend.onEvent(type, d -> {
            try {
                h.accept(d);
            } catch (RuntimeException e) {
                module.logError("event " + type, e);
            }
        }));
    }

    // ============================================================================================ loading

    private void onReady(JsonObject d) {
        readyAt = System.currentTimeMillis();
        state.clear();
        state.me = Me.from(J.obj(d, "user"));
        if (state.me != null) remember(state.me.user());
        for (Friend f : J.list(d.get("friends"), Friend::from)) {
            remember(f.user());
            state.friends.put(f.user().id(), f);
        }
        applyFeatures(J.obj(d, "features"));
        state.loaded = true;
        state.changed();
        module.presence().onReady();
        reloadRest();
    }

    /** Everything that does not come with {@code ready}. */
    public void reloadRest() {
        get("/api/v1/friends/requests", e -> {
            state.requests.clear();
            for (FriendRequest r : J.list(e, FriendRequest::from)) {
                remember(r.from());
                remember(r.to());
                state.requests.put(r.id(), r);
            }
        });
        loadBlocks();
        get("/api/v1/chats", e -> {
            state.chats.clear();
            for (Chat c : J.list(e, Chat::from)) putChatQuiet(c);
        });
        get("/api/v1/invites?direction=incoming&pendingOnly=true", e -> {
            for (Invite i : J.list(e, Invite::from)) {
                remember(i.from());
                state.invites.put(i.id(), i);
            }
        });
        loadNotifications(false);
        refreshHosts();
        backend.publicGet("/api/v1/meta").thenAcceptAsync(e -> {
            JsonObject m = J.obj(e);
            JsonObject limits = J.obj(m, "limits");
            state.chatMaxLength = J.num(limits, "chatMessageMaxLength", state.chatMaxLength);
            state.maxGroupMembers = J.num(limits, "maxGroupMembers", state.maxGroupMembers);
            applyFeatures(J.obj(m, "features"));
            state.motd = J.str(m, "motd");
            state.changed();
        }, MAIN);
    }

    private void loadBlocks() {
        get("/api/v1/blocks", e -> {
            state.blocked.clear();
            for (User u : J.list(e, User::from)) state.blocked.put(u.id(), u);
        });
    }

    public void refreshHosts() {
        if (!state.feature("hosting")) return;
        backend.request("GET", "/api/v1/hosting/friends", null).thenAcceptAsync(e -> {
            state.friendHosts.clear();
            for (HostSession s : J.list(e, HostSession::from)) if (s.online()) state.friendHosts.put(s.id(), s);
            state.changed();
        }, MAIN).exceptionally(t -> null);
        backend.request("GET", "/api/v1/hosting/sessions", null).thenAcceptAsync(e -> {
            state.myHost = null;
            for (HostSession s : J.list(e, HostSession::from)) if (s.online()) state.myHost = s;
            state.changed();
        }, MAIN).exceptionally(t -> null);
    }

    public void loadNotifications(boolean more) {
        String path = "/api/v1/notifications?limit=50";
        if (more && !state.notifications.isEmpty()) {
            path += "&before=" + enc(state.notifications.get(state.notifications.size() - 1).id());
        }
        boolean append = more;
        get(path, e -> {
            JsonObject o = J.obj(e);
            List<Notification> list = J.list(o == null ? null : o.get("notifications"), Notification::from);
            if (!append) state.notifications.clear();
            state.notifications.addAll(list);
            state.notificationsUnread = J.num(o, "unreadCount", state.notificationsUnread);
            state.notificationsHasMore = list.size() >= 50;
        });
    }

    private void get(String path, Consumer<JsonElement> apply) {
        backend.request("GET", path, null).thenAcceptAsync(e -> {
            apply.accept(e);
            state.changed();
        }, MAIN).exceptionally(t -> {
            String code = BackendClient.unwrap(t).code;
            if (!"feature_disabled".equals(code)) module.log().warn("GET {} failed: {}", path, Errors.text(t));
            return null;
        });
    }

    private void applyFeatures(JsonObject f) {
        if (f == null) return;
        for (var e : f.entrySet()) {
            if (e.getValue().isJsonPrimitive() && e.getValue().getAsJsonPrimitive().isBoolean()) {
                state.features.put(e.getKey(), e.getValue().getAsBoolean());
            }
        }
    }

    private static void remember(User u) {
        if (u != null) Heads.remember(u.id(), u.skinUrl());
    }

    private void putChat(JsonObject d) {
        putChatQuiet(Chat.from(d));
        state.changed();
    }

    private void putChatQuiet(Chat c) {
        if (c == null) return;
        for (ChatMember m : c.members()) remember(m.user());
        if (isVisible(c.id())) c = c.withUnread(0);
        state.chats.put(c.id(), c);
    }

    private void applyHost(HostSession s) {
        if (s == null) return;
        remember(s.host());
        boolean mine = s.host() != null && s.host().id().equals(state.selfId());
        if (mine) {
            state.myHost = s.online() ? s : null;
        } else if (s.online()) {
            state.friendHosts.put(s.id(), s);
        } else {
            state.friendHosts.remove(s.id());
        }
        state.changed();
    }

    // ============================================================================================ chats

    /** The chat is on screen this frame (no unread counting, no toasts, mark as read). */
    public void markVisible(String chatId) {
        boolean newly = !chatId.equals(visibleChat) || System.currentTimeMillis() > visibleUntil;
        visibleChat = chatId;
        visibleUntil = System.currentTimeMillis() + 400;
        Chat c = state.chats.get(chatId);
        if (c != null && c.unreadCount() > 0) {
            state.chats.put(chatId, c.withUnread(0));
            state.changed();
            markRead(chatId);
        } else if (newly && c != null && c.unreadCount() > 0) {
            markRead(chatId);
        }
    }

    public boolean isVisible(String chatId) {
        return chatId.equals(visibleChat) && System.currentTimeMillis() <= visibleUntil;
    }

    private void markRead(String chatId) {
        Tasks.debounce("vibeyclient-social-read-" + chatId, 400,
                () -> backend.request("POST", "/api/v1/chats/" + enc(chatId) + "/read", new JsonObject()).exceptionally(t -> null));
    }

    private void onMessage(Message m, boolean created) {
        if (m == null) return;
        boolean visible = isVisible(m.chatId());
        boolean mine = m.authorId() != null && m.authorId().equals(state.selfId());
        state.applyMessage(m, created, visible);
        if (!created || mine) return;
        if (visible) {
            markRead(m.chatId());
            return;
        }
        if (!module.toastMessages.on() || module.dnd() || "system".equals(m.kind())) return;
        Chat c = state.chats.get(m.chatId());
        String author = state.nameOf(m.authorId());
        String title = c == null || c.dm() ? author : author + " · " + chatName(c);
        String body = "invite".equals(m.kind()) && m.invite() != null ? module.tr("chat.inviteCard", m.invite().title()) : m.content();
        toast(title, body, m.authorId(), () -> module.openSidebar(SidebarRoute.chat(m.chatId())), "chat:" + m.chatId());
    }

    public String chatName(Chat c) {
        if (c == null) return "?";
        if (c.dm()) {
            User o = c.other(state.selfId());
            return o == null ? "?" : o.name();
        }
        return c.name() == null || c.name().isBlank() ? module.tr("chat.group") : c.name();
    }

    public void loadHistory(String chatId, boolean older) {
        SocialState.History h = state.history(chatId);
        if (h.loading || (older && !h.hasMore) || (!older && h.loaded)) return;
        h.loading = true;
        String path = "/api/v1/chats/" + enc(chatId) + "/messages?limit=50";
        if (older) {
            for (Message m : h.messages) {
                if (!m.isPending()) {
                    path += "&before=" + enc(m.id());
                    break;
                }
            }
        }
        backend.request("GET", path, null).thenAcceptAsync(e -> {
            JsonObject o = J.obj(e);
            List<Message> list = J.list(o == null ? null : o.get("messages"), Message::from);
            state.applyHistoryPage(chatId, list, J.bool(o, "hasMore", false));
        }, MAIN).exceptionally(t -> {
            Tasks.onMain(() -> {
                h.loading = false;
                state.changed();
            });
            error(t);
            return null;
        });
    }

    public void sendMessage(String chatId, String text) {
        String content = text.trim();
        if (content.isEmpty()) return;
        if (content.length() > state.chatMaxLength) {
            Notify.warn(module.tr("chat.tooLong", state.chatMaxLength), null);
            return;
        }
        String nonce = UUID.randomUUID().toString();
        Message pending = Message.pending(chatId, state.selfId(), content, nonce);
        SocialState.History h = state.history(chatId);
        h.messages.add(pending);
        state.changed();
        backend.request("POST", "/api/v1/chats/" + enc(chatId) + "/messages", J.of("content", content, "nonce", nonce))
                .thenAcceptAsync(e -> {
                    Message m = Message.from(J.obj(e));
                    if (m != null) state.applyMessage(m, true, true);
                }, MAIN)
                .exceptionally(t -> {
                    Tasks.onMain(() -> {
                        h.messages.remove(pending);
                        state.changed();
                    });
                    error(t);
                    return null;
                });
        lastTyping.remove(chatId);
    }

    public CompletableFuture<Void> editMessage(String chatId, String messageId, String text) {
        return act("PATCH", "/api/v1/chats/" + enc(chatId) + "/messages/" + enc(messageId), J.of("content", text.trim()),
                e -> {
                    Message m = Message.from(J.obj(e));
                    if (m != null) state.applyMessage(m, false, true);
                });
    }

    public void deleteMessage(String chatId, String messageId) {
        act("DELETE", "/api/v1/chats/" + enc(chatId) + "/messages/" + enc(messageId), null,
                e -> state.applyMessageDeleted(chatId, messageId));
    }

    /** Typing indicator, at most every 3 s per chat. */
    public void typing(String chatId) {
        long now = System.currentTimeMillis();
        Long last = lastTyping.get(chatId);
        if (last != null && now - last < 3_000) return;
        lastTyping.put(chatId, now);
        backend.sendOp("typing", J.of("chatId", chatId));
    }

    public CompletableFuture<Chat> openDm(String userId) {
        Chat existing = state.dmWith(userId);
        if (existing != null) return CompletableFuture.completedFuture(existing);
        return backend.request("POST", "/api/v1/chats/dm", J.of("userId", userId)).thenApplyAsync(e -> {
            Chat c = Chat.from(J.obj(e));
            putChatQuiet(c);
            state.changed();
            return c;
        }, MAIN).whenComplete((c, t) -> {
            if (t != null) error(t);
        });
    }

    public CompletableFuture<Chat> createGroup(String name, List<String> memberIds) {
        return backend.request("POST", "/api/v1/chats", J.of("name", name.trim(), "memberIds", J.strings(memberIds)))
                .thenApplyAsync(e -> {
                    Chat c = Chat.from(J.obj(e));
                    putChatQuiet(c);
                    state.changed();
                    return c;
                }, MAIN).whenComplete((c, t) -> {
                    if (t != null) error(t);
                });
    }

    public CompletableFuture<Void> renameGroup(String chatId, String name) {
        return act("PATCH", "/api/v1/chats/" + enc(chatId), J.of("name", name.trim()), e -> putChatQuiet(Chat.from(J.obj(e))));
    }

    public CompletableFuture<Void> addMembers(String chatId, List<String> ids) {
        return act("POST", "/api/v1/chats/" + enc(chatId) + "/members", J.of("userIds", J.strings(ids)),
                e -> putChatQuiet(Chat.from(J.obj(e))));
    }

    public CompletableFuture<Void> removeMember(String chatId, String userId) {
        boolean self = userId.equals(state.selfId());
        return act("DELETE", "/api/v1/chats/" + enc(chatId) + "/members/" + enc(userId), null, e -> {
            if (self) {
                state.chats.remove(chatId);
                state.histories.remove(chatId);
            }
        });
    }

    // ============================================================================================ friends

    public CompletableFuture<List<SearchResult>> search(String q) {
        return backend.request("GET", BackendClient.query("/api/v1/users/search", "q", q, "limit", 20), null)
                .thenApply(e -> J.list(e, SearchResult::from));
    }

    /** By Minecraft name (or user id). */
    public CompletableFuture<Void> sendFriendRequest(String name, String userId) {
        JsonObject body = userId != null ? J.of("userId", userId) : J.of("name", name.trim());
        return act("POST", "/api/v1/friends/requests", body, e -> {
            JsonObject o = J.obj(e);
            FriendRequest r = J.obj(o, "request") == null ? null : FriendRequest.from(J.obj(o, "request"));
            Friend f = J.obj(o, "friend") == null ? null : Friend.from(J.obj(o, "friend"));
            if (r != null) state.requests.put(r.id(), r);
            if (f != null) state.friends.put(f.user().id(), f);
            Notify.success(module.tr(f != null ? "toast.nowFriends" : "toast.requestSent",
                    f != null ? f.user().name() : r != null && r.to() != null ? r.to().name() : name), null);
        });
    }

    public void acceptRequest(String id) {
        act("POST", "/api/v1/friends/requests/" + enc(id) + "/accept", new JsonObject(), e -> {
            state.requests.remove(id);
            Friend f = Friend.from(J.obj(e));
            if (f != null) {
                remember(f.user());
                state.friends.put(f.user().id(), f);
            }
        });
        ToastStack.dismiss("req:" + id);
    }

    public void declineRequest(String id) {
        act("POST", "/api/v1/friends/requests/" + enc(id) + "/decline", new JsonObject(), e -> state.requests.remove(id));
        ToastStack.dismiss("req:" + id);
    }

    public void cancelRequest(String id) {
        act("DELETE", "/api/v1/friends/requests/" + enc(id), null, e -> state.requests.remove(id));
    }

    public void removeFriend(String userId) {
        act("DELETE", "/api/v1/friends/" + enc(userId), null, e -> state.friends.remove(userId));
    }

    public void block(String userId) {
        User u = state.user(userId);
        act("PUT", "/api/v1/blocks/" + enc(userId), new JsonObject(), e -> {
            state.friends.remove(userId);
            if (u != null) state.blocked.put(userId, u);
            state.requests.values().removeIf(r -> r.other() != null && r.other().id().equals(userId));
        });
    }

    public void unblock(String userId) {
        act("DELETE", "/api/v1/blocks/" + enc(userId), null, e -> state.blocked.remove(userId));
    }

    // ============================================================================================ invites

    public void declineInvite(String id) {
        ToastStack.dismiss("inv:" + id);
        act("POST", "/api/v1/invites/" + enc(id) + "/decline", new JsonObject(), e -> {
            Invite i = Invite.from(J.obj(e));
            if (i != null) state.invites.put(i.id(), i);
            else state.invites.remove(id);
        });
    }

    /** Accepts and joins (leaving the current world after a confirmation). */
    public void acceptInvite(String id) {
        ToastStack.dismiss("inv:" + id);
        backend.request("POST", "/api/v1/invites/" + enc(id) + "/accept", new JsonObject()).thenAcceptAsync(e -> {
            JsonObject o = J.obj(e);
            Invite i = Invite.from(J.obj(o, "invite"));
            if (i != null) state.invites.put(i.id(), i);
            state.changed();
            InviteTarget target = InviteTarget.of(J.obj(o, "connection"), i);
            if (target == null) {
                Notify.warn(module.tr("invite.noConnection"), i == null ? null : i.displayName());
                return;
            }
            if (J.str(o, "packCode") != null) {
                Notify.info(module.tr("invite.needsPack"), J.str(o, "packCode"));
            }
            module.invites().join(target.name(), target.address(), target.port());
        }, MAIN).exceptionally(t -> {
            error(t);
            return null;
        });
    }

    void showInviteToast(Invite i) {
        String body = i.displayName() + (i.message() != null ? " – " + i.message() : "");
        ToastStack.show(new Notify.Toast(module.tr("toast.invite", i.from().name()), body, Notify.Kind.SOCIAL, i.from().id(),
                        () -> module.openSidebar(SidebarRoute.tab(SidebarRoute.Tab.INBOX))),
                List.of(new ToastStack.Button(module.tr("action.join"), true, () -> acceptInvite(i.id())),
                        new ToastStack.Button(module.tr("action.decline"), false, () -> declineInvite(i.id()))),
                20_000, "inv:" + i.id());
    }

    /** Sends a world invite for our relay session (see {@link InviteFlow}). */
    public CompletableFuture<Void> sendInvite(JsonObject body) {
        return act("POST", "/api/v1/invites", body, e -> {
            for (Invite i : J.list(e, Invite::from)) state.invites.put(i.id(), i);
        });
    }

    // ============================================================================================ notifications

    public void markAllNotificationsRead() {
        act("POST", "/api/v1/notifications/read", J.of("all", true), e -> {
            state.notifications.replaceAll(Notification::read);
            state.notificationsUnread = 0;
        });
    }

    public void markNotificationRead(Notification n) {
        if (!n.unread()) return;
        JsonArray ids = new JsonArray();
        ids.add(n.id());
        act("POST", "/api/v1/notifications/read", J.of("ids", ids), e -> {
            state.notifications.replaceAll(x -> x.id().equals(n.id()) ? x.read() : x);
            state.notificationsUnread = Math.max(0, state.notificationsUnread - 1);
        });
    }

    public void deleteNotification(Notification n) {
        act("DELETE", "/api/v1/notifications/" + enc(n.id()), null, e -> {
            state.notifications.removeIf(x -> x.id().equals(n.id()));
            if (n.unread()) state.notificationsUnread = Math.max(0, state.notificationsUnread - 1);
        });
    }

    // ============================================================================================ profile / account

    public CompletableFuture<Void> updateMe(JsonObject body) {
        return act("PATCH", "/api/v1/me", body, e -> {
            Me me = Me.from(J.obj(e));
            if (me != null) state.me = me;
        });
    }

    public CompletableFuture<List<Device>> devices() {
        return backend.request("GET", "/api/v1/auth/sessions", null).thenApplyAsync(e -> J.list(e, Device::from), MAIN);
    }

    public CompletableFuture<Void> revokeDevice(String id) {
        return act("DELETE", "/api/v1/auth/sessions/" + enc(id), null, e -> {
        });
    }

    public CompletableFuture<User> profile(String userId) {
        return backend.request("GET", "/api/v1/users/" + enc(userId), null).thenApplyAsync(e -> {
            User u = User.from(J.obj(e));
            remember(u);
            return u;
        }, MAIN);
    }

    // ============================================================================================ helpers

    /** A REST action: applies the result on the client thread, shows errors as a toast. */
    public CompletableFuture<Void> act(String method, String path, JsonElement body, Consumer<JsonElement> apply) {
        return backend.request(method, path, body).thenAcceptAsync(e -> {
            apply.accept(e);
            state.changed();
        }, MAIN).whenComplete((v, t) -> {
            if (t != null) error(t);
        });
    }

    public void error(Throwable t) {
        String text = Errors.text(t);
        Tasks.onMain(() -> Notify.error(module.tr("error.title"), text));
    }

    private void toast(String title, String body, String icon, Runnable action, String key) {
        if (module.dnd()) return;
        ToastStack.show(new Notify.Toast(title, body, Notify.Kind.SOCIAL, icon, action), List.of(), 0, key);
    }

    /** "Singleplayer · My World" etc. for a presence (translated); null when nothing to say. */
    public String activityText(Presence p) {
        if (p == null || p.activity() == null) {
            return p == null || !p.online() ? null : module.tr("activity.online");
        }
        var a = p.activity();
        return switch (a.type()) {
            case "hosting" -> module.tr("activity.hosting", a.world() == null ? "?" : a.world());
            case "launcher" -> a.instanceName() != null ? module.tr("activity.launcherInstance", a.instanceName()) : module.tr("activity.launcher");
            default -> {
                if (a.server() != null) yield module.tr("activity.server", a.server());
                if (a.world() != null) yield module.tr("activity.world", a.world());
                if (a.instanceName() != null) yield module.tr("activity.instance", a.instanceName());
                yield a.mcVersion() != null ? module.tr("activity.minecraftVersion", a.mcVersion()) : module.tr("activity.minecraft");
            }
        };
    }

    /** Own (translated) title for the notification types we know, the backend's (German) title otherwise. */
    public String notificationTitle(Notification n) {
        String userId = J.str(n.data(), "userId");
        String name = userId == null ? null : state.user(userId) == null ? null : state.user(userId).name();
        switch (n.type()) {
            case "friend_request" -> {
                if (name != null) return module.tr("notif.friend_request", name);
            }
            case "friend_accepted" -> {
                if (name != null) return module.tr("notif.friend_accepted", name);
            }
            case "invite_accepted" -> {
                if (name != null) return module.tr("notif.invite_accepted", name);
            }
            case "invite" -> {
                Invite i = state.invites.get(J.str(n.data(), "inviteId"));
                if (i != null && i.from() != null) return module.tr("notif.invite", i.from().name(), i.targetName());
            }
            default -> {
            }
        }
        return n.title();
    }

    public static String statusLabelKey(String status) {
        return "social.status." + status;
    }

    public static String lang(String key, Object... args) {
        return Lang.tr(key, args);
    }
}
