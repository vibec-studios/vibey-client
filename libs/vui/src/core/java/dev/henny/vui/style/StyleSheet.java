package dev.henny.vui.style;

import dev.henny.vui.diag.Diagnostics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class StyleSheet {
    private final String source;
    private final List<Rule> rules = new ArrayList<>();
    private final Map<String, Keyframes> keyframes = new HashMap<>();
    private final Diagnostics diagnostics;

    public StyleSheet(String source, Diagnostics diagnostics) {
        this.source = source;
        this.diagnostics = diagnostics;
    }

    public static StyleSheet empty() {
        return new StyleSheet("<empty>", new Diagnostics());
    }

    public void add(Rule rule) {
        rules.add(rule);
    }

    public void add(Keyframes frames) {
        keyframes.put(frames.name(), frames);
    }

    public String source() {
        return source;
    }

    public List<Rule> rules() {
        return Collections.unmodifiableList(rules);
    }

    public Keyframes keyframes(String name) {
        return keyframes.get(name);
    }

    public Map<String, Keyframes> allKeyframes() {
        return Collections.unmodifiableMap(keyframes);
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }
}
