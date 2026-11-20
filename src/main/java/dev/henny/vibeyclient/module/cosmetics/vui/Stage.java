package dev.henny.vibeyclient.module.cosmetics.vui;

import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.preview.MouseAction;
import dev.henny.vibeyclient.Vibey;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.function.Consumer;

final class Stage {
    private AvatarPreview service;
    private AvatarPreview.View view;
    private float defaultYaw;
    private boolean failed;

    boolean available() {
        return view() != null;
    }

    void draw(Gfx g, int x, int y, int w, int h, float delta) {
        run(v -> v.draw(g, x, y, w, h, delta));
    }

    boolean mouse(MouseAction action, double x, double y) {
        AvatarPreview.View v = view();
        if (v == null) return false;
        try {
            return v.mouse(action, x, y);
        } catch (RuntimeException e) {
            fail(e);
            return false;
        }
    }

    void show(Slot slot, String id) {
        run(v -> {
            v.clearOverrides();
            if (slot != null && id != null) v.override(slot, id);
        });
    }

    void face(Slot slot) {
        run(v -> v.setYaw(slot == Slot.CAPE || slot == Slot.WINGS || slot == Slot.BACK ? 155f : defaultYaw));
    }

    void play(String emote) {
        run(v -> v.playEmote(emote));
    }

    void stop() {
        run(AvatarPreview.View::stopEmote);
    }

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

    void reset() {
        run(v -> {
            v.stopEmote();
            v.clearOverrides();
            v.setYaw(defaultYaw);
        });
    }

    private void run(Consumer<AvatarPreview.View> action) {
        AvatarPreview.View v = view();
        if (v == null) return;
        try {
            action.accept(v);
        } catch (RuntimeException e) {
            fail(e);
        }
    }

    private AvatarPreview.View view() {
        AvatarPreview s = Services.get(AvatarPreview.class).orElse(null);
        if (s != service) {
            service = s;
            view = null;
            failed = false;
            if (s != null) {
                try {
                    view = s.create();
                    if (view != null) defaultYaw = view.yaw();
                } catch (RuntimeException e) {
                    fail(e);
                }
            }
        }
        return failed ? null : view;
    }

    private void fail(RuntimeException e) {
        if (!failed) Vibey.LOG.error("Wardrobe preview failed", e);
        failed = true;
    }
}
