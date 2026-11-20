package dev.henny.vibeyclient.gametest.parity;

import net.minecraft.client.renderer.entity.LivingEntityRenderer;

import java.lang.reflect.Field;
import java.util.List;

final class LayerCount {
    private LayerCount() {}

    static void assertOne(Object renderer) {
        try {
            Field f = LivingEntityRenderer.class.getDeclaredField("layers");
            f.setAccessible(true);
            long n = ((List<?>) f.get(renderer)).stream().filter(l -> l.getClass().getSimpleName().equals("CosmeticLayer")).count();
            System.out.println("[vibeyclient-test] cosmetic layers on the player renderer: " + n);
            if (n != 1) throw new AssertionError("expected exactly one cosmetic layer, found " + n);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
