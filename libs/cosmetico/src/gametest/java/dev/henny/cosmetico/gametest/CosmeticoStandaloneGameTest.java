package dev.henny.cosmetico.gametest;

import com.mojang.authlib.GameProfile;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.thumbs.ResourceReader;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.player.RemotePlayer;

import java.util.Map;
import java.util.UUID;

public final class CosmeticoStandaloneGameTest implements FabricClientGameTest {
    private static final UUID MANNEQUIN = UUID.fromString("c0c0c0c0-0000-4000-8000-00000000c0de");

    @Override
    public void runTest(ClientGameTestContext context) {
        if (FabricLoader.getInstance().isModLoaded("vibeyclient")) throw new AssertionError("vibeyclient must not be loaded");
        context.getInput().resizeWindow(1280, 720);
        context.waitFor(mc -> Cosmetico.active(), 200);
        context.runOnClient(mc -> load());
        context.waitFor(mc -> Cosmetico.items().all().size() >= 5, 600);
        System.out.println("[cosmetico-standalone] host " + Cosmetico.host().getClass().getSimpleName() + ", items " + Cosmetico.items().all().size());
        StandaloneShots shots = new StandaloneShots(context);
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            world.getServer().runCommand("time set noon");
            world.getServer().runCommand("weather clear");
            RemotePlayer[] mannequin = new RemotePlayer[1];
            context.runOnClient(mc -> {
                mc.options.hideGui = true;
                mc.player.setYRot(0f);
                mc.player.setXRot(8f);
                mannequin[0] = new RemotePlayer(mc.level, new GameProfile(MANNEQUIN, "Mannequin"));
                mannequin[0].setPos(mc.player.getX(), mc.player.getY(), mc.player.getZ() + 3.2);
                Facing.set(mannequin[0], 180f);
                mc.level.addEntity(mannequin[0]);
                Cosmetico.loadouts().setIds(MANNEQUIN, Map.of("hat", "vibey:seal_beanie", "pet", "vibey:seal_buddy", "wings", "vibey:neon_wings"));
            });
            context.waitTicks(30);
            shots.take("s01_standalone_front");
            context.runOnClient(mc -> Facing.set(mannequin[0], 0f));
            context.runOnClient(mc -> Cosmetico.loadouts().setIds(MANNEQUIN, Map.of("cape", "vibey:vibe_cape", "hat", "vibey:seal_beanie")));
            context.waitTicks(20);
            shots.take("s02_standalone_cape_back");
            context.runOnClient(mc -> {
                Facing.set(mannequin[0], 180f);
                if (!Cosmetico.emotes().play(MANNEQUIN, "vibey:wave")) throw new AssertionError("wave not playable");
            });
            context.waitTicks(18);
            shots.take("s03_standalone_wave");
            boolean playing = context.computeOnClient(mc -> Cosmetico.emotes().playing(MANNEQUIN) != null);
            if (!playing) throw new AssertionError("emote stopped too early");
        }
        System.out.println("[cosmetico-standalone] done");
    }

    private static void load() {
        Engine e = Cosmetico.engine();
        Cosmetico.host().workers().execute(() -> {
            Map<String, Item> items = e.indexLoader().read(ResourceReader.classpath(CosmeticoStandaloneGameTest.class), "/cosmetico-demo/", ItemSource.BUILTIN);
            e.main.post(() -> e.items.putAll(ItemSource.BUILTIN, items, true));
        });
    }
}
