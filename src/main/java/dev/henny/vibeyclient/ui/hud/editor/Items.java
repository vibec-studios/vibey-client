package dev.henny.vibeyclient.ui.hud.editor;

import dev.henny.vibeyclient.ui.hud.HudElement;
import dev.henny.vibeyclient.ui.hud.HudOption;

import java.nio.file.Path;
import java.util.List;

public final class Items {
    private Items() {}

    public record Bar(String name, String pct, float barX, float barY, boolean above, boolean copy) {}

    public record Library(String id, String name, String tip, boolean on, boolean locked, boolean canAdd, HudElement element) {}

    public record Preset(String id, String name, String icon, String tip) {}

    public record Badge(String id, float x, float y, String tip) {}

    public record MenuEntry(String id, String icon, String label, String keys) {}

    public record Menu(float x, float y, List<MenuEntry> items) {}

    public record FileEntry(String name, Path path) {}

    public record Option(String key, String label, boolean toggle, boolean on, String value, String capture, HudOption option) {}

    public record AnchorCell(String id, boolean on) {}
}
