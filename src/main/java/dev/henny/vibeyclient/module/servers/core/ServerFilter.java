package dev.henny.vibeyclient.module.servers.core;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ServerFilter {
    public static final ServerFilter ALL = new ServerFilter(List.of(), false);

    private final List<String> terms;
    private final boolean favouritesOnly;

    private ServerFilter(List<String> terms, boolean favouritesOnly) {
        this.terms = terms;
        this.favouritesOnly = favouritesOnly;
    }

    public static ServerFilter of(String query, boolean favouritesOnly) {
        List<String> terms = new ArrayList<>();
        if (query != null) for (String w : query.trim().split("\\s+")) if (!w.isEmpty()) terms.add(fold(w));
        return new ServerFilter(List.copyOf(terms), favouritesOnly);
    }

    public boolean active() {
        return favouritesOnly || !terms.isEmpty();
    }

    public boolean matches(String name, String address, String motd, boolean favourite) {
        if (favouritesOnly && !favourite) return false;
        if (terms.isEmpty()) return true;
        String hay = fold(name) + "\n" + fold(address) + "\n" + fold(motd);
        for (String t : terms) if (!hay.contains(t)) return false;
        return true;
    }

    static String fold(String s) {
        if (s == null) return "";
        String n = Normalizer.normalize(s.toLowerCase(Locale.ROOT).replace("ß", "ss"), Normalizer.Form.NFD);
        StringBuilder out = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) if (Character.getType(n.charAt(i)) != Character.NON_SPACING_MARK) out.append(n.charAt(i));
        return out.toString();
    }
}
