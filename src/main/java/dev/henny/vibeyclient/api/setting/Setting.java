package dev.henny.vibeyclient.api.setting;

import com.google.gson.JsonElement;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * One typed, persisted setting of a {@link SettingsGroup} (a module or the global settings). Values are only changed
 * on the client thread; listeners run on the thread that called {@link #set}. The settings UI renders every visible
 * setting automatically: label {@code <group>.setting.<key>}, optional description {@code <group>.setting.<key>.desc}.
 *
 * @param <T> value type
 * @param <S> the concrete setting type (so the builder methods chain)
 */
public abstract class Setting<T, S extends Setting<T, S>> {
    private final String key;
    private final T defaultValue;
    protected T value;
    SettingsGroup group;
    private boolean advanced;
    private String section;
    private BooleanSupplier visible = () -> true;
    private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

    protected Setting(String key, T defaultValue) {
        this.key = Objects.requireNonNull(key);
        this.defaultValue = defaultValue;
        this.value = defaultValue;
    }

    @SuppressWarnings("unchecked")
    protected final S self() {
        return (S) this;
    }

    public final String key() {
        return key;
    }

    public final T defaultValue() {
        return defaultValue;
    }

    public T get() {
        return value;
    }

    /** Sets the value (sanitized/clamped), notifies listeners and schedules a save if it changed. */
    public void set(T newValue) {
        T clean = sanitize(newValue);
        if (Objects.equals(clean, value)) return;
        value = clean;
        for (Consumer<T> l : listeners) {
            try {
                l.accept(clean);
            } catch (RuntimeException e) {
                dev.henny.vibeyclient.Vibey.LOG.error("Setting listener of {} failed", id(), e);
            }
        }
        if (group != null && persisted()) group.markDirty();
    }

    public void reset() {
        set(defaultValue);
    }

    public final S onChange(Consumer<T> listener) {
        listeners.add(listener);
        return self();
    }

    /**
     * Technical setting (addresses, ports, debug switches): shown only in the collapsed "Advanced · for creators"
     * area of the settings page, so normal players never have to read it.
     */
    public final S advanced() {
        advanced = true;
        return self();
    }

    /**
     * Puts the setting into a titled group on the settings page ({@code <group>.section.<key>}, e.g.
     * {@code cosmetics.section.types}). Settings without a section come first, without a title; sections appear in
     * the order of their first setting.
     */
    public final S section(String sectionKey) {
        section = sectionKey;
        return self();
    }

    /** The section key, or null. */
    public final String section() {
        return section;
    }

    /** i18n key of the section title, or null. */
    public final String sectionNameKey() {
        return section == null ? null : (group == null ? "core" : group.id()) + ".section." + section;
    }

    public final S visibleWhen(BooleanSupplier condition) {
        visible = condition;
        return self();
    }

    public final boolean isAdvanced() {
        return advanced;
    }

    public final boolean isVisible() {
        return visible.getAsBoolean();
    }

    public final SettingsGroup group() {
        return group;
    }

    /** {@code <group>.<key>}, e.g. {@code cosmetics.showOthers}. */
    public final String id() {
        return (group == null ? "?" : group.id()) + "." + key;
    }

    public String nameKey() {
        return (group == null ? "core" : group.id()) + ".setting." + key;
    }

    public String descriptionKey() {
        return nameKey() + ".desc";
    }

    /** Whether the value goes into the JSON file (keybinds live in options.txt, actions have no value). */
    public boolean persisted() {
        return true;
    }

    protected T sanitize(T v) {
        return v == null ? defaultValue : v;
    }

    /** Loads a stored value without notifying listeners (startup). */
    final void load(JsonElement json) {
        try {
            T v = fromJson(json);
            if (v != null) value = sanitize(v);
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Ignoring bad value for setting {}: {}", id(), json);
        }
    }

    public abstract JsonElement toJson();

    protected abstract T fromJson(JsonElement json);
}
