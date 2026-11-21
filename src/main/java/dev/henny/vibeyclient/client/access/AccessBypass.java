package dev.henny.vibeyclient.client.access;

import dev.henny.vibeyclient.Vibey;

final class AccessBypass {
    private AccessBypass() {}

    private static Boolean active;

    static boolean active() {
        Boolean a = active;
        if (a == null) active = a = compute();
        return a;
    }

    private static boolean compute() {
        if (!Vibey.platform().isDevelopment()) return false;
        boolean on = "on".equalsIgnoreCase(System.getProperty("vibeyclient.accessGate", ""));
        if (!on) Vibey.LOG.info("Vibe access gate off in development (-Dvibeyclient.accessGate=on to test it)");
        return !on;
    }
}
