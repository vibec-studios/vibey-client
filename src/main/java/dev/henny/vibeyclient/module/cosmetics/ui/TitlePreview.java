package dev.henny.vibeyclient.module.cosmetics.ui;

import dev.henny.cosmetico.preview.MouseAction;

import dev.henny.vibeyclient.api.Services;
import dev.henny.vibeyclient.api.service.MenuPreview;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.Cosmetico;
import dev.henny.vibeyclient.module.cosmetics.Wardrobe;
import dev.henny.vibeyclient.module.cosmetics.preview.AvatarPreview;
import dev.henny.vibeyclient.ui.Gfx;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * The player on the right side of the Vibey title screen ({@link MenuPreview}, registered by the cosmetics module
 * while it is enabled): the local player with the equipped cosmetics (their idle animations run in the preview), drag
 * to rotate, and now and then one of the player's emotes (wheel emotes first, else any usable one). Without the
 * avatar preview service the {@link PreviewHost} placeholder is drawn.
 */
public final class TitlePreview implements MenuPreview {
    /** Start angle: a little turned, so capes and wings are visible at a glance. */
    static final float START_YAW = -24f;
    private static final long FIRST_EMOTE_MS = 7_000, EMOTE_MIN_MS = 16_000, EMOTE_MAX_MS = 28_000;

    private final PreviewHost host = new PreviewHost();
    private boolean started;
    private long nextEmoteAt;
    private String lastEmote;
    private final List<String> candidates = new ArrayList<>();

    @Override
    public void render(Gfx g, int x, int y, int w, int h, int mouseX, int mouseY, float delta) {
        AvatarPreview.View v = host.view();
        if (v != null && !started) {
            started = true;
            v.setYaw(START_YAW);
        }
        long now = System.currentTimeMillis();
        if (v != null && nextEmoteAt > 0 && now >= nextEmoteAt) {
            if (host.emote() == null) {
                String id = pickEmote();
                if (id != null) host.playEmote(id);
            }
            nextEmoteAt = now + ThreadLocalRandom.current().nextLong(EMOTE_MIN_MS, EMOTE_MAX_MS);
        }
        // headroom for hats, crowns and wings above the head (the preview fits the player body into the box)
        int top = h / 8;
        host.draw(g, x, y + top, w, h - top, delta);
    }

    /** A usable emote, preferring the wheel; never the same twice in a row when there is a choice. */
    private String pickEmote() {
        Wardrobe wd = Services.get(Wardrobe.class).orElse(null);
        if (wd == null) return null;
        candidates.clear();
        for (int i = 0; i < Wardrobe.WHEEL_SLOTS; i++) {
            Item item = Cosmetico.items().get(wd.wheel(i));
            if (usable(wd, item)) candidates.add(item.id());
        }
        if (candidates.isEmpty()) {
            for (Item item : Cosmetico.items().emotes()) if (usable(wd, item)) candidates.add(item.id());
        }
        if (candidates.size() > 1) candidates.remove(lastEmote);
        if (candidates.isEmpty()) return null;
        lastEmote = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return lastEmote;
    }

    private static boolean usable(Wardrobe wd, Item item) {
        // looping emotes (sit, dance) would never end on the title screen
        return item != null && item.isEmote() && !item.isDev() && wd.canUse(item)
                && item.bundle() != null && (item.bundle().emote == null || !item.bundle().emote.loop());
    }

    @Override
    public boolean mouseClicked(double x, double y, int button) {
        return button == 0 && host.mouse(MouseAction.PRESS, x, y);
    }

    @Override
    public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return host.mouse(MouseAction.DRAG, x, y);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        return host.mouse(MouseAction.RELEASE, x, y);
    }

    @Override
    public void onShow(String place) {
        nextEmoteAt = System.currentTimeMillis() + FIRST_EMOTE_MS;
    }

    @Override
    public void onHide(String place) {
        nextEmoteAt = 0;
        host.stopEmote();
    }
}
