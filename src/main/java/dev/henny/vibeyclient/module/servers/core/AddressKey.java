package dev.henny.vibeyclient.module.servers.core;

import java.util.Locale;

public final class AddressKey {
    private static final String DEFAULT_PORT = ":25565";

    private AddressKey() {}

    public static String of(String address) {
        if (address == null) return "";
        String a = address.trim().toLowerCase(Locale.ROOT);
        if (a.endsWith(".")) a = a.substring(0, a.length() - 1);
        if (a.endsWith(DEFAULT_PORT) && a.indexOf(':') == a.length() - DEFAULT_PORT.length()) {
            a = a.substring(0, a.length() - DEFAULT_PORT.length());
        }
        return a;
    }
}
