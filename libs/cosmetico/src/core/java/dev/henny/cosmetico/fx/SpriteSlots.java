package dev.henny.cosmetico.fx;

final class SpriteSlots {
    private SpriteSlots() {}

    static void move(SpritePool p, int from, int to) {
        p.x[to] = p.x[from];
        p.y[to] = p.y[from];
        p.z[to] = p.z[from];
        p.px[to] = p.px[from];
        p.py[to] = p.py[from];
        p.pz[to] = p.pz[from];
        p.vx[to] = p.vx[from];
        p.vy[to] = p.vy[from];
        p.vz[to] = p.vz[from];
        p.age[to] = p.age[from];
        p.life[to] = p.life[from];
        p.size[to] = p.size[from];
        p.gravity[to] = p.gravity[from];
        p.bundle[to] = p.bundle[from];
        p.sprite[to] = p.sprite[from];
        p.emissive[to] = p.emissive[from];
    }
}
