package dev.henny.vibeyclient.module.chat.core;

public record ChatEntry(String plain, String key, long at, int count, boolean mention, String dmSender) {
    public static ChatEntry of(String plain, long at, boolean mention, String dmSender) {
        return new ChatEntry(plain, SpamCollapse.normalize(plain), at, 1, mention, dmSender);
    }

    public ChatEntry repeated(long now) {
        return new ChatEntry(plain, key, now, count + 1, mention, dmSender);
    }
}
