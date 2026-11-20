package dev.henny.vibeyclient.gametest;

import dev.henny.cosmetico.preview.MouseAction;

import dev.henny.vibeyclient.api.GlobalSettings;
import dev.henny.vibeyclient.api.Services;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.registry.ItemInfo;
import dev.henny.cosmetico.registry.ItemMeta;
import dev.henny.cosmetico.model.Kind;
import dev.henny.cosmetico.model.Text;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.registry.ItemSource;
import dev.henny.cosmetico.model.Rarity;
import dev.henny.cosmetico.model.Slot;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.module.cosmetics.ui.WardrobeUi;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import dev.henny.vibeyclient.ui.VibeyScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Wardrobe / emote wheel UI with fake items and fake Wardrobe + AvatarPreview services (the real ones are restored
 * afterwards). Opens every category, hover try-on, equip by click, search, the wheel editor, the emote wheel (held
 * key simulated) at GUI scale 2 and 3, English and German, on the title screen and in a world.
 * Screenshots: versions/fabric-1.21.11/build/run/clientGameTest/screenshots/wardrobe_*.png
 */
public final class WardrobeUiGameTest implements FabricClientGameTest {
    private static final String NS = "vibeytest:";
    private static final int HEADER = 28;
    private static final int PAD = 10;

    private final FakeWardrobe wardrobe = new FakeWardrobe();
    private final FakePreview preview = new FakePreview();
    private final List<Item> items = new ArrayList<>();
    private Optional<Wardrobe> oldWardrobe = Optional.empty();
    private Optional<AvatarPreview> oldPreview = Optional.empty();
    private int oldScale;

    @Override
    public void runTest(ClientGameTestContext context) {
        context.getInput().resizeWindow(1280, 720);
        context.runOnClient(mc -> {
            oldScale = mc.options.guiScale().get();
            oldWardrobe = Services.get(Wardrobe.class);
            oldPreview = Services.get(AvatarPreview.class);
            Services.register(Wardrobe.class, wardrobe);
            Services.register(AvatarPreview.class, preview);
            WardrobeUi.register();
            GlobalSettings.language.set(GlobalSettings.Language.ENGLISH);
            addItems();
            wardrobe.equipped.put(Slot.CAPE, NS + "vibe_cape");
            wardrobe.equipped.put(Slot.PET, NS + "seal_buddy");
            wardrobe.wheel[0] = NS + "wave";
            wardrobe.wheel[1] = NS + "cheer";
            wardrobe.wheel[2] = NS + "vibe_dance";
            wardrobe.wheel[4] = NS + "sit";
            wardrobe.wheel[6] = NS + "seal_summon";
        });
        try {
            titleScreen(context);
            inWorld(context);
        } finally {
            context.setScreen(() -> null);
            context.runOnClient(mc -> {
                for (Item i : items) Cosmetico.items().remove(i.id(), i.source());
                Services.unregister(Wardrobe.class, wardrobe);
                Services.unregister(AvatarPreview.class, preview);
                oldWardrobe.ifPresent(w -> Services.register(Wardrobe.class, w));
                oldPreview.ifPresent(p -> Services.register(AvatarPreview.class, p));
                GlobalSettings.language.set(GlobalSettings.Language.AUTO);
            });
            setScale(context, oldScale);
            context.getInput().resizeWindow(1280, 720);
        }
        System.out.println("[vibeyclient-test] wardrobe ui ok");
    }

    // ============================================================================================ title screen

    private void titleScreen(ClientGameTestContext context) {
        setScale(context, 2);
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWardrobe(mc.screen));
        context.waitTicks(6);
        String[] cats = {"cape", "wings", "aura", "pet", "shield", "hat", "back", "emotes"};
        for (int i = 0; i < cats.length; i++) {
            clickGui(context, 30, HEADER + 1 + 20 + i * rowH(context) + 5);
            context.waitTicks(5);
            if (i == cats.length - 1) {
                // select an emote card for the preview
                clickGui(context, firstCardX(context) + 20, firstCardY() + 20);
                context.waitTicks(4);
                if (!(NS + "wave").equals(preview.view.emote)) throw new AssertionError("Clicking an emote should preview it, got " + preview.view.emote);
            }
            context.takeScreenshot("wardrobe_s2_en_" + (i + 1) + "_" + cats[i]);
        }

