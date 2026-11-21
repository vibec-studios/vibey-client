package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.access.AccessInfo;
import dev.henny.vibeyclient.access.AccessPhase;
import dev.henny.vibeyclient.access.AccessStatus;
import dev.henny.vibeyclient.access.AccessView;
import dev.henny.vibeyclient.api.Lang;

public final class AccessText {
    private AccessText() {}

    public static String line(AccessView v) {
        return Lang.tr("core.access.line", body(v));
    }

    public static String detail(AccessView v) {
        AccessInfo i = v.info();
        if (v.phase() == AccessPhase.DENIED && i != null && i.status() == AccessStatus.REJECTED && i.reason() != null) {
            return Lang.tr("core.access.reason", i.reason());
        }
        return Lang.tr("core.access.tip");
    }

    public static boolean canRetry(AccessView v) {
        return v.phase() != AccessPhase.CHECKING && v.phase() != AccessPhase.OPEN;
    }

    static String body(AccessView v) {
        return switch (v.phase()) {
            case CHECKING, OPEN -> Lang.tr("core.access.checking");
            case NO_LOGIN -> Lang.tr("core.access.noLogin");
            case UNREACHABLE -> Lang.tr("core.access.unreachable");
            case ONLINE_OFF -> Lang.tr("core.access.onlineOff");
            case DENIED -> status(v.info());
        };
    }

    private static String status(AccessInfo i) {
        AccessStatus s = i == null ? AccessStatus.NONE : i.status();
        return switch (s) {
            case WAITLIST -> i.position() != null ? Lang.tr("core.access.waitlistAt", i.position()) : Lang.tr("core.access.waitlist");
            case REJECTED -> Lang.tr("core.access.rejected");
            case NONE, APPROVED -> Lang.tr("core.access.none");
        };
    }
}
