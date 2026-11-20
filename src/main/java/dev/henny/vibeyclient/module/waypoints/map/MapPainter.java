package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.TileKey;
import dev.henny.vibeyclient.ui.Gfx;
import net.minecraft.resources.Identifier;

public final class MapPainter {
    private MapPainter() {}

    public static int draw(Gfx g, TileTextures textures, String dimension, double centerX, double centerZ, double zoom,
                           double halfWidthPx, double halfHeightPx) {
        textures.beginFrame();
        int fx = (int) Math.floor(centerX), fz = (int) Math.floor(centerZ);
        int rx = (int) Math.ceil(halfWidthPx / zoom) + 1, rz = (int) Math.ceil(halfHeightPx / zoom) + 1;
        int minTx = Math.floorDiv(fx - rx, TileKey.SIZE), maxTx = Math.floorDiv(fx + rx, TileKey.SIZE);
        int minTz = Math.floorDiv(fz - rz, TileKey.SIZE), maxTz = Math.floorDiv(fz + rz, TileKey.SIZE);
        g.push();
        g.scale((float) zoom, (float) zoom);
        g.translate((float) -(centerX - fx), (float) -(centerZ - fz));
        int drawn = 0;
        for (int tz = minTz; tz <= maxTz; tz++) {
            for (int tx = minTx; tx <= maxTx; tx++) {
                Identifier id = textures.get(new TileKey(dimension, tx, tz));
                if (id == null) continue;
                int x = tx * TileKey.SIZE - fx, z = tz * TileKey.SIZE - fz;
                g.image(id, x, z, TileKey.SIZE, TileKey.SIZE, 0, 0, TileKey.SIZE, TileKey.SIZE, TileKey.SIZE, TileKey.SIZE);
                drawn++;
            }
        }
        g.pop();
        return drawn;
    }
}
