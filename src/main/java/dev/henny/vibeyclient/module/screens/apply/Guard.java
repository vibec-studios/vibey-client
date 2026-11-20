package dev.henny.vibeyclient.module.screens.apply;

import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.module.screens.core.ScreenNames;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class Guard {
    private static final Set<String> BROKEN = ConcurrentHashMap.newKeySet();

    private Guard() {}

    public static void run(ScreenSession s, Runnable r) {
        if (s == null || s.broken) return;
        try {
            r.run();
        } catch (Throwable t) {
            fail(s, t);
        }
    }

    public static void fail(ScreenSession s, Throwable t) {
        if (s.broken) return;
        s.broken = true;
        Vibey.LOG.error("Screen layout failed for {}, skipped", s.name, t);
        try {
            LayoutApplier.restore(s);
        } catch (Throwable again) {
            Vibey.LOG.debug("Restore after failure failed too", again);
        }
        if (BROKEN.add(s.name)) {
            Notify.warn(Lang.tr("screens.toast.failed"), ScreenNames.readable(s.name));
        }
    }

    public static boolean broken(String name) {
        return BROKEN.contains(name);
    }

    public static void clear() {
        BROKEN.clear();
    }
}
