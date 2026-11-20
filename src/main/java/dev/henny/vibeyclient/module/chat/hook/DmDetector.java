package dev.henny.vibeyclient.module.chat.hook;

import dev.henny.vibeyclient.module.chat.core.DirectMessage;
import dev.henny.vibeyclient.module.chat.core.DmPatterns;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

public final class DmDetector {
    public static final String VANILLA_KEY = "commands.message.display.incoming";

    private DmDetector() {}

    public static DirectMessage detect(Component message, String plain, String ownName) {
        if (message.getContents() instanceof TranslatableContents t && VANILLA_KEY.equals(t.getKey())) {
            Object[] args = t.getArgs();
            if (args.length >= 2) return new DirectMessage(text(args[0]), text(args[1]));
        }
        return DmPatterns.match(plain, ownName);
    }

    private static String text(Object arg) {
        return arg instanceof Component c ? c.getString() : String.valueOf(arg);
    }
}
