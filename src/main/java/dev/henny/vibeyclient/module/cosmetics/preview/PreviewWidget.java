package dev.henny.vibeyclient.module.cosmetics.preview;

import dev.henny.cosmetico.mc.preview.McPreview;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.preview.MouseAction;
import dev.henny.cosmetico.preview.PreviewView;
import dev.henny.vibeyclient.ui.Gfx;

final class PreviewWidget implements AvatarPreview.View {
    private final PreviewView view;

    PreviewWidget(PreviewView view) {
        this.view = view;
    }

    @Override
    public void draw(Gfx g, int x, int y, int w, int h, float delta) {
        McPreview.draw(g.g, view, x, y, w, h, delta);
    }

    @Override
    public float yaw() {
        return view.yaw();
    }

    @Override
    public void setYaw(float degrees) {
        view.setYaw(degrees);
    }

    @Override
    public void setAutoRotate(boolean on) {
        view.setAutoRotate(on);
    }

    @Override
    public boolean mouse(MouseAction action, double x, double y) {
        return view.mouse(action, x, y);
    }

    @Override
    public void playEmote(String emoteId) {
        view.playEmote(emoteId);
    }

    @Override
    public void stopEmote() {
        view.stopEmote();
    }

    @Override
    public String emote() {
        return view.emote();
    }

    @Override
    public void override(Slot slot, String itemId) {
        view.override(slot, itemId);
    }

    @Override
    public void clearOverrides() {
        view.clearOverrides();
    }
}