        // capes: hover try-on, then click equips
        clickGui(context, 30, HEADER + 1 + 20 + 5);
        context.waitTicks(4);
        int cx = firstCardX(context) + 20, cy = firstCardY() + 20;
        int secondX = cx + cardStep(context);
        moveGui(context, secondX, cy);
        context.waitTicks(4);
        context.takeScreenshot("wardrobe_s2_en_9_hover_tryon");
        String tried = preview.view.overrides.get(Slot.CAPE);
        if (tried == null) throw new AssertionError("Hovering a cape should try it on");
        clickGui(context, secondX, cy);
        context.waitTicks(4);
        if (!tried.equals(wardrobe.equipped.get(Slot.CAPE))) throw new AssertionError("Click should equip " + tried + ", equipped " + wardrobe.equipped.get(Slot.CAPE));
        clickGui(context, secondX, cy);
        context.waitTicks(4);
        if (wardrobe.equipped.get(Slot.CAPE) != null) throw new AssertionError("Second click should unequip");
        wardrobe.equipped.put(Slot.CAPE, NS + "vibe_cape");

        // search
        int w = guiWidth(context);
        int sw = Math.max(80, Math.min(180, w / 4));
        clickGui(context, w - 24 - 8 - sw + 30, HEADER / 2);
        context.waitTicks(2);
        context.getInput().typeChars("seal");
        context.waitTicks(4);
        context.takeScreenshot("wardrobe_s2_en_10_search_seal");
        context.getInput().typeChars("zzz");
        context.waitTicks(4);
        context.takeScreenshot("wardrobe_s2_en_11_search_empty");

