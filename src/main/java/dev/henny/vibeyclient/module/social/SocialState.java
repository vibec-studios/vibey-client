package dev.henny.vibeyclient.module.social;

import dev.henny.vibeyclient.module.social.model.Model.Chat;
import dev.henny.vibeyclient.module.social.model.Model.ChatMember;
import dev.henny.vibeyclient.module.social.model.Model.Friend;
import dev.henny.vibeyclient.module.social.model.Model.FriendRequest;
import dev.henny.vibeyclient.module.social.model.Model.HostSession;
import dev.henny.vibeyclient.module.social.model.Model.Invite;
import dev.henny.vibeyclient.module.social.model.Model.Me;
import dev.henny.vibeyclient.module.social.model.Model.Message;
import dev.henny.vibeyclient.module.social.model.Model.Notification;
import dev.henny.vibeyclient.module.social.model.Model.Presence;
import dev.henny.vibeyclient.module.social.model.Model.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Everything social the client knows, filled from REST and kept current by gateway events. Only touched on the
 * client thread. {@link #revision()} changes on every mutation so views can cache derived lists.
 */
public final class SocialState {
    /** One chat's loaded messages (oldest first). */
    public static final class History {
        public final List<Message> messages = new ArrayList<>();
        public boolean hasMore = true;
        public boolean loading;
        public boolean loaded;
    }

    public Me me;
    public final Map<String, Friend> friends = new LinkedHashMap<>();
    public final Map<String, FriendRequest> requests = new LinkedHashMap<>();
    public final Map<String, User> blocked = new LinkedHashMap<>();
    public final Map<String, Chat> chats = new LinkedHashMap<>();
    public final Map<String, History> histories = new HashMap<>();
    /** chatId → userId → typing until (epoch ms). */
    public final Map<String, Map<String, Long>> typing = new HashMap<>();
    public final Map<String, Invite> invites = new LinkedHashMap<>();
    public final List<Notification> notifications = new ArrayList<>();
    public int notificationsUnread;
    public boolean notificationsHasMore;
    /** Joinable sessions of friends (id → session). */
    public final Map<String, HostSession> friendHosts = new LinkedHashMap<>();
    /** Our own running relay session, or null. */
    public HostSession myHost;
    public final Map<String, Boolean> features = new HashMap<>();
    public String motd;
    public int chatMaxLength = 2000;
    public int maxGroupMembers = 50;
    public boolean loaded;

    private int revision;
    private int sortedRev = -1;
    private final List<Friend> sorted = new ArrayList<>();
    private int chatsRev = -1;
    private final List<Chat> sortedChats = new ArrayList<>();

    public int revision() {
        return revision;
    }

    public void changed() {
        revision++;
    }

    public void clear() {
        me = null;
        friends.clear();
        requests.clear();
        blocked.clear();
        chats.clear();
        histories.clear();
        typing.clear();
        invites.clear();
        notifications.clear();
        notificationsUnread = 0;
        friendHosts.clear();
        myHost = null;
        features.clear();
        motd = null;
        loaded = false;
        changed();
    }

    public String selfId() {
        return me == null ? null : me.user().id();
    }

    public boolean feature(String name) {
        return features.getOrDefault(name, true);
    }

    // ============================================================================================ derived

    /** Online first (online, busy, away), then by name. Cached per revision. */
    public List<Friend> sortedFriends() {
        if (sortedRev != revision) {
            sortedRev = revision;
            sorted.clear();
            sorted.addAll(friends.values());
            sorted.sort(Comparator.comparingInt((Friend f) -> statusRank(f.presence().status()))
                    .thenComparing(f -> f.user().name().toLowerCase(Locale.ROOT)));
        }
        return sorted;
    }

    public static int statusRank(String status) {
        return switch (status) {
            case "online" -> 0;
            case "busy" -> 1;
            case "away" -> 2;
            default -> 3;
        };
    }

    /** Newest activity first. */
    public List<Chat> sortedChats() {
        if (chatsRev != revision) {
            chatsRev = revision;
            sortedChats.clear();
            sortedChats.addAll(chats.values());
            sortedChats.sort(Comparator.comparingLong(Chat::activity).reversed());
        }
        return sortedChats;
    }

    public int onlineCount() {
        int n = 0;
        for (Friend f : friends.values()) if (f.presence().online()) n++;
        return n;
    }

    public int unreadChats() {
        int n = 0;
        for (Chat c : chats.values()) n += c.unreadCount();
        return n;
    }

    public int incomingRequests() {
        int n = 0;
        for (FriendRequest r : requests.values()) if (r.incoming()) n++;
        return n;
    }

    public List<Invite> openIncomingInvites() {
        List<Invite> out = new ArrayList<>();
        String self = selfId();
        for (Invite i : invites.values()) {
            if (i.open() && i.from() != null && !i.from().id().equals(self)) out.add(i);
        }
        out.sort(Comparator.comparingLong(Invite::createdAt).reversed());
        return out;
    }

    /** The DM chat with {@code userId}, or null. */
    public Chat dmWith(String userId) {
        for (Chat c : chats.values()) {
            if (c.dm() && c.member(userId) != null) return c;
        }
        return null;
    }

    public User user(String id) {
        if (id == null) return null;
        if (me != null && id.equals(me.user().id())) return me.user();
        Friend f = friends.get(id);
        if (f != null) return f.user();
        for (Chat c : chats.values()) {
            ChatMember m = c.member(id);
            if (m != null) return m.user();
        }
        for (FriendRequest r : requests.values()) {
            if (r.other() != null && r.other().id().equals(id)) return r.other();
        }
        return blocked.get(id);
    }

    public String nameOf(String id) {
        User u = user(id);
        return u == null ? "?" : u.name();
    }

    public Presence presenceOf(String userId) {
        Friend f = friends.get(userId);
        return f == null ? null : f.presence();
    }

    /** Users typing in {@code chatId} right now (not us). */
    public List<String> typingIn(String chatId) {
        Map<String, Long> m = typing.get(chatId);
        if (m == null || m.isEmpty()) return List.of();
        long now = System.currentTimeMillis();
        List<String> out = new ArrayList<>();
        for (Map.Entry<String, Long> e : m.entrySet()) {
            if (e.getValue() > now && !e.getKey().equals(selfId())) out.add(e.getKey());
        }
        return out;
    }

    public History history(String chatId) {
        return histories.computeIfAbsent(chatId, k -> new History());
    }

    // ============================================================================================ mutations

    /**
     * A new or edited message. Replaces our local echo with the same nonce, keeps the chat's last message and unread
     * count current. {@code visible}: the chat is open on screen (no unread increment).
     */
    public void applyMessage(Message m, boolean created, boolean visible) {
        History h = histories.get(m.chatId());
        if (h != null) {
            int idx = -1;
            for (int i = h.messages.size() - 1; i >= 0; i--) {
                Message x = h.messages.get(i);
                if (x.id().equals(m.id()) || (m.nonce() != null && m.nonce().equals(x.nonce()) && x.isPending())) {
                    idx = i;
                    break;
                }
            }
            if (idx >= 0) h.messages.set(idx, m);
            else if (created) insertSorted(h.messages, m);
        }
        Chat c = chats.get(m.chatId());
        if (c != null) {
            if (created) {
                c = c.withLast(m);
                boolean mine = m.authorId() != null && m.authorId().equals(selfId());
                if (!mine && !visible && !"system".equals(m.kind())) {
                    c = c.withUnread(c.unreadCount() + 1);
                }
            } else if (c.lastMessage() != null && c.lastMessage().id().equals(m.id())) {
                c = c.withLast(m);
            }
            chats.put(c.id(), c);
        }
        // a message ends the author's typing indicator
        Map<String, Long> t = typing.get(m.chatId());
        if (t != null && m.authorId() != null) t.remove(m.authorId());
        changed();
    }

    private static void insertSorted(List<Message> list, Message m) {
        int i = list.size();
        while (i > 0 && list.get(i - 1).createdAt() > m.createdAt()) i--;
        list.add(i, m);
    }

    public void applyMessageDeleted(String chatId, String messageId) {
        History h = histories.get(chatId);
        if (h != null) {
            for (int i = 0; i < h.messages.size(); i++) {
                Message x = h.messages.get(i);
                if (x.id().equals(messageId)) {
                    h.messages.set(i, new Message(x.id(), x.chatId(), x.authorId(), x.kind(), "", x.system(), x.invite(),
                            x.nonce(), x.createdAt(), x.editedAt(), true));
                }
            }
        }
        changed();
    }

    public void applyChatRead(String chatId, String userId, String messageId) {
        Chat c = chats.get(chatId);
        if (c == null) return;
        List<ChatMember> members = new ArrayList<>(c.members().size());
        for (ChatMember m : c.members()) members.add(m.user().id().equals(userId) ? m.withRead(messageId) : m);
        c = c.withMembers(members);
        if (userId.equals(selfId())) c = c.withUnread(0);
        chats.put(chatId, c);
        changed();
    }

    /** Older page from the REST history (newest first in the response). */
    public void applyHistoryPage(String chatId, List<Message> newestFirst, boolean hasMore) {
        History h = history(chatId);
        for (Message m : newestFirst) {
            boolean dup = false;
            for (Message x : h.messages) {
                if (x.id().equals(m.id())) {
                    dup = true;
                    break;
                }
            }
            if (!dup) insertSorted(h.messages, m);
        }
        h.hasMore = hasMore;
        h.loading = false;
        h.loaded = true;
        changed();
    }

    public void applyPresence(Presence p) {
        Friend f = friends.get(p.userId());
        if (f != null) {
            friends.put(p.userId(), f.withPresence(p));
            changed();
        }
    }

    public void applyUser(User u) {
        Friend f = friends.get(u.id());
        if (f != null) friends.put(u.id(), f.withUser(u));
        changed();
    }

    /** Has someone read our message {@code messageId} (or later)? Uses message order of the loaded history. */
    public List<String> readers(Chat c, Message m) {
        History h = histories.get(c.id());
        List<String> out = new ArrayList<>();
        if (h == null) return out;
        int idx = h.messages.indexOf(m);
        if (idx < 0) return out;
        for (ChatMember member : c.members()) {
            if (member.user().id().equals(selfId()) || member.lastReadMessageId() == null) continue;
            int readIdx = -1;
            for (int i = h.messages.size() - 1; i >= 0; i--) {
                if (h.messages.get(i).id().equals(member.lastReadMessageId())) {
                    readIdx = i;
                    break;
                }
            }
            if (readIdx >= idx) out.add(member.user().id());
        }
        return out;
    }
}
