package dev.henny.vibeyclient.module.social.model;

import com.google.gson.JsonObject;
import dev.henny.vibeyclient.backend.J;

import java.time.Instant;
import java.util.List;

/**
 * The backend's social types (vibe-backend {@code packages/shared/src/social.ts}, {@code users.ts},
 * {@code hosting.ts}) as immutable records, parsed leniently (unknown fields ignored, missing ones defaulted).
 */
public final class Model {
    private Model() {}

    static long time(String iso) {
        if (iso == null) return 0;
        try {
            return Instant.parse(iso).toEpochMilli();
        } catch (RuntimeException e) {
            return 0;
        }
    }

    // ============================================================================================ users

    public record User(String id, String name, String skinUrl, boolean slim, String statusText, String badge, long createdAt) {
        public static User from(JsonObject o) {
            if (o == null) return null;
            JsonObject skin = J.obj(o, "skin");
            return new User(J.str(o, "id", "").intern(), J.str(o, "name", "?"), J.str(skin, "url"),
                    "slim".equals(J.str(skin, "model")), J.str(o, "statusText"), J.str(o, "badge", "user"),
                    time(J.str(o, "createdAt")));
        }
    }

    public record Privacy(String friendRequests, String directMessages, String invites, boolean showHosting) {
        public static Privacy from(JsonObject o) {
            return new Privacy(J.str(o, "friendRequests", "everyone"), J.str(o, "directMessages", "friends"),
                    J.str(o, "invites", "friends"), J.bool(o, "showHosting", true));
        }
    }

    public record Me(User user, Privacy privacy, String presenceStatus) {
        public static Me from(JsonObject o) {
            if (o == null) return null;
            return new Me(User.from(o), Privacy.from(J.obj(o, "privacy")), J.str(o, "presenceStatus", "online"));
        }
    }

    /** Relationship of a search result: none, friend, incoming, outgoing, blocked, blocked_by, self. */
    public record SearchResult(User user, String relationship) {
        public static SearchResult from(JsonObject o) {
            return new SearchResult(User.from(o), J.str(o, "relationship", "none"));
        }
    }

    // ============================================================================================ presence

    public record Join(String type, String address, int port, String hostSessionId) {
        public static Join from(JsonObject o) {
            if (o == null) return null;
            return new Join(J.str(o, "type", "direct"), J.str(o, "address"), J.num(o, "port", 25565), J.str(o, "hostSessionId"));
        }
    }

    public record Activity(String type, String instanceName, String mcVersion, String loader, String server, String world,
                           boolean joinable, Join join, long startedAt) {
        public static Activity from(JsonObject o) {
            if (o == null) return null;
            return new Activity(J.str(o, "type", "playing"), J.str(o, "instanceName"), J.str(o, "mcVersion"),
                    J.str(o, "loader"), J.str(o, "server"), J.str(o, "world"), J.bool(o, "joinable", false),
                    Join.from(J.obj(o, "join")), time(J.str(o, "startedAt")));
        }
    }

    /** {@code status}: online, away, busy, offline. */
    public record Presence(String userId, String status, Activity activity, long updatedAt) {
        public static Presence from(JsonObject o) {
            if (o == null) return null;
            return new Presence(J.str(o, "userId", "").intern(), J.str(o, "status", "offline"), Activity.from(J.obj(o, "activity")),
                    time(J.str(o, "updatedAt")));
        }

        public boolean online() {
            return !"offline".equals(status);
        }

        public static Presence offline(String userId) {
            return new Presence(userId, "offline", null, 0);
        }
    }

    // ============================================================================================ friends

    public record Friend(User user, long since, Presence presence) {
        public static Friend from(JsonObject o) {
            User u = User.from(J.obj(o, "user"));
            if (u == null) return null;
            Presence p = Presence.from(J.obj(o, "presence"));
            return new Friend(u, time(J.str(o, "since")), p == null ? Presence.offline(u.id()) : p);
        }

        public Friend withPresence(Presence p) {
            return new Friend(user, since, p);
        }

        public Friend withUser(User u) {
            return new Friend(u, since, presence);
        }
    }

    public record FriendRequest(String id, User from, User to, long createdAt, boolean incoming) {
        public static FriendRequest from(JsonObject o) {
            return new FriendRequest(J.str(o, "id", "").intern(), User.from(J.obj(o, "from")), User.from(J.obj(o, "to")),
                    time(J.str(o, "createdAt")), "incoming".equals(J.str(o, "direction")));
        }

        /** The other person. */
        public User other() {
            return incoming ? from : to;
        }
    }

    // ============================================================================================ chats

    public record ChatMember(User user, String role, String lastReadMessageId) {
        public static ChatMember from(JsonObject o) {
            User u = User.from(J.obj(o, "user"));
            if (u == null) return null;
            return new ChatMember(u, J.str(o, "role", "member"), J.str(o, "lastReadMessageId"));
        }

        public ChatMember withRead(String messageId) {
            return new ChatMember(user, role, messageId);
        }
    }

    public record InviteCard(String inviteId, String kind, String title, String subtitle, String packCode, long expiresAt) {
        public static InviteCard from(JsonObject o) {
            if (o == null) return null;
            return new InviteCard(J.str(o, "inviteId", "").intern(), J.str(o, "kind", "world"), J.str(o, "title", ""),
                    J.str(o, "subtitle"), J.str(o, "packCode"), time(J.str(o, "expiresAt")));
        }
    }

    public record SystemPayload(String event, String actorId, String targetId, String name) {
        public static SystemPayload from(JsonObject o) {
            if (o == null) return null;
            return new SystemPayload(J.str(o, "event", ""), J.str(o, "actorId"), J.str(o, "targetId"), J.str(o, "name"));
        }
    }

