package dev.henny.vibeyclient.ui.vui.menu;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.ui.title.BackendStatus;

public final class Connection {
    private Connection() {}

    public static String status() {
        return switch (BackendStatus.state()) {
            case ONLINE, REACHABLE -> "online";
            case CHECKING, CONNECTING -> "connecting";
            default -> "offline";
        };
    }

    public static String label() {
        return Lang.tr(switch (BackendStatus.state()) {
            case ONLINE, REACHABLE -> "ui.status.online";
            case CHECKING, CONNECTING -> "ui.status.connecting";
            case OFF -> "ui.status.off";
            case SIGNED_OUT, NO_ACCOUNT -> "ui.status.signedOut";
            case LOGIN_ERROR -> "ui.status.error";
            case OFFLINE -> "ui.status.offline";
        });
    }

    public static String tip() {
        return Lang.tr(switch (BackendStatus.state()) {
            case ONLINE, REACHABLE, CHECKING, CONNECTING, SIGNED_OUT, NO_ACCOUNT -> "ui.status.tipOn";
            case OFF -> "ui.status.tipOff";
            case OFFLINE, LOGIN_ERROR -> "ui.status.tipDown";
        });
    }
}
