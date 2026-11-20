package dev.henny.companio.config;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CompanioConfig {
    public List<String> active;
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
        if (active == null) active = new ArrayList<>();
        return active;
    }

    public boolean shown() {
        return !serverHidden && !hostHidden;
    }
}
