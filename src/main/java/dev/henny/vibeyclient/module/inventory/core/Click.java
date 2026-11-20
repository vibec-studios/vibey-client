package dev.henny.vibeyclient.module.inventory.core;

public record Click(int slot, int button, ClickKind kind) {
    public static final int LEFT = 0, RIGHT = 1;

    public static Click left(int slot) {
        return new Click(slot, LEFT, ClickKind.PICKUP);
    }

    public static Click right(int slot) {
        return new Click(slot, RIGHT, ClickKind.PICKUP);
    }

    public static Click quickMove(int slot) {
        return new Click(slot, LEFT, ClickKind.QUICK_MOVE);
    }

    public static Click swap(int slot, int hotbar) {
        return new Click(slot, hotbar, ClickKind.SWAP);
    }
}
