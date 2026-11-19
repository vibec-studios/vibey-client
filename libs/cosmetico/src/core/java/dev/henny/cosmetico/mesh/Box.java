package dev.henny.cosmetico.mesh;

record Box(float x0, float y0, float z0, float x1, float y1, float z1) {
    Box mirrored() {
        return new Box(x1, y0, z0, x0, y1, z1);
    }
}
