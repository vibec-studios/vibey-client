package dev.henny.vibeyclient.avatar.preview;

import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.ui.Gfx;

/**
 * Renders the local player (skin + equipped cosmetics, optional emote) into a GUI rectangle. Works in a world and on
 * the title screen (no player entity needed). Registered as a service: {@code Services.get(AvatarPreview.class)}.
 * Each screen keeps its own {@link View} (rotation, emote, try-on overrides).
 */
public interface AvatarPreview {
    View create();

    enum MouseAction { PRESS, DRAG, RELEASE }

    interface View {
        /** Draws the player centered in {@code w x h} (feet near the bottom edge). Call every frame. */
        void draw(Gfx g, int x, int y, int w, int h, float delta);

        /** Body yaw in degrees (0 = facing the viewer). */
        float yaw();

        void setYaw(float degrees);

        /** Turns slowly while nobody drags (default off). */
        void setAutoRotate(boolean on);

        /**
         * Drag-to-rotate: forward the screen's mouse press/drag/release (screen coordinates). Returns true when the
         * event belongs to the preview (press inside the last drawn rectangle, or a drag that started there).
         */
        boolean mouse(MouseAction action, double x, double y);

        /** Plays an emote on the preview (loops if the emote loops, otherwise once). */
        void playEmote(String emoteId);

        void stopEmote();

        /** Emote playing on the preview, or null. */
        String emote();

        /** Shows {@code itemId} in {@code slot} instead of the equipped item (null id = slot shown empty). */
        void override(Slot slot, String itemId);

        /** Back to the real loadout for all slots. */
        void clearOverrides();
    }
}
