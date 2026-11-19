package dev.henny.cosmetico.model;

public enum PlayerBone {
    HEAD("head", 0, 24, 0), BODY("body", 0, 24, 0), RIGHT_ARM("right_arm", 5, 22, 0), LEFT_ARM("left_arm", -5, 22, 0),
    RIGHT_LEG("right_leg", 1.9f, 12, 0), LEFT_LEG("left_leg", -1.9f, 12, 0), ROOT("root", 0, 0, 0);

    public static final PlayerBone[] VALUES = values();
    public final String id;
    public final float px, py, pz;

    PlayerBone(String id, float px, float py, float pz) {
        this.id = id;
        this.px = px;
        this.py = py;
        this.pz = pz;
    }

    public static PlayerBone of(String s) {
        for (PlayerBone b : VALUES) if (b.id.equals(s)) return b;
        return null;
    }
}