    /** {@code kind}: text, system, invite. */
    public record Message(String id, String chatId, String authorId, String kind, String content, SystemPayload system,
                          InviteCard invite, String nonce, long createdAt, long editedAt, boolean deleted) {
        public static Message from(JsonObject o) {
            if (o == null) return null;
            return new Message(J.str(o, "id", "").intern(), J.str(o, "chatId", "").intern(), J.str(o, "authorId"), J.str(o, "kind", "text"),
                    J.str(o, "content", ""), SystemPayload.from(J.obj(o, "system")), InviteCard.from(J.obj(o, "invite")),
                    J.str(o, "nonce"), time(J.str(o, "createdAt")), time(J.str(o, "editedAt")), J.bool(o, "deleted", false));
        }

        /** Local echo of a message being sent. */
        public static Message pending(String chatId, String authorId, String content, String nonce) {
            return new Message("pending:" + nonce, chatId, authorId, "text", content, null, null, nonce,
                    System.currentTimeMillis(), 0, false);
        }

        public boolean isPending() {
            return id.startsWith("pending:");
        }
    }

    /** {@code type}: dm, group. */
    public record Chat(String id, String type, String name, String ownerId, List<ChatMember> members, Message lastMessage,
                       int unreadCount, long createdAt, long updatedAt) {
        public static Chat from(JsonObject o) {
            if (o == null) return null;
            return new Chat(J.str(o, "id", "").intern(), J.str(o, "type", "dm"), J.str(o, "name"), J.str(o, "ownerId"),
                    J.list(o.get("members"), ChatMember::from), Message.from(J.obj(o, "lastMessage")),
                    J.num(o, "unreadCount", 0), time(J.str(o, "createdAt")), time(J.str(o, "updatedAt")));
        }

        public boolean dm() {
            return "dm".equals(type);
        }

        public long activity() {
            return lastMessage != null ? Math.max(lastMessage.createdAt(), updatedAt) : updatedAt;
        }

        public Chat withUnread(int n) {
            return new Chat(id, type, name, ownerId, members, lastMessage, n, createdAt, updatedAt);
        }

        public Chat withLast(Message m) {
            return new Chat(id, type, name, ownerId, members, m, unreadCount, createdAt, Math.max(updatedAt, m.createdAt()));
        }

        public Chat withMembers(List<ChatMember> m) {
            return new Chat(id, type, name, ownerId, m, lastMessage, unreadCount, createdAt, updatedAt);
        }

        public ChatMember member(String userId) {
            for (ChatMember m : members) if (m.user().id().equals(userId)) return m;
            return null;
        }

        /** The other member of a DM (or null). */
        public User other(String selfId) {
            for (ChatMember m : members) if (!m.user().id().equals(selfId)) return m.user();
            return null;
        }
    }

    // ============================================================================================ invites

    public record Invite(String id, String kind, User from, String toUserId, String chatId, String targetName,
                         String mcVersion, String loader, String packCode, String message, String hostSessionId,
                         String status, String myResponse, long createdAt, long expiresAt) {
        public static Invite from(JsonObject o) {
            if (o == null) return null;
            JsonObject t = J.obj(o, "target");
            return new Invite(J.str(o, "id", "").intern(), J.str(o, "kind", "world"), User.from(J.obj(o, "from")), J.str(o, "toUserId"),
                    J.str(o, "chatId"), J.str(t, "name", ""), J.str(t, "mcVersion"), J.str(t, "loader"), J.str(t, "packCode"),
                    J.str(o, "message"), J.str(o, "hostSessionId"), J.str(o, "status", "pending"), J.str(o, "myResponse"),
                    time(J.str(o, "createdAt")), time(J.str(o, "expiresAt")));
        }

        public boolean open() {
            return "pending".equals(status) && myResponse == null && expiresAt > System.currentTimeMillis();
        }
    }

    // ============================================================================================ notifications

    public record Notification(String id, String type, String title, String body, JsonObject data, long createdAt, long readAt) {
        public static Notification from(JsonObject o) {
            if (o == null) return null;
            return new Notification(J.str(o, "id", "").intern(), J.str(o, "type", "system"), J.str(o, "title", ""), J.str(o, "body"),
                    J.obj(o, "data"), time(J.str(o, "createdAt")), time(J.str(o, "readAt")));
        }

        public boolean unread() {
            return readAt == 0;
        }

        public Notification read() {
            return new Notification(id, type, title, body, data, createdAt, readAt == 0 ? System.currentTimeMillis() : readAt);
        }
    }

    // ============================================================================================ hosting / devices

    public record HostSession(String id, User host, String kind, String address, String mcVersion, String status,
                              int players, long startedAt) {
        public static HostSession from(JsonObject o) {
            if (o == null) return null;
            var p = J.arr(o, "players");
            return new HostSession(J.str(o, "id", "").intern(), User.from(J.obj(o, "host")), J.str(o, "kind", "world"),
                    J.str(o, "address"), J.str(o, "mcVersion"), J.str(o, "status", "online"), p == null ? 0 : p.size(),
                    time(J.str(o, "startedAt")));
        }

        public boolean online() {
            return "online".equals(status) && address != null;
        }
    }

    public record Device(String id, String clientName, String clientVersion, String platform, String ip, long lastUsedAt,
                         boolean current) {
        public static Device from(JsonObject o) {
            JsonObject c = J.obj(o, "client");
            return new Device(J.str(o, "id", "").intern(), J.str(c, "name", "?"), J.str(c, "version", ""), J.str(c, "platform", ""),
                    J.str(o, "ip"), time(J.str(o, "lastUsedAt")), J.bool(o, "current", false));
        }
    }
}
