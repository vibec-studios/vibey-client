package dev.henny.cosmetico.mc.boot;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.host.BundleResources;
import dev.henny.cosmetico.mc.fx.EmbeddedSounds;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.model.Bundle;
import net.minecraft.client.Minecraft;

final class McResources implements BundleResources {
    static final McResources INSTANCE = new McResources();

    private McResources() {}

    @Override
    public void prepare(Bundle b) {
        GpuCache.prepare(b);
    }

    @Override
    public void evict(Bundle b) {
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.isSameThread()) evictNow(b);
        else Cosmetico.engine().main.post(() -> evictNow(b));
    }

    private static void evictNow(Bundle b) {
        GpuCache.evict(b);
        EmbeddedSounds.evict(b);
    }

    @Override
    public void prefetchSounds(Bundle b) {
        EmbeddedSounds.prefetch(b);
    }
}
