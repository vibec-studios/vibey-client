package dev.henny.vibeyclient.ranks;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class LegacyBadges {
    private LegacyBadges() {}

    public static final String STAFF = "staff";
    private static final List<String> ORDER = List.of(STAFF, "supporter", "creator");

    public static Identity identity(List<String> badges) {
        List<String> known = new ArrayList<>();
        for (String id : ORDER) if (contains(badges, id)) known.add(id);
        if (known.isEmpty()) return Identity.EMPTY;
        return new Identity(known.get(0), List.copyOf(known.subList(1, Math.min(2, known.size()))), null);
    }

    private static boolean contains(List<String> badges, String id) {
        for (String b : badges) if (b != null && id.equals(b.toLowerCase(Locale.ROOT))) return true;
        return false;
    }
}
