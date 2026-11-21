package dev.henny.vibeyclient.dev;

import dev.henny.vibeyclient.access.AccessInfo;
import dev.henny.vibeyclient.access.AccessView;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.client.access.Access;

import java.util.concurrent.atomic.AtomicInteger;

public final class DevAccessCommands {
    private DevAccessCommands() {}

    public static void register() {
        DevDriver.EXTRA.put("access", arg -> {
            if (arg.equals("recheck")) Access.recheck();
            return "ok " + describe();
        });
    }

    private static String describe() {
        AccessView v = Access.view();
        AccessInfo i = v.info();
        AtomicInteger enabled = new AtomicInteger();
        ModuleManager mm = ModuleManager.get();
        mm.forEachEnabled(m -> enabled.incrementAndGet());
        return "phase=" + v.phase() + " checking=" + v.checking()
                + " status=" + (i == null ? "-" : i.status()) + " position=" + (i == null ? "-" : i.position())
                + " total=" + (i == null ? "-" : i.total()) + " reason=" + (i == null ? "-" : i.reason())
                + " started=" + mm.isStarted() + " modules=" + enabled.get();
    }
}
