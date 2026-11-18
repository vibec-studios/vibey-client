package dev.henny.vibeyclient.ui;

/**
 * A page inside the Vibey menu: a tab ({@link VibeyTabs}) or a module's custom settings page
 * ({@code VibeyModule#createSettingsPanel}). Drawn in immediate mode every frame with the {@link Ui} context.
 */
public interface Panel {
    void draw(Ui ui, int x, int y, int w, int h);

    /** The panel became visible. */
    default void onShow() {}

    /** The panel was hidden or the menu closed. */
    default void onHide() {}

    /** A key press; true if used. Escape closes the menu unless a panel uses it. */
    default boolean key(int keyCode) {
        return false;
    }

    /** A typed character while no text field has the focus (e.g. type-to-search); true if used. */
    default boolean typed(int codepoint) {
        return false;
    }
}
