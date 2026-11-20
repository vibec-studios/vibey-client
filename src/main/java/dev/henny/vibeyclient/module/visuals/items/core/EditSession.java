package dev.henny.vibeyclient.module.visuals.items.core;

import java.util.function.UnaryOperator;

public final class EditSession {
    public static final int MAIN = 0, OFF = 1;

    private final HandPose[] start;
    private final HandPose[] poses;
    private int hand = MAIN;

    public EditSession(HandPose main, HandPose off) {
        start = new HandPose[] {main, off};
        poses = new HandPose[] {main, off};
    }

    public int hand() {
        return hand;
    }

    public void select(int hand) {
        this.hand = hand == OFF ? OFF : MAIN;
    }

    public HandPose pose(int hand) {
        return poses[hand == OFF ? OFF : MAIN];
    }

    public HandPose current() {
        return poses[hand];
    }

    public void edit(UnaryOperator<HandPose> change) {
        poses[hand] = change.apply(poses[hand]);
    }

    public void preset(HandPreset preset) {
        poses[hand] = preset.pose();
    }

    public void presetBoth(HandPreset preset) {
        poses[MAIN] = preset.pose();
        poses[OFF] = preset.pose();
    }

    public void reset() {
        poses[hand] = start[hand];
    }

    public void copyToOther() {
        poses[1 - hand] = poses[hand];
    }

    public boolean changed() {
        return !poses[MAIN].equals(start[MAIN]) || !poses[OFF].equals(start[OFF]);
    }

    public HandPreset matchingPreset() {
        return HandPreset.match(poses[hand]);
    }
}
