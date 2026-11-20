package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vibeyclient.api.setting.Setting;
import dev.henny.vibeyclient.api.setting.Settings.ActionSetting;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public final class SettingRow {
    final Setting<?, ?> setting;
    private final String kind;
    private String label;
    private String tip;
    private final SettingValue value;
    float min;
    float max = 1;
    float step = 0.01f;
    Function<Object, String> format = String::valueOf;
    List<Option> options = List.of();
    boolean compact;
    boolean icons;
    String placeholder = "";
    String button = "";
    int maxLength = 256;
    List<Integer> presets = List.of();
    Supplier<String> conflict = () -> "";
    Supplier<String> preview = () -> "";
    Supplier<String> previewFx = () -> "none";

    SettingRow(Setting<?, ?> setting, String kind, String label, String tip, SettingValue value) {
        this.setting = setting;
        this.kind = kind;
        this.label = label;
        this.tip = tip;
        this.value = value;
    }

    public String getId() { return setting.id(); }
    public String getKind() { return kind; }
    public String getLabel() { return label; }
    public String getTip() { return tip; }
    public SettingValue getValue() { return value; }
    public float getMin() { return min; }
    public float getMax() { return max; }
    public float getStep() { return step; }
    public List<java.util.Map<String, Object>> getOptions() { return Option.maps(options); }
    public boolean isCompact() { return compact; }
    public boolean isIcons() { return icons; }
    public String getPlaceholder() { return placeholder; }
    public String getButton() { return button; }
    public int getMaxLength() { return maxLength; }
    public List<Integer> getPresets() { return presets; }
    public String getDisplay() { return format.apply(value.get()); }
    public String getConflict() { return conflict.get(); }
    public String getPreview() { return preview.get(); }
    public String getPreviewFx() { return previewFx.get(); }
    public boolean isBlocked() { return setting.blockedByServer(); }
    public Setting<?, ?> setting() { return setting; }

    public SettingRow labelled(String text, String tooltip) {
        label = text;
        tip = tooltip == null ? "" : tooltip;
        return this;
    }

    public SettingRow withOptions(List<Option> list, boolean segmented) {
        options = List.copyOf(list);
        compact = segmented;
        return this;
    }

    public SettingRow withButton(String text) {
        button = text;
        return this;
    }

    public void run() {
        if (setting instanceof ActionSetting a && !isBlocked()) a.run();
    }

    public void sync() {
        value.sync();
    }
}
