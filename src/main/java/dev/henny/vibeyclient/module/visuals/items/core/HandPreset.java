package dev.henny.vibeyclient.module.visuals.items.core;

public enum HandPreset {
    VANILLA(HandPose.VANILLA),
    SMALL(new HandPose(0.04f, -0.05f, 0, 0, 0, 0, 0.65f)),
    LOW(new HandPose(0, -0.16f, 0.02f, 0, 0, 0, 1)),
    CENTERED(new HandPose(-0.34f, 0.04f, -0.06f, 0, 8, 0, 0.9f));

    private final HandPose pose;

    HandPreset(HandPose pose) {
        this.pose = pose;
    }

    public HandPose pose() {
        return pose;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }

    public static HandPreset match(HandPose pose) {
        for (HandPreset p : values()) if (HandPose.near(p.pose, pose)) return p;
        return null;
    }

    public static HandPreset byId(String id) {
        for (HandPreset p : values()) if (p.id().equals(id)) return p;
        return null;
    }
}
