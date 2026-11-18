package dev.henny.vibeyclient.ui;

import net.minecraft.client.gui.components.EditBox;

import java.util.function.Consumer;

/**
 * Lets screens outside this package (module screens, overlays) drive a {@link Ui} context the way
 * {@link VibeyScreen} does: create it with the screen's {@code addWidget}, then per frame {@link #begin}/{@link #end},
 * and forward clicks, releases and wheel movement.
 */
public final class UiHost {
    private UiHost() {}

    public static Ui create(Consumer<EditBox> addWidget) {
        return new Ui(addWidget);
    }

    public static void begin(Ui ui, Gfx g, int mouseX, int mouseY, float delta) {
        ui.begin(g, mouseX, mouseY, delta);
    }

    public static void end(Ui ui) {
        ui.end();
    }

    /** @return true if something clickable (or a text field) is under the click */
    public static boolean click(Ui ui, double x, double y, int button) {
        return ui.queueClick(x, y, button);
    }

    public static void release(Ui ui) {
        ui.release();
    }

    public static void scroll(Ui ui, double amount) {
        ui.queueScroll(amount);
    }

    public static void reattach(Ui ui) {
        ui.reattach();
    }

    public static boolean anyInputFocused(Ui ui) {
        return ui.anyInputFocused();
    }

    /** The text field of {@code key} (created on first use, like {@link Widgets#textField}). */
    public static EditBox input(Ui ui, Object key, String value, int maxLength, Consumer<String> onChange) {
        return ui.input(key, value, maxLength, onChange);
    }
}
