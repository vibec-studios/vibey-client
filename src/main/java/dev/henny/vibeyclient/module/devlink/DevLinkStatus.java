package dev.henny.vibeyclient.module.devlink;

import dev.henny.vibeyclient.api.StatusCard;
import dev.henny.vibeyclient.api.StatusCard.Kind;

final class DevLinkStatus {
    private DevLinkStatus() {}

    static StatusCard of(DevLinkModule m) {
        return switch (m.state()) {
            case LISTENING -> new StatusCard(Kind.OK, m.tr("status.on"), "127.0.0.1:" + m.listeningPort(), clients(m));
            case PORT_IN_USE -> new StatusCard(Kind.ERROR, m.tr("status.error"), m.tr("status.portInUse", m.listeningPort()), "");
            case FAILED -> new StatusCard(Kind.ERROR, m.tr("status.error"), m.failure(), "");
            default -> new StatusCard(Kind.OFF, m.tr("status.off"), m.tr(m.isEnabled() ? "status.creatorOff" : "status.moduleOff"), "");
        };
    }

    private static String clients(DevLinkModule m) {
        int n = m.clients();
        return n == 0 ? m.tr("status.noClient") : m.tr("status.clients", n);
    }
}
