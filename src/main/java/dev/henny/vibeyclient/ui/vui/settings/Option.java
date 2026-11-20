package dev.henny.vibeyclient.ui.vui.settings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record Option(String value, String label) {
    public Map<String, Object> map() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("value", value);
        m.put("label", label);
        return m;
    }

    public static List<Map<String, Object>> maps(List<Option> options) {
        List<Map<String, Object>> out = new ArrayList<>(options.size());
        for (Option o : options) out.add(o.map());
        return List.copyOf(out);
    }
}
