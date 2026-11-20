package dev.henny.vibeyclient.module.social.streamer;

import dev.henny.vibeyclient.api.privacy.Privacy;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.DirectJoinServerScreen;
import net.minecraft.client.gui.screens.ManageServerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.util.FormattedCharSequence;

public final class AddressMask {
    private static final String DOT = "•";

    private AddressMask() {}

    public static void apply(Screen screen) {
        if (!(screen instanceof DirectJoinServerScreen) && !(screen instanceof ManageServerScreen)) return;
        for (GuiEventListener w : screen.children()) {
            if (w instanceof EditBox box && addressBox(box)) box.addFormatter(AddressMask::mask);
        }
    }

    private static boolean addressBox(EditBox box) {
        return box.getMessage().getContents() instanceof TranslatableContents t && t.getKey().toLowerCase(java.util.Locale.ROOT).contains("ip");
    }

    private static FormattedCharSequence mask(String text, int offset) {
        if (!Privacy.hideServer()) return null;
        return FormattedCharSequence.forward(DOT.repeat(text.length()), Style.EMPTY);
    }
}
