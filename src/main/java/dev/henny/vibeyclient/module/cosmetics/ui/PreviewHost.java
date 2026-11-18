package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.vibeyclient.Compat;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;

/**
 * One screen's 3D player preview. Looks the {@link AvatarPreview} service up every frame and (re)creates its
 * {@link AvatarPreview.View} when the service appears or changes; without the service (or if the view throws) a
 * placeholder is drawn instead, so the screens never depend on the engine being there.
 */
final class PreviewHost {
    private AvatarPreview service;
    private AvatarPreview.View view;
    private boolean autoRotate;
    private float defaultYaw;
    private boolean failed;
    /** Try-on currently applied by hovering (slot + id), to undo it when the hover ends. */
    private Slot tryOnSlot;
    private String tryOnId;

    PreviewHost autoRotate(boolean on) {
        autoRotate = on;
        if (view != null) view.setAutoRotate(on);
        return this;
    }

    /** The current view, or null without a preview service. */
    AvatarPreview.View view() {
        AvatarPreview s = Services.get(AvatarPreview.class).orElse(null);
        if (s != service) {
            service = s;
            view = null;
            failed = false;
            tryOnSlot = null;
            tryOnId = null;
            if (s != null) {
                try {
                    view = s.create();
                    if (view != null) {
                        defaultYaw = view.yaw();
                        view.setAutoRotate(autoRotate);
                        if (facing != null) view.setYaw(yawFor(facing));
                    }
                } catch (RuntimeException e) {
                    fail(e);
                }
            }
        }
        return failed ? null : view;
    }

    boolean available() {
        return view() != null;
    }

    void draw(Gfx g, int x, int y, int w, int h, float delta) {
        AvatarPreview.View v = view();
        if (v != null) {
            try {
                v.draw(g, x, y, w, h, delta);
                return;
            } catch (RuntimeException e) {
                fail(e);
            }
        }
        placeholder(g, x, y, w, h);
    }

    private final TextCache placeholderText = new TextCache();

    private void placeholder(Gfx g, int x, int y, int w, int h) {
        int s = Math.min(32, Math.min(w, h) / 3);
        int cy = y + h / 2 - s / 2 - 8;
        if (s >= 12) {
            g.image(Compat.id("icon.png"), x + (w - s) / 2, cy, s, s, 512);
            g.rect(x + (w - s) / 2, cy, s, s, 0x99000000 | (Theme.PANEL & 0xFFFFFF));
        }
        int ty = cy + s + 8;
        for (String line : placeholderText.wrap(g, Lang.tr("wardrobe.preview.unavailable"), w - 12)) {
            g.textCenter(line, x + w / 2, ty, Theme.DIM);
            ty += 10;
        }
    }

    boolean mouse(AvatarPreview.MouseAction action, double x, double y) {
        AvatarPreview.View v = view();
        if (v == null) return false;
        try {
            return v.mouse(action, x, y);
        } catch (RuntimeException e) {
            fail(e);
            return false;
        }
    }

    private Slot facing;

    /** Turns the preview so the category's items are visible (capes, wings and back items from behind). */
    void faceFor(Slot slot) {
        facing = slot;
        AvatarPreview.View v = view;
        if (v != null && !failed) {
            try {
                v.setYaw(yawFor(slot));
            } catch (RuntimeException e) {
                fail(e);
            }
        }
    }

    private float yawFor(Slot slot) {
        return slot == Slot.CAPE || slot == Slot.WINGS || slot == Slot.BACK ? 155f : defaultYaw;
    }

    /** Shows {@code id} in {@code slot} while hovered; null slot/id ends the try-on. Cheap to call every frame. */
    void tryOn(Slot slot, String id) {
        if (slot == tryOnSlot && (id == null ? tryOnId == null : id.equals(tryOnId))) return;
        AvatarPreview.View v = view();
        tryOnSlot = slot;
        tryOnId = id;
        if (v == null) return;
        try {
            v.clearOverrides();
            if (slot != null && id != null) v.override(slot, id);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    String tryOnId() {
        return tryOnId;
    }

    void playEmote(String id) {
        AvatarPreview.View v = view();
        if (v == null || id == null) return;
        try {
            v.playEmote(id);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    void stopEmote() {
        AvatarPreview.View v = view();
        if (v == null) return;
        try {
            v.stopEmote();
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    /** Emote playing on the preview, or null. */
    String emote() {
        AvatarPreview.View v = view();
        if (v == null) return null;
        try {
            return v.emote();
        } catch (RuntimeException e) {
            fail(e);
            return null;
        }
    }

    /** Back to the start angle, real loadout, no emote. */
    void reset() {
        AvatarPreview.View v = view();
        tryOnSlot = null;
        tryOnId = null;
        if (v == null) return;
        try {
            v.stopEmote();
            v.clearOverrides();
            v.setYaw(defaultYaw);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    private void fail(RuntimeException e) {
        if (!failed) Vibey.LOG.error("Avatar preview failed, showing a placeholder", e);
        failed = true;
    }
}
