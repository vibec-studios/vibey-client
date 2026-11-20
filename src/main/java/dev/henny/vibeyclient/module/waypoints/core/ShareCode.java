package dev.henny.vibeyclient.module.waypoints.core;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ShareCode {
    public static final String PREFIX = "VWP1.";
    public static final int MAX_CHARS = 4096;
    public static final int MAX_WAYPOINTS = 64;
    private static final Pattern FIND = Pattern.compile("VWP1\\.[A-Za-z0-9_-]{4,4096}");

    public static final class Invalid extends Exception {
        public Invalid(String reason) {
            super(reason);
        }
    }

    private ShareCode() {}

    public static String encode(List<Waypoint> list) {
        StringBuilder payload = new StringBuilder();
        int n = 0;
        for (Waypoint w : list) {
            if (n++ >= MAX_WAYPOINTS) break;
            if (payload.length() > 0) payload.append('|');
            Pos p = w.pos();
            payload.append(p.x()).append(',').append(p.y()).append(',').append(p.z()).append(',')
                    .append(Dimensions.shortCode(w.dimension())).append(',').append(Colors.hex(w.color())).append(',')
                    .append(NameEscape.escape(w.name()));
        }
        byte[] bytes = payload.toString().getBytes(StandardCharsets.UTF_8);
        return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public static String find(String text) {
        if (text == null) return null;
        Matcher m = FIND.matcher(text);
        return m.find() ? m.group() : null;
    }

    public static List<Waypoint> decode(String text, Supplier<String> ids) throws Invalid {
        String code = find(text);
        if (code == null || code.length() > MAX_CHARS) throw new Invalid("format");
        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(code.substring(PREFIX.length())), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new Invalid("base64");
        }
        List<Waypoint> out = new ArrayList<>();
        for (String entry : payload.split("\\|")) {
            if (out.size() >= MAX_WAYPOINTS) break;
            out.add(entry(entry, ids));
        }
        return out;
    }

    private static Waypoint entry(String entry, Supplier<String> ids) throws Invalid {
        String[] f = entry.split(",", 6);
        if (f.length != 6) throw new Invalid("fields");
        long x, y, z;
        int color;
        try {
            x = Long.parseLong(f[0]);
            y = Long.parseLong(f[1]);
            z = Long.parseLong(f[2]);
            color = Colors.parseHex(f[4]);
        } catch (IllegalArgumentException e) {
            throw new Invalid("number");
        }
        String dim = Dimensions.fromShortCode(f[3]);
        if (!Pos.valid(x, y, z) || !Dimensions.valid(dim)) throw new Invalid("range");
        return new Waypoint(ids.get(), NameEscape.unescape(f[5]), new Pos((int) x, (int) y, (int) z), dim, color);
    }
}
