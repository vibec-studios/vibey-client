package dev.henny.vibeyclient.gametest.parity;

import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.screens.Screen;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ParityTitle {
    private static final String[] THUMBS = {
            "vibey:vibe_cape", "vibey:crystal_wings", "vibey:seal_beanie", "vibey:seal_buddy",
            "vibey:ice_crown", "vibey:glacier_shield", "vibey:wave", "vibey:guitar"};
    private final ClientGameTestContext context;
    private final ParityEngine engine;
    private final ParityShots shots;

    public ParityTitle(ClientGameTestContext context, ParityEngine engine, ParityShots shots) {
        this.context = context;
        this.engine = engine;
        this.shots = shots;
    }

    public void run() {
        Map<String, String> self = new LinkedHashMap<>();
        self.put("cape", "vibey:vibe_cape");
        self.put("wings", "vibey:crystal_wings");
        self.put("hat", "vibey:seal_beanie");
        self.put("pet", "vibey:seal_buddy");
        context.runOnClient(mc -> {
            ParityClock.start(engine, 0);
            engine.seed(7);
            engine.equipSelf(self);
        });
        Screen[] previews = new Screen[1];
        context.setScreen(() -> previews[0] = engine.previews(new float[]{0f, 90f, 180f}));
        context.waitTicks(20);
        shots.take("p01_preview_title");
        context.runOnClient(mc -> {
            ParityClock.pin();
            engine.previewEmote(previews[0], "vibey:guitar");
        });
        context.waitTicks(40);
        shots.take("p02_preview_guitar");
        context.runOnClient(mc -> {
            ParityClock.pin();
            engine.previewOverride(previews[0], "wings", "vibey:neon_wings");
            engine.previewOverride(previews[0], "pet", "vibey:pixel_penguin");
            engine.previewEmote(previews[0], "vibey:wave");
        });
        context.waitTicks(40);
        shots.take("p03_preview_override_wave");
        context.setScreen(() -> engine.thumbnails(THUMBS));
        context.waitTicks(40);
        shots.take("t01_thumbnails");
        context.runOnClient(mc -> engine.openWardrobe("wings"));
        context.waitTicks(30);
        shots.take("w01_wardrobe_wings");
        context.runOnClient(mc -> engine.openWardrobeEmotes());
        context.waitTicks(20);
        shots.take("w02_wardrobe_emotes");
        context.setScreen(() -> null);
        context.runOnClient(mc -> engine.clearSelf());
    }
}
