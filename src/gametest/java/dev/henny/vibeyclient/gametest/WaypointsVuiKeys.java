package dev.henny.vibeyclient.gametest;

import dev.henny.vibeyclient.api.Lang;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WaypointsVuiKeys {
    private static final String[] DOCS = {"list", "map"};
    private static final Pattern KEY = Pattern.compile("t\\('([a-zA-Z0-9_.]+)'\\)");

    private WaypointsVuiKeys() {}

    static void check() {
        for (String doc : DOCS) {
            String path = "/assets/vibeyclient/vui/waypoints/" + doc + ".vui";
            try (InputStream in = WaypointsVuiKeys.class.getResourceAsStream(path)) {
                if (in == null) throw new AssertionError("missing " + path);
                Matcher m = KEY.matcher(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                while (m.find()) {
                    if (!Lang.has(m.group(1))) throw new AssertionError(path + ": no translation for " + m.group(1));
                }
            } catch (IOException e) {
                throw new AssertionError(path, e);
            }
        }
    }
}
