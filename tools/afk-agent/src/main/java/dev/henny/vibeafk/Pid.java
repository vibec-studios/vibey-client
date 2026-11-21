package dev.henny.vibeafk;

import java.lang.management.ManagementFactory;

final class Pid {
    private Pid() {}

    static Long current() {
        try {
            Class<?> ph = Class.forName("java.lang.ProcessHandle");
            Object cur = ph.getMethod("current").invoke(null);
            return (Long) ph.getMethod("pid").invoke(cur);
        } catch (Throwable ignored) {
        }
        try {
            String n = ManagementFactory.getRuntimeMXBean().getName();
            int at = n.indexOf('@');
            return Long.valueOf(at > 0 ? n.substring(0, at) : n);
        } catch (Throwable t) {
            return null;
        }
    }
}
