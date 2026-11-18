package dev.henny.vibeyclient.module.social;

/** What the social sidebar shows: a tab of the home view or one detail page. */
public record SidebarRoute(Kind kind, Tab tab, String id) {
    public enum Kind { TAB, CHAT, PROFILE, NEW_GROUP, GROUP_SETTINGS, INVITE }

    public enum Tab { FRIENDS, CHATS, REQUESTS, INBOX }

    public static SidebarRoute tab(Tab t) {
        return new SidebarRoute(Kind.TAB, t, null);
    }

    public static SidebarRoute chat(String chatId) {
        return new SidebarRoute(Kind.CHAT, Tab.CHATS, chatId);
    }

    public static SidebarRoute profile(String userId) {
        return new SidebarRoute(Kind.PROFILE, Tab.FRIENDS, userId);
    }

    public static SidebarRoute newGroup() {
        return new SidebarRoute(Kind.NEW_GROUP, Tab.CHATS, null);
    }

    public static SidebarRoute groupSettings(String chatId) {
        return new SidebarRoute(Kind.GROUP_SETTINGS, Tab.CHATS, chatId);
    }

    /** Pick friends to invite into our world; {@code preselect} = a user id or null. */
    public static SidebarRoute invite(String preselect) {
        return new SidebarRoute(Kind.INVITE, Tab.FRIENDS, preselect);
    }

    public boolean isTab() {
        return kind == Kind.TAB;
    }
}
