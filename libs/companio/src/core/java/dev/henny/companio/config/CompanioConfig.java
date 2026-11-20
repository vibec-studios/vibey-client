package dev.henny.companio.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanioConfig {
    public List<String> active;
    public List<String> knownBuiltins;
    public String selected = "seal:seal";
    public boolean enabled = true;
    public String name = "";
    public String variant = "";
    public float size = 0f;
    public String command = "follow";
    public float volume = 1f;
    public boolean bubbles = true;
    public boolean showOthers = true;
    public boolean share = true;
    public Map<String, Saved> saved = new HashMap<>();
    public transient boolean serverHidden;
    public transient boolean hostHidden;

    public List<String> active() {
        return active == null ? List.of() : active;
    }

    public List<String> editActive() {
        if (active == null) active = new ArrayList<>();
        return active;
    }

    public boolean adoptBuiltin(String packId) {
        if (knownBuiltins == null) knownBuiltins = new ArrayList<>();
        if (knownBuiltins.contains(packId)) return false;
        knownBuiltins.add(packId);
        if (!editActive().contains(packId)) editActive().add(packId);
        return true;
    }

    public boolean shown() {
        return !serverHidden && !hostHidden;
    }
}
