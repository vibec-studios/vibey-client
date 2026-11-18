package dev.henny.vibeyclient.api.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonPrimitive;
import dev.henny.vibeyclient.client.Keybinds;
import net.minecraft.client.KeyMapping;

/** All setting types. Create them through the helpers on {@code VibeyModule} (bool, intRange, ...). */
public final class Settings {
    private Settings() {}

    public static final class BoolSetting extends Setting<Boolean, BoolSetting> {
        public BoolSetting(String key, boolean def) {
            super(key, def);
        }

        public boolean on() {
            return value;
        }

        public void toggle() {
            set(!value);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        protected Boolean fromJson(JsonElement json) {
            return json.getAsBoolean();
        }
    }

    public static final class IntSetting extends Setting<Integer, IntSetting> {
        private final int min;
        private final int max;
        private final int step;
        private String unit = "";

        public IntSetting(String key, int def, int min, int max, int step) {
            super(key, def);
            this.min = min;
            this.max = max;
            this.step = Math.max(1, step);
        }

        public int min() { return min; }
        public int max() { return max; }
        public int step() { return step; }
        public int intValue() { return value; }
        public String unit() { return unit; }

        /** Shown after the number, e.g. "%" or " ms". */
        public IntSetting unit(String unit) {
            this.unit = unit;
            return this;
        }

        @Override
        protected Integer sanitize(Integer v) {
            if (v == null) return defaultValue();
            int s = min + Math.round((v - min) / (float) step) * step;
            return Math.max(min, Math.min(max, s));
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        protected Integer fromJson(JsonElement json) {
            return json.getAsInt();
        }
    }

    public static final class FloatSetting extends Setting<Float, FloatSetting> {
        private final float min;
        private final float max;
        private final float step;
        private int decimals = 2;

        public FloatSetting(String key, float def, float min, float max, float step) {
            super(key, def);
            this.min = min;
            this.max = max;
            this.step = step;
        }

        public float min() { return min; }
        public float max() { return max; }
        public float step() { return step; }
        public float floatValue() { return value; }
        public int decimals() { return decimals; }

        public FloatSetting decimals(int decimals) {
            this.decimals = decimals;
            return this;
        }

        @Override
        protected Float sanitize(Float v) {
            if (v == null || v.isNaN()) return defaultValue();
            float s = step > 0 ? min + Math.round((v - min) / step) * step : v;
            return Math.max(min, Math.min(max, s));
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        protected Float fromJson(JsonElement json) {
            return json.getAsFloat();
        }
    }

    /** Enum choice. Value labels: {@code <nameKey>.<value in lower case>}. */
    public static final class EnumSetting<E extends Enum<E>> extends Setting<E, EnumSetting<E>> {
        private final Class<E> type;

        public EnumSetting(String key, E def) {
            super(key, def);
            this.type = def.getDeclaringClass();
        }

        public E[] values() {
            return type.getEnumConstants();
        }

        public String valueKey(E v) {
            return nameKey() + "." + v.name().toLowerCase(java.util.Locale.ROOT);
        }

        public void cycle(int dir) {
            E[] all = values();
            set(all[Math.floorMod(value.ordinal() + dir, all.length)]);
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value.name());
        }

        @Override
        protected E fromJson(JsonElement json) {
            return Enum.valueOf(type, json.getAsString());
        }
    }

    public static final class StringSetting extends Setting<String, StringSetting> {
        private final int maxLength;

        public StringSetting(String key, String def, int maxLength) {
            super(key, def);
            this.maxLength = maxLength;
        }

        public int maxLength() {
            return maxLength;
        }

        @Override
        protected String sanitize(String v) {
            if (v == null) return defaultValue();
            return v.length() > maxLength ? v.substring(0, maxLength) : v;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(value);
        }

        @Override
        protected String fromJson(JsonElement json) {
            return json.getAsString();
        }
    }

    /** ARGB color, stored as "#AARRGGBB". The UI offers {@link #presets()} as swatches. */
    public static final class ColorSetting extends Setting<Integer, ColorSetting> {
        private int[] presets = {0xFF8B6CFF, 0xFF3FE0C5, 0xFFFF5470, 0xFFFFB547, 0xFFF4F4F5, 0xFF6B6B75};

        public ColorSetting(String key, int def) {
            super(key, def);
        }

        public int argb() {
            return value;
        }

        public int[] presets() {
            return presets;
        }

        public ColorSetting presets(int... argb) {
            this.presets = argb.clone();
            return this;
        }

        @Override
        public JsonElement toJson() {
            return new JsonPrimitive(String.format("#%08X", value));
        }

        @Override
        protected Integer fromJson(JsonElement json) {
            String s = json.getAsString().trim();
            if (s.startsWith("#")) s = s.substring(1);
            long v = Long.parseLong(s, 16);
            if (s.length() <= 6) v |= 0xFF000000L;
            return (int) v;
        }
    }

    /**
     * Keybind, registered with the loader at startup (so it must be created in the module constructor or onLoad).
     * The key itself is stored by vanilla in options.txt. Presses only reach {@link #onPress} while the owning module
     * is enabled; name in the vanilla controls screen: {@code key.vibeyclient.<group>.<key>} (vanilla lang file).
     */
    public static final class KeybindSetting extends Setting<Integer, KeybindSetting> {
        private final KeyMapping mapping;
        private Runnable onPress;
        java.util.function.BooleanSupplier active = () -> true;

        public KeybindSetting(String groupId, String key, int defaultKey) {
            super(key, defaultKey);
            this.mapping = Keybinds.create("key.vibeyclient." + groupId + "." + key, defaultKey, groupId, this);
        }

        public KeyMapping mapping() {
            return mapping;
        }

        public KeybindSetting onPress(Runnable action) {
            this.onPress = action;
            return this;
        }

        /** Held down right now (and the owning module is enabled). */
        public boolean isDown() {
            return active.getAsBoolean() && mapping.isDown();
        }

        public boolean isActive() {
            return active.getAsBoolean();
        }

        /** Called by the core once per click, client thread. */
        public void fire() {
            if (onPress != null && active.getAsBoolean()) onPress.run();
        }

        public void setActive(java.util.function.BooleanSupplier active) {
            this.active = active;
        }

        @Override
        public boolean persisted() {
            return false;
        }

        @Override
        public JsonElement toJson() {
            return JsonNull.INSTANCE;
        }

        @Override
        protected Integer fromJson(JsonElement json) {
            return null;
        }
    }

    /** A button in the settings UI that runs {@code action} (client thread). Label: {@code <nameKey>}. */
    public static final class ActionSetting extends Setting<Boolean, ActionSetting> {
        private final Runnable action;

        public ActionSetting(String key, Runnable action) {
            super(key, Boolean.FALSE);
            this.action = action;
        }

        public void run() {
            action.run();
        }

        @Override
        public boolean persisted() {
            return false;
        }

        @Override
        public JsonElement toJson() {
            return JsonNull.INSTANCE;
        }

        @Override
        protected Boolean fromJson(JsonElement json) {
            return null;
        }
    }
}
