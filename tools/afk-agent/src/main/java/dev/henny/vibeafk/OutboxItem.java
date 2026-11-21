package dev.henny.vibeafk;

import java.util.Map;
import java.util.regex.Pattern;

public final class OutboxItem {
    private static final Pattern ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

    public final String id;
    public final boolean command;
    public final String text;
    public final long queuedAt;

    private OutboxItem(String id, boolean command, String text, long queuedAt) {
        this.id = id;
        this.command = command;
        this.text = text;
        this.queuedAt = queuedAt;
    }

    public static final class Invalid extends Exception {
        public final String id;

        Invalid(String id, String msg) {
            super(msg);
            this.id = id;
        }
    }

    public static OutboxItem parse(String line, long now) throws Invalid {
        Map<String, Object> o;
        try {
            o = Json.parseObject(line);
        } catch (RuntimeException e) {
            throw new Invalid(null, "not json");
        }
        Object rawId = o.get("id");
        String id = rawId instanceof String && ID.matcher((String) rawId).matches() ? (String) rawId : null;
        if (id == null) throw new Invalid(null, "bad id");
        Object type = o.get("type");
        boolean command;
        if ("chat".equals(type)) command = false;
        else if ("command".equals(type)) command = true;
        else throw new Invalid(id, "bad type");
        Object t = o.get("text");
        if (!(t instanceof String)) throw new Invalid(id, "no text");
        String raw = (String) t;
        for (int k = 0; k < raw.length(); k++) {
            char c = raw.charAt(k);
            if ((c < 0x20 && c != ' ') || c == 0x7f || c == '§') throw new Invalid(id, "bad character");
        }
        String text = raw.trim();
        if (command && text.startsWith("/")) text = text.substring(1).trim();
        if (text.isEmpty() || text.length() > 256) throw new Invalid(id, "bad length");
        return new OutboxItem(id, command, text, now);
    }
}
