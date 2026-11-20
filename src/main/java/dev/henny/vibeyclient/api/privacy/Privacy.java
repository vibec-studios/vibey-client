package dev.henny.vibeyclient.api.privacy;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class Privacy {
    public record Flags(boolean names, boolean skins, boolean server) {
        public static final Flags OFF = new Flags(false, false, false);

        public boolean any() {
            return names || skins || server;
        }
    }

    private static final NameAliases ALIASES = new NameAliases();
    private static final List<Runnable> LISTENERS = new CopyOnWriteArrayList<>();
    private static volatile Flags flags = Flags.OFF;
    private static volatile NameRedactor redactor = NameRedactor.NONE;
    private static volatile int version;

    private Privacy() {}

    public static Flags flags() {
        return flags;
    }

    public static boolean hideNames() {
        return flags.names;
    }

    public static boolean hideSkins() {
        return flags.skins;
    }

    public static boolean hideServer() {
        return flags.server;
    }

    public static int version() {
        return version;
    }

    public static NameAliases aliases() {
        return ALIASES;
    }

    public static NameRedactor redactor() {
        return flags.names ? redactor : NameRedactor.NONE;
    }

    public static void set(Flags next) {
        Flags f = next == null ? Flags.OFF : next;
        if (f.equals(flags)) return;
        flags = f;
        if (!f.names) ALIASES.clear();
        changed();
    }

    public static void names(Collection<String> known) {
        NameRedactor next = NameRedactor.of(known, ALIASES);
        if (next.names().equals(redactor.names())) return;
        redactor = next;
        if (flags.names) changed();
    }

    public static void listen(Runnable r) {
        LISTENERS.add(r);
    }

    private static void changed() {
        version++;
        for (Runnable r : LISTENERS) r.run();
    }
}
