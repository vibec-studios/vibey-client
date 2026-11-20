package dev.henny.vibeyclient.module.chat.core;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

public final class Timestamps {
    private static final Map<TimeStyle, DateTimeFormatter> FORMATS = new EnumMap<>(TimeStyle.class);

    static {
        for (TimeStyle s : TimeStyle.values()) FORMATS.put(s, DateTimeFormatter.ofPattern(s.pattern(), Locale.ROOT));
    }

    private Timestamps() {}

    public static String format(long epochMillis, TimeStyle style, ZoneId zone) {
        return FORMATS.get(style).format(Instant.ofEpochMilli(epochMillis).atZone(zone));
    }

    public static String bracketed(long epochMillis, TimeStyle style, ZoneId zone) {
        return "[" + format(epochMillis, style, zone) + "] ";
    }
}
