package dev.henny.vibeyclient.gametest.parity;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.avatar.AvatarItem;
import dev.henny.vibeyclient.avatar.AvatarItems;
import dev.henny.vibeyclient.avatar.AvatarStates;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.Thumbnails;
import dev.henny.vibeyclient.avatar.Wardrobe;
import dev.henny.vibeyclient.avatar.fx.Effects;
import dev.henny.vibeyclient.avatar.fx.SpriteParticles;
import dev.henny.vibeyclient.avatar.preview.AvatarPreview;
import dev.henny.vibeyclient.avatar.render.GpuBundles;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

public final class OldEngineParity implements ParityEngine {
    @Override
    public String name() {
        return "old";
    }

    @Override
    public int itemCount() {
        return AvatarItems.all().size();
    }

    @Override
    public boolean has(String itemId) {
        AvatarItem i = AvatarItems.get(itemId);
        return i != null && i.bundle() != null;
    }

    @Override
    public void pin(double seconds) {
    }

    @Override
    public void tick(long ticks) {
        StaticFields.set(GpuBundles.class, "ticks", ticks - 1);
        StaticFields.set(dev.henny.vibeyclient.avatar.AvatarTicker.class, "tick", ticks - 1);
        StaticFields.set(dev.henny.vibeyclient.avatar.AvatarTicker.class, "lastNanos", 0L);
    }

    @Override
    public void seed(long seed) {
        ((Random) StaticFields.get(SpriteParticles.class, "RANDOM")).setSeed(seed);
        ((RandomSource) StaticFields.get(Effects.class, "RANDOM")).setSeed(seed);
    }

    @Override
    public void equipSelf(Map<String, String> slots) {
        Wardrobe w = wardrobe();
        for (String id : slots.values()) w.equip(AvatarItems.get(id));
    }

    @Override
    public void clearSelf() {
        Wardrobe w = wardrobe();
        for (Slot s : Slot.all()) w.unequip(s);
    }

    @Override
    public void wear(UUID player, Map<String, String> slots) {
        AvatarStates.setLoadout(player, slots);
    }

    @Override
    public void track(UUID player) {
        AvatarStates.get(player);
    }

    @Override
    public void play(UUID player, String emoteId) {
        if (!AvatarStates.play(AvatarStates.get(player), emoteId)) throw new AssertionError("emote not playable: " + emoteId);
    }

    @Override
    public void stop(UUID player) {
        AvatarStates.get(player).emote = null;
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
            views[i] = new OldPreviewView(v);
        }
        return new ParityPreviewScreen(views);
    }

    @Override
    public void previewEmote(Screen previews, String emoteId) {
        for (ParityPreviewScreen.View v : ((ParityPreviewScreen) previews).views) ((OldPreviewView) v).view.playEmote(emoteId);
    }

    @Override
    public void previewOverride(Screen previews, String slot, String itemId) {
        for (ParityPreviewScreen.View v : ((ParityPreviewScreen) previews).views) ((OldPreviewView) v).view.override(Slot.of(slot), itemId);
    }

    @Override
    public Screen thumbnails(String[] itemIds) {
        return new ParityThumbScreen(itemIds.length, (g, i, x, y, size) -> Thumbnails.draw(g, AvatarItems.get(itemIds[i]), x, y, size));
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

    private record OldPreviewView(AvatarPreview.View view) implements ParityPreviewScreen.View {
        @Override
        public void draw(dev.henny.vibeyclient.ui.Gfx g, int x, int y, int w, int h, float delta) {
            view.draw(g, x, y, w, h, delta);
        }
    }
}
