package dev.henny.vibeyclient.module.social.presence;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import java.util.regex.Pattern;

public final class ServerFields {
    private static final Pattern HOSTNAME = Pattern.compile("^(?=.{1,253}$)[A-Za-z0-9][A-Za-z0-9_-]{0,62}(\\.[A-Za-z0-9_-]{1,63})*\\.?$");
    private static final Pattern IPV6 = Pattern.compile("^(?=[^:]*:[^:]*:)[0-9A-Fa-f:.]+$");

    private ServerFields() {}

    public static void add(JsonObject activity, String ip, String listName) {
        String name = name(listName);
        HostPort hp = shareable(ip);
        put(activity, "serverName", name);
        put(activity, "serverAddress", hp == null ? null : hp.host());
        if (hp == null) activity.add("serverPort", JsonNull.INSTANCE);
        else activity.addProperty("serverPort", hp.port());
    }

    public static HostPort shareable(String ip) {
        HostPort hp = HostPort.parse(ip);
        if (hp == null || LocalAddress.isLocal(hp.host())) return null;
        String h = hp.host().trim();
        return HOSTNAME.matcher(h).matches() || IPV6.matcher(h).matches() ? new HostPort(h, hp.port()) : null;
    }

    static String name(String s) {
        if (s == null || s.isBlank()) return null;
        String t = s.trim();
        return t.length() <= 100 ? t : t.substring(0, 100).trim();
    }

    private static void put(JsonObject o, String key, String v) {
        if (v == null) o.add(key, JsonNull.INSTANCE);
        else o.addProperty(key, v);
    }
}
