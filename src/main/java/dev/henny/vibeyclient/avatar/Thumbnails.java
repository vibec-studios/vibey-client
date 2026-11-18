package dev.henny.vibeyclient.avatar;

import dev.henny.vibeyclient.ui.Gfx;

/**
 * Item thumbnails for the wardrobe (builtin PNGs from the jar or the catalog's {@code thumbnailUrl}), loaded in the
 * background and uploaded as textures on the client thread. Call {@link #draw} every frame; it starts loading on first
 * use and returns false until the image is ready (draw a placeholder then).
 */
public final class Thumbnails {
    private Thumbnails() {}

    /** Draws the thumbnail of {@code item} into {@code size x size} at x, y. False if not (yet) available. */
    public static boolean draw(Gfx g, AvatarItem item, int x, int y, int size) {
        return ThumbnailCache.draw(g, item, x, y, size);
    }
}
