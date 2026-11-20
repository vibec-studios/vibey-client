package dev.henny.vibeyclient.module.chat.line;

import dev.henny.vibeyclient.api.Lang;
import net.minecraft.client.GuiMessageTag;

public final class MentionTag {
    public static final int COLOR = 0xFFC857;
    public static final GuiMessageTag TAG = new GuiMessageTag(COLOR, null, Lang.text("chat.tag.mention"), "Mention");

    private MentionTag() {}

    public static boolean replaces(GuiMessageTag tag) {
        return tag == null || tag.equals(GuiMessageTag.system()) || tag.equals(GuiMessageTag.systemSinglePlayer());
    }
}
