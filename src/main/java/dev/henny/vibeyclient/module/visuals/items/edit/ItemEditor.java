package dev.henny.vibeyclient.module.visuals.items.edit;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.visuals.VisualsModule;
import dev.henny.vibeyclient.module.visuals.items.ItemRules;
import dev.henny.vibeyclient.module.visuals.items.core.EditSession;
import dev.henny.vibeyclient.ui.vui.VibeyVui;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

public final class ItemEditor {
    private static volatile EditSession session;

    private ItemEditor() {}

    public static EditSession session() {
        return session;
    }

    public static void open() {
        VisualsModule m = VisualsModule.get();
        Minecraft mc = Minecraft.getInstance();
        if (m == null || !m.isEnabled()) return;
        if (mc.player == null || mc.level == null) {
            Notify.warn(Lang.tr("visuals.items.edit.title"), Lang.tr("visuals.items.edit.noWorld"));
            return;
        }
        if (ItemRules.heldBlocked()) {
            Notify.warn(Lang.tr("visuals.items.edit.title"), Lang.tr("visuals.items.edit.blocked"));
            return;
        }
        if (!mc.options.getCameraType().isFirstPerson()) mc.options.setCameraType(CameraType.FIRST_PERSON);
        EditSession s = new EditSession(m.held.main.pose(), m.held.off.pose());
        session = s;
        VibeyVui.init();
        ClientCompat.openScreen(new ItemEditScreen(mc.screen, s));
    }

    static void finish(EditSession s, boolean save) {
        VisualsModule m = VisualsModule.get();
        if (save && m != null) m.held.save(s.pose(EditSession.MAIN), s.pose(EditSession.OFF));
        if (session == s) session = null;
    }
}
