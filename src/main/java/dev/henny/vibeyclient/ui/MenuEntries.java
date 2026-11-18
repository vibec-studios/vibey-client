package dev.henny.vibeyclient.ui;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.ModuleManager;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * Entries of the Vibey title screen navigation and the Vibey row of the pause menu. Modules add theirs in
 * {@code onLoad}; registering an id that exists replaces it (the core ships defaults for "wardrobe", "friends" and
 * "host" that the owning modules may replace with their own screens). Example:
 * <pre>{@code
 * MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("wardrobe", "cosmetics.menu.wardrobe",
 *         Icons.SHIRT, 10, MenuEntries.whenEnabled("cosmetics"), parent -> ClientCompat.openScreen(new WardrobeScreen(parent))));
 * }</pre>
 * Labels are Vibey i18n keys; icons are {@link Icons} names. The action gets the screen the entry was clicked on
 * (use it as the parent of the screen you open).
 */
public final class MenuEntries {
    private MenuEntries() {}

    public enum Place { TITLE, PAUSE }

    /**
     * @param id       stable id (same id = replace)
     * @param labelKey Vibey i18n key of the label
     * @param icon     {@link Icons} name, may be null
     * @param order    lower first; core: title 0 singleplayer, 10 multiplayer, 20 wardrobe, 30 friends, 40 Vibey,
     *                 50 options, 60 mods, 1000 quit; pause 0 Vibey, 10 wardrobe, 20 friends, 30 host
     * @param visible  shown at all? (e.g. module enabled) – checked every frame, keep it cheap
     * @param action   click; gets the current screen
     * @param badge    small number on the entry (0 = none), e.g. unread messages
     */
    public record Entry(String id, String labelKey, String icon, int order, BooleanSupplier visible,
                        Consumer<Screen> action, IntSupplier badge) {
        public Entry(String id, String labelKey, String icon, int order, BooleanSupplier visible, Consumer<Screen> action) {
            this(id, labelKey, icon, order, visible, action, () -> 0);
        }
    }

    private static final List<Entry> TITLE = new ArrayList<>();
    private static final List<Entry> PAUSE = new ArrayList<>();

    private static List<Entry> list(Place p) {
        return p == Place.TITLE ? TITLE : PAUSE;
    }

    public static synchronized void register(Place place, Entry entry) {
        List<Entry> l = list(place);
        l.removeIf(e -> e.id().equals(entry.id()));
        l.add(entry);
        l.sort(Comparator.comparingInt(Entry::order));
    }

    public static synchronized void remove(Place place, String id) {
        list(place).removeIf(e -> e.id().equals(id));
    }

    /** The entries to show now (visible ones, in order). Never throws because of a broken supplier. */
    public static synchronized List<Entry> visible(Place place) {
        List<Entry> out = new ArrayList<>();
        for (Entry e : list(place)) {
            boolean v;
            try {
                v = e.visible().getAsBoolean();
            } catch (RuntimeException ex) {
                v = false;
            }
            if (v) out.add(e);
        }
        return out;
    }

    /** Visible while module {@code id} is enabled. */
    public static BooleanSupplier whenEnabled(String moduleId) {
        return () -> ModuleManager.get().isEnabled(moduleId);
    }

    /** Visible while module {@code id} is enabled and online features are on. */
    public static BooleanSupplier whenOnline(String moduleId) {
        return () -> GlobalSettings.online() && ModuleManager.get().isEnabled(moduleId);
    }

    /** Safe badge value. */
    public static int badge(Entry e) {
        try {
            return e.badge() == null ? 0 : e.badge().getAsInt();
        } catch (RuntimeException ex) {
            return 0;
        }
    }
}
