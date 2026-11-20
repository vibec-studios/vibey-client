package dev.henny.vui.mc.draw;

import dev.henny.vui.render.Cursor;

final class CursorShapes {
    private CursorShapes() {
    }

    //? if >=1.21.9 {
    static com.mojang.blaze3d.platform.cursor.CursorType of(Cursor cursor) {
        return switch (cursor) {
            case POINTER -> com.mojang.blaze3d.platform.cursor.CursorTypes.POINTING_HAND;
            case TEXT -> com.mojang.blaze3d.platform.cursor.CursorTypes.IBEAM;
            case MOVE, GRAB, RESIZE_NWSE, RESIZE_NESW -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_ALL;
            case RESIZE_NS -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_NS;
            case RESIZE_EW -> com.mojang.blaze3d.platform.cursor.CursorTypes.RESIZE_EW;
            case NOT_ALLOWED -> com.mojang.blaze3d.platform.cursor.CursorTypes.NOT_ALLOWED;
            default -> com.mojang.blaze3d.platform.cursor.CursorTypes.ARROW;
        };
    }
    //?}
}
