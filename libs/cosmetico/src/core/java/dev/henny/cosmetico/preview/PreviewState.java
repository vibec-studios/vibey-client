package dev.henny.cosmetico.preview;

import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.sim.StateStarts;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.EmoteControl;

public final class PreviewState implements PreviewView {
    public final Avatar avatar = new Avatar(null, true);
    private final EmoteControl emotes;
    private final ShownBundle shown;
    private final PreviewDrag drag = new PreviewDrag();
    private float yaw;
    private boolean auto;
    private double lastFrame;

    public PreviewState(EmoteControl emotes, ShownBundle shown) {
        this.emotes = emotes;
        this.shown = shown;
        avatar.pet.init = true;
    }

    public float frame(int x, int y, int w, int h, float partial, double now, Avatar self) {
        drag.rect(x, y, w, h);
        float dt = lastFrame == 0 ? 0 : (float) Math.min(0.1, now - lastFrame);
        lastFrame = now;
        if (auto && !drag.dragging()) yaw += dt * 18f;
        for (Slot s : Slot.all()) avatar.loadout.base[s.ordinal()] = self == null ? null : self.loadout.base[s.ordinal()];
        emotes.tick(avatar, now);
        if (avatar.emote != null && !avatar.emote.loops() && avatar.emote.finished(now)) avatar.emote = null;
        pose(partial, dt, now);
        Bundle pet = shown.bundle(avatar, Slot.PET);
        PreviewLayout.placePet(avatar, pet);
        return PreviewLayout.size(w, h, PreviewLayout.span(pet));
    }

    private void pose(float partial, float dt, double now) {
        avatar.frame.hidden = false;
        avatar.frame.partial = partial;
        avatar.frame.x = avatar.frame.y = avatar.frame.z = 0;
        avatar.frame.bodyYaw = 180f + yaw;
        avatar.motion.still();
        avatar.motion.happyTarget = avatar.emote != null && !avatar.emote.stopping() ? 1 : 0;
        avatar.motion.happy += (avatar.motion.happyTarget - avatar.motion.happy) * Math.min(1f, dt * 5f);
        StateStarts.update(avatar.motion, now);
    }

    @Override
    public float yaw() {
        return yaw;
    }

    @Override
    public void setYaw(float degrees) {
        yaw = degrees;
    }

    @Override
    public void setAutoRotate(boolean on) {
        auto = on;
    }

    @Override
    public boolean mouse(MouseAction action, double x, double y) {
        Float next = drag.mouse(action, x, y, yaw);
        if (next == null) return false;
        yaw = next;
        return true;
    }

    @Override
    public void playEmote(String emoteId) {
        emotes.play(avatar, emoteId);
    }

    @Override
    public void stopEmote() {
        emotes.stop(avatar);
    }

    @Override
    public String emote() {
        return avatar.emote == null || avatar.emote.stopping() ? null : avatar.emote.id;
    }

    @Override
    public void override(Slot slot, String itemId) {
        avatar.loadout.override(slot, itemId);
    }

    @Override
    public void clearOverrides() {
        avatar.loadout.clearOverrides();
    }
}
