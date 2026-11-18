package dev.henny.vibeyclient.api;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.event.Events;
import dev.henny.vibeyclient.api.event.VibeyEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Vibey's own translations: {@code assets/vibeyclient/i18n/<de_de|en_us>/<file>.json}, one file per module plus
 * {@code core.json}. Keys inside a file are prefixed with the file name: {@code "name"} in {@code social.json} is
 * {@code social.name}. Missing keys fall back to English, then to the key itself. Arguments: {@code {0}}, {@code {1}}.
 * The language follows the global setting (auto = game language, German if it starts with "de").
 */
public final class Lang {
    private Lang() {}

    public static final String EN = "en_us";
    public static final String DE = "de_de";

    private static final Set<String> FILES = new LinkedHashSet<>(java.util.List.of("core"));
    private static volatile Map<String, String> current = Map.of();
    private static volatile Map<String, String> fallback = Map.of();
    private static String code;

    /** Adds {@code i18n/<lang>/<file>.json} (modules are added automatically). */
    public static synchronized void addFile(String file) {
        if (FILES.add(file) && code != null) reload(code);
    }

    public static String tr(String key, Object... args) {
        ensure();
        String s = current.get(key);
        if (s == null) s = fallback.get(key);
        if (s == null) return key;
        if (args.length == 0) return s;
        StringBuilder out = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '{' && i + 2 < s.length() && s.charAt(i + 2) == '}' && Character.isDigit(s.charAt(i + 1))) {
                int idx = s.charAt(i + 1) - '0';
                out.append(idx < args.length ? String.valueOf(args[idx]) : "");
                i += 2;
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public static Component text(String key, Object... args) {
        return Component.literal(tr(key, args));
    }

    public static boolean has(String key) {
        ensure();
        return current.containsKey(key) || fallback.containsKey(key);
    }

    /** "de_de" or "en_us". */
    public static String code() {
        ensure();
        return code;
    }

    public static boolean german() {
        return DE.equals(code());
    }

    /** Called every client tick: follows the game language when the setting is "auto". */
    public static void tick() {
        String want = wanted();
        if (!want.equals(code)) {
            reload(want);
            Events.post(new VibeyEvents.LanguageChanged(want));
        }
    }

    private static void ensure() {
        if (code == null) reload(wanted());
    }

    private static String wanted() {
        GlobalSettings.Language l = GlobalSettings.language.get();
        if (l == GlobalSettings.Language.GERMAN) return DE;
        if (l == GlobalSettings.Language.ENGLISH) return EN;
        try {
            Minecraft mc = Minecraft.getInstance();
            if (mc != null && mc.options != null && mc.options.languageCode != null
                    && mc.options.languageCode.toLowerCase(java.util.Locale.ROOT).startsWith("de")) return DE;
        } catch (RuntimeException ignored) {
            // too early during startup
        }
        return EN;
    }

    public static synchronized void reload(String lang) {
        Map<String, String> en = load(EN);
        fallback = en;
        current = lang.equals(EN) ? en : load(lang);
        code = lang;
    }

    private static Map<String, String> load(String lang) {
        Map<String, String> map = new HashMap<>();
        for (String file : FILES) {
            String path = "/assets/" + Vibey.MOD_ID + "/i18n/" + lang + "/" + file + ".json";
            try (InputStream in = Lang.class.getResourceAsStream(path)) {
                if (in == null) continue;
                JsonObject json = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                flatten(file, json, map);
            } catch (Exception e) {
                Vibey.LOG.warn("Bad translation file {}: {}", path, e.toString());
            }
        }
        return map;
    }

    /** Nested objects are allowed: {"setting": {"x": "..."}} is the same as {"setting.x": "..."}. */
    private static void flatten(String prefix, JsonObject json, Map<String, String> out) {
        for (Map.Entry<String, JsonElement> e : json.entrySet()) {
            String key = prefix + "." + e.getKey();
            if (e.getValue().isJsonObject()) flatten(key, e.getValue().getAsJsonObject(), out);
            else if (e.getValue().isJsonPrimitive()) out.put(key, e.getValue().getAsString());
        }
    }
}
