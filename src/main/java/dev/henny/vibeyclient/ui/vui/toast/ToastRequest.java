package dev.henny.vibeyclient.ui.vui.toast;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.ui.vui.HeadTextures;
import dev.henny.vui.host.Toast;

record ToastRequest(Notify.Toast toast, String actionLabel, Runnable actionRun, int ms, String key) {
    static final int DEFAULT_MS = 5_000;
    static final int ACTION_MS = 12_000;

    static ToastRequest of(Notify.Toast t) {
        boolean act = t.action() != null;
        return new ToastRequest(t, act ? Lang.tr("ui.toast.open") : "", t.action(), act ? 7_000 : DEFAULT_MS, null);
    }

    Toast build() {
        Toast out = new Toast(toast.title(), toast.body(), icon(), kind(), ms / 1000f);
        if (actionRun != null) out.action(actionLabel, key == null ? "" : key, actionRun);
        return out;
    }

    private String kind() {
        return switch (toast.kind()) {
            case SUCCESS -> "ok";
            case WARN -> "warn";
            case ERROR -> "error";
            case SOCIAL -> actionRun != null ? "invite" : "info";
            default -> "info";
        };
    }

    private String icon() {
        String k = toast.iconKey();
        if (k != null && k.length() >= 32 && k.contains("-")) {
            String head = HeadTextures.of(k, null);
            if (!head.isEmpty()) return head;
        }
        if (k != null && k.contains(":")) return k;
        return "vibeyclient:" + switch (toast.kind()) {
            case SUCCESS -> "check";
            case WARN, ERROR -> "warn";
            case SOCIAL -> "friends";
            default -> "info";
        };
    }
}
