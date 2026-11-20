package dev.henny.vibeyclient.module.chat.line;

import dev.henny.vibeyclient.api.privacy.Privacy;
import dev.henny.vibeyclient.module.chat.ChatModule;
import dev.henny.vibeyclient.module.chat.core.MentionMatcher;
import dev.henny.vibeyclient.module.chat.core.Words;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class Mentions {
    private static String cachedKey = "";
    private static MentionMatcher cached = new MentionMatcher("", List.of());

    private Mentions() {}

    public static String ownName() {
        return Minecraft.getInstance().getUser().getName();
    }

    public static MentionMatcher matcher(ChatModule m) {
        return matcher(ownName(), m.mentionWords.get(), null);
    }

    public static MentionMatcher highlighter(ChatModule m) {
        String alias = Privacy.hideNames() ? Privacy.aliases().alias(ownName()) : null;
        return matcher(ownName(), m.mentionWords.get(), alias);
    }

    private static synchronized MentionMatcher matcher(String own, String words, String alias) {
        String key = own + "\n" + words + "\n" + alias;
        if (key.equals(cachedKey)) return cached;
        List<String> extra = new ArrayList<>(Words.split(words));
        if (alias != null) extra.add(alias);
        cachedKey = key;
        cached = new MentionMatcher(own, extra);
        return cached;
    }
}
