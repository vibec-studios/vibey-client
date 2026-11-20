package dev.henny.cosmetico.mc.tick;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.fx.EventRunner;
import dev.henny.cosmetico.mc.fx.EmbeddedSounds;
import dev.henny.cosmetico.mc.fx.Sprites;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.sim.MotionInput;
import dev.henny.cosmetico.sim.MotionModel;
import dev.henny.cosmetico.sim.PetFollower;
import dev.henny.cosmetico.sim.PetInput;
import dev.henny.cosmetico.state.Avatar;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

public final class McTicker {
    private McTicker() {}

    private static final long MIN_GAP_NANOS = 25_000_000L;
    private static long tick;
    private static long lastNanos;
    private static EventRunner events;

    public static void tick() {
        long t = System.nanoTime();
        if (t - lastNanos < MIN_GAP_NANOS) return;
        lastNanos = t;
        tick++;
        GpuCache.tick();
        Sprites.pools().tick();
        EmbeddedSounds.cleanup();
        Engine e = Cosmetico.engine();
        if (events == null) events = e.eventRunner();
        double now = e.clock.now();
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            for (Avatar a : e.avatars.all()) e.emotes.tick(a, now);
            return;
        }
        for (Player p : mc.level.players()) {
            Avatar a = e.avatars.peek(p.getUUID());
            if (a != null) avatar(e, a, p, now);
        }
        if (tick % 200 == 0) e.avatars.prune(tick, 600);
    }

    private static void avatar(Engine e, Avatar a, Player p, double now) {
        a.lastSeenTick = tick;
        MotionModel.step(a, motion(p), now);
        PetFollower.step(a.pet, e.shown.bundle(a, Slot.PET), new PetInput(p.getX(), p.getY(), p.getZ(), p.yBodyRot), tick);
        e.emotes.tick(a, now);
        events.tick(a, now);
    }

    private static MotionInput motion(Player p) {
        return new MotionInput(p.walkAnimation.speed(), p.getAbilities().flying, p.isFallFlying(), p.getX(), p.getY(), p.getZ(),
                p.onGround(), p.isInWater(), p.isCrouching(), p.isSprinting(), p.isSwimming(), p.yBodyRot);
    }

    public static long ticks() {
        return tick;
    }
}
