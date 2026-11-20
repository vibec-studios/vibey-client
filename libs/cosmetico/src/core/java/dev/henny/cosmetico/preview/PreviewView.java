package dev.henny.cosmetico.preview;

import dev.henny.cosmetico.model.Slot;

public interface PreviewView {
    float yaw();

    void setYaw(float degrees);

    void setAutoRotate(boolean on);

    boolean mouse(MouseAction action, double x, double y);

    void playEmote(String emoteId);

    void stopEmote();

    String emote();

    void override(Slot slot, String itemId);

    void clearOverrides();
}