        // wheel editor, emote wheel
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWheelEditor(null));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s2_en_12_wheel_editor");
        wheel(context, "wardrobe_s2_en_13_wheel", 2);

        // Vibey menu tab
        context.setScreen(() -> new VibeyScreen(null, WardrobeUi.TAB_ID));
        context.waitTicks(8);
        context.takeScreenshot("wardrobe_s2_en_14_menu_tab");

        // GUI scale 3 (426 x 240), German
        setScale(context, 3);
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.GERMAN));
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWardrobe(null, Slot.CAPE));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s3_de_1_cape");
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWardrobeEmotes(null));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s3_de_2_emotes");
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWheelEditor(null));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s3_de_3_wheel_editor");
        wheel(context, "wardrobe_s3_de_4_wheel", 3);
        context.setScreen(() -> new VibeyScreen(null, WardrobeUi.TAB_ID));
        context.waitTicks(8);
        context.takeScreenshot("wardrobe_s3_de_5_menu_tab");

        // English at scale 3
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWardrobe(null, Slot.PET));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s3_en_1_pet");

        // no services, no items: empty states
        context.runOnClient(mc -> {
            Services.unregister(Wardrobe.class, wardrobe);
            Services.unregister(AvatarPreview.class, preview);
            for (Item i : items) Cosmetico.items().remove(i.id(), i.source());
        });
        context.waitTicks(4);
        context.takeScreenshot("wardrobe_s3_en_2_empty_no_services");
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWheel(() -> true));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_s3_en_3_wheel_no_services");
        context.setScreen(() -> new VibeyScreen(null, WardrobeUi.TAB_ID));
        context.waitTicks(8);
        context.takeScreenshot("wardrobe_s3_en_4_menu_tab_no_services");
        context.setScreen(() -> null);
        context.runOnClient(mc -> {
            Services.register(Wardrobe.class, wardrobe);
            Services.register(AvatarPreview.class, preview);
            for (Item i : items) Cosmetico.items().put(i);
        });

        // big window (4K at scale 4 = 960 x 540) and small (854 x 480 at scale 2)
        context.getInput().resizeWindow(3840, 2160);
        setScale(context, 4);
        context.runOnClient(mc -> WardrobeUi.openWardrobe(null, Slot.CAPE));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_4k_s4_cape");
        context.setScreen(() -> null);
        context.getInput().resizeWindow(854, 480);
        setScale(context, 2);
        context.runOnClient(mc -> WardrobeUi.openWardrobeEmotes(null));
        context.waitTicks(6);
        context.takeScreenshot("wardrobe_854_s2_emotes");
        context.setScreen(() -> null);
        context.getInput().resizeWindow(1280, 720);
    }

    /** Opens the wheel with a simulated held key, points up-right (slot 1), screenshots, releases → plays slot 1. */
    private void wheel(ClientGameTestContext context, String shot, int scale) {
        AtomicBoolean held = new AtomicBoolean(true);
        wardrobe.played = null;
        context.setScreen(() -> null);
        context.runOnClient(mc -> WardrobeUi.openWheel(held::get));
        context.waitTicks(2);
        int w = guiWidth(context), h = guiHeight(context);
        moveGui(context, w / 2 + 70, h / 2 - 14 - 70);
        context.waitTicks(8);
        context.takeScreenshot(shot);
        held.set(false);
        context.waitTicks(3);
        if (!(NS + "cheer").equals(wardrobe.played)) throw new AssertionError("Releasing the wheel key on slot 1 should play cheer, played " + wardrobe.played);
        boolean closed = context.computeOnClient(mc -> !WardrobeUi.isOpen());
        if (!closed) throw new AssertionError("Wheel should close after releasing the key");
    }

    // ============================================================================================ world

    private void inWorld(ClientGameTestContext context) {
        setScale(context, 2);
        context.runOnClient(mc -> GlobalSettings.language.set(GlobalSettings.Language.ENGLISH));
        try (TestSingleplayerContext world = context.worldBuilder().create()) {
            world.getClientWorld().waitForChunksRender();
            context.runOnClient(mc -> WardrobeUi.openWardrobe(null, Slot.WINGS));
            context.waitTicks(6);
            boolean paused = context.computeOnClient(mc -> mc.screen != null && mc.screen.isPauseScreen());
            if (paused) throw new AssertionError("Wardrobe opened from the game should not pause");
            context.takeScreenshot("wardrobe_world_s2_wings");
            context.setScreen(() -> null);
            context.waitTicks(2);

            AtomicBoolean held = new AtomicBoolean(true);
            context.runOnClient(mc -> WardrobeUi.openWheel(held::get));
            context.waitTicks(2);
            boolean wheelPause = context.computeOnClient(mc -> mc.screen != null && mc.screen.isPauseScreen());
            if (wheelPause) throw new AssertionError("Emote wheel must not pause");
            int w = guiWidth(context), h = guiHeight(context);
            moveGui(context, w / 2 - 80, h / 2 - 14); // left = slot 6
            context.waitTicks(8);
            context.takeScreenshot("wardrobe_world_s2_wheel_left");
            context.getInput().pressMouse(1); // right click cancels
            context.waitTicks(3);
            boolean closed = context.computeOnClient(mc -> !WardrobeUi.isOpen());
            if (!closed) throw new AssertionError("Right click should close the wheel");

            context.setScreen(() -> new VibeyScreen(null, WardrobeUi.TAB_ID));
            context.waitTicks(8);
            context.takeScreenshot("wardrobe_world_s2_menu_tab");
            context.setScreen(() -> null);
        }
    }

    // ============================================================================================ helpers

    private static void setScale(ClientGameTestContext context, int scale) {
        context.runOnClient(mc -> {
            mc.options.guiScale().set(scale);
            mc.resizeDisplay();
        });
        context.waitTicks(1);
    }

    private static int guiWidth(ClientGameTestContext context) {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScaledWidth());
    }

    private static int guiHeight(ClientGameTestContext context) {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScaledHeight());
    }

    private static double scale(ClientGameTestContext context) {
        return context.computeOnClient(mc -> mc.getWindow().getGuiScale());
    }

    private static void moveGui(ClientGameTestContext context, double x, double y) {
        double s = scale(context);
        context.getInput().setCursorPos(x * s, y * s);
    }

    private static void clickGui(ClientGameTestContext context, double x, double y) {
        moveGui(context, x, y);
        context.waitTicks(1);
        context.getInput().pressMouse(0);
    }

    /** Same formulas as the wardrobe screen. */
    private static int listW(ClientGameTestContext context) {
        return Math.max(84, Math.min(140, guiWidth(context) / 6));
    }

    private static int rowH(ClientGameTestContext context) {
        int bodyH = guiHeight(context) - HEADER - 1;
        return Math.max(14, Math.min(22, (bodyH - 20 - 18 - PAD * 2) / 8));
    }

    private static int firstCardX(ClientGameTestContext context) {
        return listW(context) + 1 + PAD;
    }

    private static int firstCardY() {
        return HEADER + 1 + 28 + 1;
    }

    private static int cardStep(ClientGameTestContext context) {
        int w = guiWidth(context);
        int prevW = Math.max(110, Math.min(300, w * 3 / 10));
        int gridW = w - listW(context) - 1 - prevW - 1;
        int innerW = gridW - PAD * 2 + 4 - 8;
        int cols = Math.max(1, (innerW + 6) / (70 + 6));
        int cardW = (innerW - 6 * (cols - 1)) / cols;
        if (cardW > 104) {
            cols = (innerW + 6 + 104 + 6 - 1) / (104 + 6);
            cardW = (innerW - 6 * (cols - 1)) / cols;
        }
        return cardW + 6;
    }

    // ============================================================================================ fake data

    private void addItems() {
        cosmetic("vibe_cape", Slot.CAPE, "Vibe Cape", "Vibe-Umhang", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("aurora_cape", Slot.CAPE, "Aurora Cape", "Polarlicht-Umhang", Rarity.EPIC, ItemSource.BUILTIN);
        cosmetic("pixel_stars_cape", Slot.CAPE, "Pixel Stars Cape", "Pixelsterne-Umhang", Rarity.RARE, ItemSource.DEV);
        cosmetic("ice_floe_cape", Slot.CAPE, "Ice Floe Cape with a very long name", "Eisscholle-Umhang mit langem Namen", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("founder_cape", Slot.CAPE, "Founder Cape", "Gründer-Umhang", Rarity.LEGENDARY, ItemSource.REMOTE);
        cosmetic("crystal_wings", Slot.WINGS, "Crystal Ice Wings", "Kristalleisflügel", Rarity.LEGENDARY, ItemSource.BUILTIN);
        cosmetic("neon_wings", Slot.WINGS, "Neon Pixel Wings", "Neon-Pixelflügel", Rarity.EPIC, ItemSource.BUILTIN);
        cosmetic("snowflake_orbit", Slot.AURA, "Snowflake Orbit", "Schneeflocken-Orbit", Rarity.EPIC, ItemSource.BUILTIN);
        cosmetic("ember_sparks", Slot.AURA, "Ember Sparks", "Glutfunken", Rarity.EPIC, ItemSource.BUILTIN);
        cosmetic("seal_buddy", Slot.PET, "Seal Buddy", "Robbenkumpel", Rarity.EPIC, ItemSource.BUILTIN);
        cosmetic("pixel_penguin", Slot.PET, "Pixel Penguin", "Pixel-Pinguin", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("seal_crest_shield", Slot.SHIELD, "Seal Crest Shield", "Robbenwappen-Schild", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("vibe_chevron_shield", Slot.SHIELD, "Vibe Chevron Shield", "Vibe-Sparren-Schild", Rarity.COMMON, ItemSource.BUILTIN);
        cosmetic("glacier_shield", Slot.SHIELD, "Glacier Shield", "Gletscherschild", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("seal_beanie", Slot.HAT, "Seal Beanie", "Robbenmütze", Rarity.RARE, ItemSource.BUILTIN);
        cosmetic("ice_crown", Slot.HAT, "Ice Crown", "Eiskrone", Rarity.EPIC, ItemSource.BUILTIN);
        // no back item: empty category state
        String[][] emotes = {
                {"wave", "Wave", "Winken"}, {"clap", "Clap", "Klatschen"}, {"cheer", "Cheer", "Jubeln"},
                {"vibe_dance", "Vibe Dance", "Vibe-Tanz"}, {"facepalm", "Facepalm", "Facepalm"}, {"sit", "Sit", "Hinsetzen"},
                {"seal_summon", "Seal Summon", "Robben-Beschwörung"}, {"guitar", "Guitar Jam", "Gitarrensolo"},
                {"seal_flop", "Seal Flop", "Robben-Plumps"}, {"shrug", "Shrug", "Schulterzucken"}};
        for (int i = 0; i < emotes.length; i++) {
            Rarity r = emotes[i][0].equals("seal_summon") ? Rarity.EPIC : emotes[i][0].startsWith("seal") || emotes[i][0].equals("guitar") ? Rarity.RARE : Rarity.COMMON;
            add(item(NS + emotes[i][0], Kind.EMOTE, null, new Text(emotes[i][1], emotes[i][2]), List.of("emote"), r, ItemSource.BUILTIN, i));
        }
        wardrobe.locked.add(NS + "founder_cape");
    }

    private void cosmetic(String path, Slot slot, String en, String de, Rarity r, ItemSource src) {
        add(item(NS + path, Kind.COSMETIC, slot, new Text(en, de), List.of("seal"), r, src, items.size()));
    }

    private static Item item(String id, Kind kind, Slot slot, Text name, List<String> tags, Rarity r, ItemSource src, int sort) {
        ItemInfo info = new ItemInfo(id, kind, slot, name, Text.EMPTY, "Vibe Craft Studios", tags, r);
        return new Item(info, src, ItemMeta.of(sort, null, null, "default"), null, Cosmetico.items().nextRevision());
    }

    private void add(Item item) {
        items.add(item);
        Cosmetico.items().put(item);
    }

    // ============================================================================================ fakes

    private static final class FakeWardrobe implements Wardrobe {
        final Map<Slot, String> equipped = new EnumMap<>(Slot.class);
        final String[] wheel = new String[WHEEL_SLOTS];
        final java.util.Set<String> locked = new java.util.HashSet<>();
        volatile String played;

        @Override public String equipped(Slot slot) { return equipped.get(slot); }
        @Override public void equip(Item item) { if (!item.isEmote() && canUse(item)) equipped.put(item.slot(), item.id()); }
        @Override public void unequip(Slot slot) { equipped.remove(slot); }
        @Override public boolean canUse(Item item) { return !locked.contains(item.id()); }
        @Override public String lockedReason(Item item) { return canUse(item) ? null : "Not unlocked"; }
        @Override public String wheel(int index) { return wheel[index]; }
        @Override public void setWheel(int index, String emoteId) { wheel[index] = emoteId; }
        @Override public void playEmote(String emoteId) { played = emoteId; }
        @Override public void stopEmote() {}
        @Override public String playingEmote() { return null; }
    }

    private static final class FakePreview implements AvatarPreview {
        final FakeView view = new FakeView();

        @Override
        public View create() {
            view.overrides.clear();
            view.emote = null;
            return view;
        }
    }

    /** Draws a flat blocky figure; shows yaw, try-on overrides and the emote as text. */
    private static final class FakeView implements AvatarPreview.View {
        final Map<Slot, String> overrides = new EnumMap<>(Slot.class);
        volatile String emote;
        float yaw = -20f;
        int lx, ly, lw, lh;
        double dragX;
        boolean drag;

        @Override
        public void draw(Gfx g, int x, int y, int w, int h, float delta) {
            lx = x;
            ly = y;
            lw = w;
            lh = h;
            int u = Math.max(2, Math.min(w / 12, h / 36));
            int cx = x + w / 2;
            int feet = y + h - u * 3;
            int body = Theme.BRAND2;
            // legs, body, arms, head
            g.rect(cx - 2 * u, feet - 12 * u, 2 * u, 12 * u, 0xFF3A4A8A);
            g.rect(cx, feet - 12 * u, 2 * u, 12 * u, 0xFF34437D);
            g.rect(cx - 2 * u, feet - 24 * u, 4 * u, 12 * u, body);
            g.rect(cx - 4 * u, feet - 24 * u, 2 * u, 12 * u, 0xFFC69C7C);
            g.rect(cx + 2 * u, feet - 24 * u, 2 * u, 12 * u, 0xFFC69C7C);
            g.rect(cx - 4 * u, feet - 32 * u, 8 * u, 8 * u, 0xFFD8AE8C);
            String cape = overrides.get(Slot.CAPE);
            if (cape != null) g.rect(cx - 3 * u, feet - 23 * u, 6 * u, 16 * u, Theme.alpha(Theme.BRAND, 0.6f));
            int ty = y + 4;
            g.text("FAKE PREVIEW yaw " + Math.round(yaw), x + 4, ty, Theme.DIM);
            for (Map.Entry<Slot, String> e : overrides.entrySet()) {
                ty += 10;
                g.text(g.clip("try " + e.getValue(), w - 8), x + 4, ty, Theme.BRAND2);
            }
            if (emote != null) g.text(g.clip("emote " + emote, w - 8), x + 4, ty + 10, Theme.WARN);
        }

        @Override public float yaw() { return yaw; }
        @Override public void setYaw(float degrees) { yaw = degrees; }
        @Override public void setAutoRotate(boolean on) {}

        @Override
        public boolean mouse(MouseAction action, double x, double y) {
            switch (action) {
                case PRESS -> {
                    if (x >= lx && x < lx + lw && y >= ly && y < ly + lh) {
                        drag = true;
                        dragX = x;
                        return true;
                    }
                    return false;
                }
                case DRAG -> {
                    if (!drag) return false;
                    yaw += (float) (x - dragX) * 2f;
                    dragX = x;
                    return true;
                }
                default -> {
                    boolean was = drag;
                    drag = false;
                    return was;
                }
            }
        }

        @Override public void playEmote(String emoteId) { emote = emoteId; }
        @Override public void stopEmote() { emote = null; }
        @Override public String emote() { return emote; }
        @Override public void override(Slot slot, String itemId) { overrides.put(slot, itemId); }
        @Override public void clearOverrides() { overrides.clear(); }
    }
}
