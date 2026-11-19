package dev.henny.cosmetico.preview;

final class PreviewDrag {
    private int rx, ry, rw, rh;
    private boolean dragging;
    private double dragX;
    private float dragStartYaw;

    void rect(int x, int y, int w, int h) {
        rx = x;
        ry = y;
        rw = w;
        rh = h;
    }

    boolean dragging() {
        return dragging;
    }

    Float mouse(MouseAction action, double x, double y, float yaw) {
        switch (action) {
            case PRESS -> {
                if (!(x >= rx && x < rx + rw && y >= ry && y < ry + rh)) return null;
                dragging = true;
                dragX = x;
                dragStartYaw = yaw;
                return yaw;
            }
            case DRAG -> {
                return dragging ? dragStartYaw - (float) (x - dragX) * 0.9f : null;
            }
            default -> {
                boolean was = dragging;
                dragging = false;
                return was ? yaw : null;
            }
        }
    }
}
