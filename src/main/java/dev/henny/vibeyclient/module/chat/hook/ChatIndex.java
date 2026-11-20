package dev.henny.vibeyclient.module.chat.hook;

import dev.henny.vibeyclient.module.chat.core.ChatEntry;
import net.minecraft.client.GuiMessage;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ChatIndex {
    private final Map<GuiMessage, ChatEntry> entries = new IdentityHashMap<>();
    private final Map<GuiMessage.Line, GuiMessage> lines = new IdentityHashMap<>();

    public void bind(GuiMessage message, ChatEntry entry) {
        entries.put(message, entry);
    }

    public ChatEntry entry(GuiMessage message) {
        return message == null ? null : entries.get(message);
    }

    public void line(GuiMessage.Line line, GuiMessage owner) {
        if (owner != null) lines.put(line, owner);
    }

    public GuiMessage owner(GuiMessage.Line line) {
        return lines.get(line);
    }

    public void forget(GuiMessage message) {
        entries.remove(message);
        lines.values().removeIf(m -> m == message);
    }

    public void prune(List<GuiMessage> all, List<GuiMessage.Line> shown) {
        if (entries.size() > all.size() + 32) retain(entries.keySet(), all);
        if (lines.size() > shown.size() + 64) retain(lines.keySet(), shown);
    }

    private static <T> void retain(Set<T> keys, List<T> live) {
        Set<T> keep = Collections.newSetFromMap(new IdentityHashMap<>());
        keep.addAll(live);
        keys.retainAll(keep);
    }

    public void clear() {
        entries.clear();
        lines.clear();
    }

    public int size() {
        return entries.size();
    }
}
