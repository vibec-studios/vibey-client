package dev.henny.cosmetico.model;

public enum Attach {
    HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG, ROOT, WORLD;

    public static Attach of(String s) {
        if (s == null) return null;
        for (Attach a : values()) if (a.name().equalsIgnoreCase(s)) return a;
        return null;
    }

    public PlayerBone part() {
        return switch (this) {
            case HEAD -> PlayerBone.HEAD;
            case BODY -> PlayerBone.BODY;
            case RIGHT_ARM -> PlayerBone.RIGHT_ARM;
            case LEFT_ARM -> PlayerBone.LEFT_ARM;
            case RIGHT_LEG -> PlayerBone.RIGHT_LEG;
            case LEFT_LEG -> PlayerBone.LEFT_LEG;
            default -> null;
        };
    }
}
