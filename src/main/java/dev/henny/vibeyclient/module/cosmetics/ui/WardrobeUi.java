package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.ui.Icons;
import dev.henny.vibeyclient.ui.MenuEntries;
import dev.henny.vibeyclient.ui.VibeyTabs;
import net.minecraft.client.gui.screens.Screen;

import java.util.function.BooleanSupplier;

/**
 * Entry points of the wardrobe UI (client thread only). Every screen looks the {@code Wardrobe} and
 * {@code AvatarPreview} services up each frame and shows a clean empty state while they are missing.
 */
public final class WardrobeUi {
    private WardrobeUi() {}

    public static final String TAB_ID = "wardrobe";

    /** Adds the "Wardrobe" tab to the Vibey menu (visible while the cosmetics module is on) and loads the strings. */
    public static void register() {
        register(() -> {
            ModuleManager mm = ModuleManager.get();
            return mm != null && mm.isEnabled("cosmetics");
        });
    }

    /** Same as {@link #register()} with an own visibility condition. Safe to call again (replaces the tab). */
    public static void register(BooleanSupplier visible) {
        Lang.addFile("wardrobe");
        VibeyTabs.register(new VibeyTabs.Tab(TAB_ID, "wardrobe.tab", 20, WardrobePanel::new, visible));
        // "Wardrobe" on the title screen and in the pause row opens the full-screen wardrobe (replaces the core
        // default, which would open the Vibey menu tab)
        MenuEntries.register(MenuEntries.Place.TITLE, new MenuEntries.Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 10,
                visible, WardrobeUi::openWardrobe));
        MenuEntries.register(MenuEntries.Place.PAUSE, new MenuEntries.Entry("wardrobe", "core.title.wardrobe", Icons.SHIRT, 10,
                visible, WardrobeUi::openWardrobe));
    }

    /** Opens the full-screen wardrobe on the last used category; Escape returns to {@code parent} (null = game). */
    public static void openWardrobe(Screen parent) {
        openWardrobe(parent, (Slot) null);
    }

    /** Opens the wardrobe on the category of {@code slot} (null = last used). */
    public static void openWardrobe(Screen parent, Slot slot) {
        openWardrobe(parent, slot == null ? null : Category.of(slot));
    }

    /** Opens the wardrobe on its emotes category. */
    public static void openWardrobeEmotes(Screen parent) {
        openWardrobe(parent, Category.EMOTES);
    }

    static void openWardrobe(Screen parent, Category category) {
        Lang.addFile("wardrobe");
        ClientCompat.openScreen(new WardrobeScreen(parent, category));
    }

    /**
     * Opens the emote wheel over the game. It closes and plays the pointed-at emote as soon as {@code keyHeld}
     * returns false (checked every client tick); a quick tap keeps it open until a click. Left click plays (or opens
     * the wheel editor on an empty slot), right click / Escape cancel. Does not pause the game.
     */
    public static void openWheel(BooleanSupplier keyHeld) {
        Lang.addFile("wardrobe");
        Screen current = ClientCompat.screen();
        if (current instanceof EmoteWheelScreen) return;
        ClientCompat.openScreen(new EmoteWheelScreen(current, keyHeld == null ? () -> false : keyHeld));
    }

    /** Opens the editor for the 8 wheel slots; closing returns to {@code parent} (null = game). */
    public static void openWheelEditor(Screen parent) {
        Lang.addFile("wardrobe");
        ClientCompat.openScreen(new WheelEditorScreen(parent));
    }

    /** True while one of the wardrobe screens (wardrobe, wheel, editor) is open. */
    public static boolean isOpen() {
        Screen s = ClientCompat.screen();
        return s instanceof WardrobeScreen || s instanceof EmoteWheelScreen || s instanceof WheelEditorScreen;
    }
}
