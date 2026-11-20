package dev.henny.vibeyclient.module.waypoints.map;

import dev.henny.vibeyclient.module.waypoints.core.map.LruSet;
import dev.henny.vibeyclient.module.waypoints.core.map.TileData;
import dev.henny.vibeyclient.module.waypoints.core.map.TileKey;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import org.lwjgl.system.MemoryUtil;

import java.util.HashMap;
import java.util.Map;

public final class TileTextures {
    static final int MAX_TEXTURES = 160;
    static final int UPLOADS_PER_FRAME = 6;

    private static final class Tex {
        final Identifier id;
        final DynamicTexture texture;
        int version = -1;

        Tex(Identifier id, DynamicTexture texture) {
            this.id = id;
            this.texture = texture;
        }
    }

    private static int sequence;

    private final TileStore store;
    private final Map<TileKey, Tex> textures = new HashMap<>();
    private final LruSet<TileKey> lru = new LruSet<>(MAX_TEXTURES);
    private int uploadsLeft;
    private long uploads;

    TileTextures(TileStore store) {
        this.store = store;
    }

    public void beginFrame() {
        uploadsLeft = UPLOADS_PER_FRAME;
    }

    public Identifier get(TileKey key) {
        TileData data = store.request(key);
        Tex tex = textures.get(key);
        if (data != null && data.shadedVersion() >= 0 && (tex == null || tex.version != data.shadedVersion()) && uploadsLeft > 0) {
            uploadsLeft--;
            tex = upload(key, data, tex);
        }
        if (tex == null) return null;
        for (TileKey old : lru.touch(key)) release(old);
        return tex.id;
    }

    public long uploads() {
        return uploads;
    }

    public int count() {
        return textures.size();
    }

    private Tex upload(TileKey key, TileData data, Tex tex) {
        if (tex == null) {
            DynamicTexture dt = new DynamicTexture(() -> "vibeyclient map tile", TileKey.SIZE, TileKey.SIZE, true);
            Identifier id = Identifier.fromNamespaceAndPath("vibeyclient", "waypoints/tile_" + sequence++);
            Minecraft.getInstance().getTextureManager().register(id, dt);
            tex = new Tex(id, dt);
            textures.put(key, tex);
        }
        long pointer = tex.texture.getPixels().getPointer();
        tex.version = data.copyShaded(MemoryUtil.memIntBuffer(pointer, TileData.PIXELS));
        tex.texture.upload();
        uploads++;
        return tex;
    }

    private void release(TileKey key) {
        Tex tex = textures.remove(key);
        if (tex != null) Minecraft.getInstance().getTextureManager().release(tex.id);
    }

    public void clear() {
        for (TileKey k : textures.keySet().toArray(new TileKey[0])) release(k);
        lru.clear();
    }
}
