package dev.henny.vibeyclient.module.devlink;

import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.ui.vui.dialog.Confirm;
import net.minecraft.client.Minecraft;

final class DevLinkToken {
    private DevLinkToken() {}

    static void copy(DevLinkModule m) {
        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(m.token());
            Notify.success(m.tr("status.token"), m.tr("status.copied"));
        } catch (RuntimeException e) {
            Notify.warn(m.tr("status.token"), m.tr("status.noClipboard"));
        }
    }

    static void renew(DevLinkModule m) {
        Confirm.ask("key", m.tr("status.renewAsk"), m.tr("status.renewText"), m.tr("status.renew"), true, () -> {
            m.regenerateToken();
            Notify.success(m.tr("status.token"), m.tr("status.renewed"));
        });
    }
}
