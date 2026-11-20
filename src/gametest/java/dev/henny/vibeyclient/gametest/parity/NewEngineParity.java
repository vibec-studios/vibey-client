package dev.henny.vibeyclient.gametest.parity;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.mc.fx.Sprites;
import dev.henny.cosmetico.mc.fx.VanillaFx;
import dev.henny.cosmetico.mc.render.GpuCache;
import dev.henny.cosmetico.mc.thumbs.McThumbnails;
import dev.henny.cosmetico.mc.tick.McTicker;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import net.minecraft.client.gui.screens.Screen;

import java.util.Map;
import java.util.UUID;

public final class NewEngineParity implements ParityEngine {
    @Override
    public String name() {
        return "new";
    }

    @Override
    public int itemCount() {
        return Cosmetico.items().all().size();
    }

    @Override
    public boolean has(String itemId) {
        Item i = Cosmetico.items().get(itemId);
        return i != null && i.bundle() != null;
    }

    @Override
    public void pin(double seconds) {
        Cosmetico.engine().clock.pin(seconds);
    }

    @Override
    public void tick(long ticks) {
        StaticFields.set(McTicker.class, "tick", ticks - 1);
        StaticFields.set(McTicker.class, "lastNanos", 0L);
        StaticFields.set(GpuCache.class, "ticks", ticks - 1);
    }

    @Override
    public void seed(long seed) {
        Sprites.random().setSeed(seed);
        VanillaFx.random().setSeed(seed);
    }

    @Override
    public void equipSelf(Map<String, String> slots) {
        Wardrobe w = wardrobe();
        for (String id : slots.values()) w.equip(Cosmetico.items().get(id));
    }

    @Override
    public void clearSelf() {
        Wardrobe w = wardrobe();
        for (Slot s : Slot.all()) w.unequip(s);
    }

    @Override
    public void wear(UUID player, Map<String, String> slots) {
        Cosmetico.loadouts().setIds(player, slots);
    }

    @Override
    public void track(UUID player) {
        Cosmetico.engine().avatars.get(player);
    }

    @Override
    public void play(UUID player, String emoteId) {
        if (!Cosmetico.engine().emotes.play(Cosmetico.engine().avatars.get(player), emoteId)) throw new AssertionError("emote not playable: " + emoteId);
    }

    @Override
    public void stop(UUID player) {
        Avatar a = Cosmetico.engine().avatars.peek(player);
        if (a != null) a.emote = null;
    }

    @Override
    public void playSelf(String emoteId) {
        wardrobe().playEmote(emoteId);
    }

    @Override
    public Screen previews(float[] yaws) {
        AvatarPreview service = Services.get(AvatarPreview.class).orElseThrow();
        ParityPreviewScreen.View[] views = new ParityPreviewScreen.View[yaws.length];
        for (int i = 0; i < yaws.length; i++) {
            AvatarPreview.View v = service.create();
            v.setYaw(yaws[i]);
            views[i] = new NewView(v);
        }
        return new ParityPreviewScreen(views);
    }

    @Override
    public void previewEmote(Screen previews, String emoteId) {
        for (ParityPreviewScreen.View v : ((ParityPreviewScreen) previews).views) ((NewView) v).view.playEmote(emoteId);
    }

    @Override
    public void previewOverride(Screen previews, String slot, String itemId) {
        for (ParityPreviewScreen.View v : ((ParityPreviewScreen) previews).views) ((NewView) v).view.override(Slot.of(slot), itemId);
    }

    @Override
    public Screen thumbnails(String[] itemIds) {
        return new ParityThumbScreen(itemIds.length, (g, i, x, y, size) -> McThumbnails.draw(g.g, Cosmetico.items().get(itemIds[i]), x, y, size));
    }

    @Override
    public void openWardrobe(String slot) {
        WardrobeUi.openWardrobe(null, Slot.of(slot));
    }

    @Override
    public void openWardrobeEmotes() {
        WardrobeUi.openWardrobeEmotes(null);
    }

    private static Wardrobe wardrobe() {
        return Services.get(Wardrobe.class).orElseThrow();
    }

    private record NewView(AvatarPreview.View view) implements ParityPreviewScreen.View {
        @Override
        public void draw(dev.henny.vibeyclient.ui.Gfx g, int x, int y, int w, int h, float delta) {
            view.draw(g, x, y, w, h, delta);
        }
    }
}
