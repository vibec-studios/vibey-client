package dev.henny.r3.math;

import java.util.Locale;

public enum Face {
    DOWN(0, -1, 0),
    UP(0, 1, 0),
    NORTH(0, 0, -1),
    SOUTH(0, 0, 1),
    WEST(-1, 0, 0),
    EAST(1, 0, 0);

    private final int stepX;
    private final int stepY;
    private final int stepZ;

    Face(int stepX, int stepY, int stepZ) {
        this.stepX = stepX;
        this.stepY = stepY;
        this.stepZ = stepZ;
    }

    public int stepX() {
        return stepX;
    }

    public int stepY() {
        return stepY;
    }

    public int stepZ() {
        return stepZ;
    }

    public boolean vertical() {
        return stepY != 0;
    }

    public int axis() {
        return stepX != 0 ? 0 : stepY != 0 ? 1 : 2;
    }

    public Face opposite() {
        return switch (this) {
            case DOWN -> UP;
            case UP -> DOWN;
            case NORTH -> SOUTH;
            case SOUTH -> NORTH;
            case WEST -> EAST;
            case EAST -> WEST;
        };
    }

    public Face clockwise() {
        return switch (this) {
            case NORTH -> EAST;
            case EAST -> SOUTH;
            case SOUTH -> WEST;
            case WEST -> NORTH;
            default -> this;
        };
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Face byName(String name) {
        if (name == null) return null;
        for (Face f : values()) if (f.serializedName().equals(name)) return f;
        return null;
    }

    public static Face horizontalFromYaw(float yawDegrees) {
        int index = Math.floorMod((int) Math.floor(yawDegrees / 90.0 + 0.5), 4);
        return switch (index) {
            case 0 -> SOUTH;
            case 1 -> WEST;
            case 2 -> NORTH;
            default -> EAST;
        };
    }
}
