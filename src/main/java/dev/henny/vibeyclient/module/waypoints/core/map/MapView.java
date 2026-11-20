package dev.henny.vibeyclient.module.waypoints.core.map;

public final class MapView {
    public static final double MIN_ZOOM = 0.5;
    public static final double MAX_ZOOM = 8;

    private double centerX;
    private double centerZ;
    private double zoom = 1;
    private double width = 1;
    private double height = 1;

    public MapView center(double x, double z) {
        centerX = x;
        centerZ = z;
        return this;
    }

    public MapView size(double w, double h) {
        width = Math.max(1, w);
        height = Math.max(1, h);
        return this;
    }

    public MapView zoom(double z) {
        zoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z));
        return this;
    }

    public double zoom() {
        return zoom;
    }

    public double centerX() {
        return centerX;
    }

    public double centerZ() {
        return centerZ;
    }

    public double screenX(double worldX) {
        return width / 2 + (worldX - centerX) * zoom;
    }

    public double screenY(double worldZ) {
        return height / 2 + (worldZ - centerZ) * zoom;
    }

    public double worldX(double screenX) {
        return centerX + (screenX - width / 2) / zoom;
    }

    public double worldZ(double screenY) {
        return centerZ + (screenY - height / 2) / zoom;
    }

    public void pan(double dxScreen, double dyScreen) {
        centerX -= dxScreen / zoom;
        centerZ -= dyScreen / zoom;
    }

    public void zoomAt(double sx, double sy, double factor) {
        double wx = worldX(sx), wz = worldZ(sy);
        zoom(zoom * factor);
        centerX = wx - (sx - width / 2) / zoom;
        centerZ = wz - (sy - height / 2) / zoom;
    }

    public int minTileX() {
        return Math.floorDiv((int) Math.floor(worldX(0)), TileKey.SIZE);
    }

    public int maxTileX() {
        return Math.floorDiv((int) Math.floor(worldX(width)), TileKey.SIZE);
    }

    public int minTileZ() {
        return Math.floorDiv((int) Math.floor(worldZ(0)), TileKey.SIZE);
    }

    public int maxTileZ() {
        return Math.floorDiv((int) Math.floor(worldZ(height)), TileKey.SIZE);
    }
}
