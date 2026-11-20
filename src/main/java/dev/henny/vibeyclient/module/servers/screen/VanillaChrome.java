package dev.henny.vibeyclient.module.servers.screen;

import dev.henny.vibeyclient.module.servers.mixin.JoinMultiplayerAccess;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;

final class VanillaChrome {
    private final JoinMultiplayerScreen screen;
    private Boolean friendsShown;

    VanillaChrome(JoinMultiplayerScreen screen) {
        this.screen = screen;
    }

    void apply(ServerSelectionList list, boolean tabs, boolean friends) {
        title(!tabs);
        list.visible = !friends;
        JoinMultiplayerAccess a = (JoinMultiplayerAccess) screen;
        if (friends) {
            for (Button b : new Button[] {a.vibey$select(), a.vibey$edit(), a.vibey$delete()}) if (b != null) b.active = false;
        } else if (Boolean.TRUE.equals(friendsShown)) {
            a.vibey$selectedChanged();
        }
        friendsShown = friends;
    }

    void restore(ServerSelectionList list) {
        if (list != null) list.visible = true;
        title(true);
    }

    private void title(boolean visible) {
        for (GuiEventListener l : screen.children()) {
            if (l instanceof StringWidget w && w.getMessage().equals(screen.getTitle())) w.visible = visible;
        }
    }
}
