package dev.henny.vibeyclient.backend;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Announcements of the backend ({@code GET /api/v1/announcements?app=client&version=<v>&lang=<de|en>}, public, see
 * vibe-launcher docs/DOMAINS.md): parsing, sanity limits and the visibility rules ({@code until}, {@code apps},
 * {@code max_version}). Pure logic, no game classes (JUnit: NoticesTest). The UI side is
 * {@code ui.notice.Announcements}.
 */
public final class Notices {
    private Notices() {}

    public enum Level { INFO, WARN }

    /**
     * One notice. {@code until}: epoch millis after which it is hidden, 0 = no end. {@code apps}: empty = every app.
     * {@code maxVersion}: hidden for client versions above it, null = all.
     */
    public record Notice(String id, Level level, String title, String body, String url, long until, List<String> apps,
                         String maxVersion) {}

    public static final int MAX_ITEMS = 20;
    public static final int MAX_TITLE = 120;
    public static final int MAX_BODY = 600;
    public static final String APP = "client";

    /** Valid items of a response ({@code {"items":[…]}}); bad items are skipped, a bad body gives an empty list. */
    public static List<Notice> parse(JsonElement body) {
        if (!(body instanceof JsonObject o) || !(o.get("items") instanceof JsonArray items)) return List.of();
        List<Notice> out = new ArrayList<>();
        for (JsonElement e : items) {
            if (out.size() >= MAX_ITEMS) break;
            if (!(e instanceof JsonObject it)) continue;
            Notice n = item(it);
            if (n != null && out.stream().noneMatch(x -> x.id().equals(n.id()))) out.add(n);
        }
        return Collections.unmodifiableList(out);
    }

    private static Notice item(JsonObject it) {
        String id = str(it, "id");
        if (id == null || !id.matches("[A-Za-z0-9._-]{1,64}")) return null;
        String title = clean(str(it, "title"), MAX_TITLE);
        if (title == null || title.isEmpty()) return null;
        String body = clean(str(it, "body"), MAX_BODY);
        String url = str(it, "url");
        if (url != null) {
            url = url.trim();
            String l = url.toLowerCase(Locale.ROOT);
            if (url.length() > 500 || !(l.startsWith("https://") || l.startsWith("http://")) || url.chars().anyMatch(c -> c <= ' ')) url = null;
        }
        Level level = "warn".equalsIgnoreCase(str(it, "level")) ? Level.WARN : Level.INFO;
        long until = 0;
        String u = str(it, "until");
        if (u != null && !u.isBlank()) {
            try {
                until = Instant.parse(u.trim()).toEpochMilli();
            } catch (RuntimeException ex) {
                return null; // an end we cannot read: rather not show it at all
            }
        }
        List<String> apps = new ArrayList<>();
        if (it.get("apps") instanceof JsonArray a) {
            for (JsonElement x : a) if (x.isJsonPrimitive()) apps.add(x.getAsString().trim().toLowerCase(Locale.ROOT));
        }
        String max = str(it, "max_version");
        if (max != null && max.isBlank()) max = null;
        return new Notice(id, level, title, body == null || body.isEmpty() ? null : body, url, until, List.copyOf(apps), max);
    }

    /** Shown for this app and version at {@code now}. */
    public static boolean visible(Notice n, long now, String version) {
        if (n.until() > 0 && now >= n.until()) return false;
        if (!n.apps().isEmpty() && !n.apps().contains(APP)) return false;
        return n.maxVersion() == null || compareVersions(version, n.maxVersion()) <= 0;
    }

    /**
     * Compares dotted versions numerically ("0.10.0" > "0.9.2"); missing parts are 0, anything after the numbers
     * ("-beta.1", "+build") is ignored.
     */
    public static int compareVersions(String a, String b) {
        int[] x = parts(a), y = parts(b);
        for (int i = 0; i < Math.max(x.length, y.length); i++) {
            int p = i < x.length ? x[i] : 0, q = i < y.length ? y[i] : 0;
            if (p != q) return Integer.compare(p, q);
        }
        return 0;
    }

    private static int[] parts(String v) {
        if (v == null) return new int[0];
        String s = v.trim();
        if (s.startsWith("v") || s.startsWith("V")) s = s.substring(1);
        List<Integer> out = new ArrayList<>();
        for (String p : s.split("\\.")) {
            int end = 0;
            while (end < p.length() && Character.isDigit(p.charAt(end))) end++;
            if (end == 0) break;
            try {
                out.add(Integer.parseInt(p.substring(0, Math.min(end, 9))));
            } catch (NumberFormatException e) {
                break;
            }
            if (end < p.length()) break; // "1-beta": stop after the number
        }
        int[] r = new int[out.size()];
        for (int i = 0; i < r.length; i++) r[i] = out.get(i);
        return r;
    }

    private static String clean(String s, int max) {
        if (s == null) return null;
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '§') i++; // no formatting codes (section sign + code letter)
            else if (c == '\n') b.append('\n');
            else if (c == '\t') b.append(' ');
            else if (!Character.isISOControl(c)) b.append(c);
        }
        String t = b.toString().trim();
        return t.length() > max ? t.substring(0, max - 1).trim() + "…" : t;
    }

    private static String str(JsonObject o, String key) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : null;
    }
}
