package dev.henny.vibeyclient.module.media.lyrics.world;

import dev.henny.r3.anchor.FreeAnchor;
import dev.henny.r3.anchor.SoftFollowAnchor;
import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.R3Mod;
import dev.henny.r3.mc.client.WorldListener;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementCodec;
import dev.henny.r3.persist.PlacementRecord;
import dev.henny.r3.vui.VuiPanelContent;

import java.util.List;

public final class MusicPanel {
    public static final String KIND = "vibeyclient:music";
    public static final String DOCUMENT = "vibeyclient:music/panel";
    public static final float WIDTH = 1.5f;
    public static final float HEIGHT = 0.5f;
    public static final float PIXELS_PER_BLOCK = 128f;
    public static final double PINNED_DISTANCE = 1.4;
    public static final float PINNED_ELEVATION = -16f;

    private static final MusicControls controls = new MusicControls();
    private static WorldObject panel;
    private static boolean hooked;

    private MusicPanel() {}

    public static void hook() {
        if (hooked) return;
        hooked = true;
        R3Mod.whenReady(() -> {
            R3Mc.texture(CoverTexture.ID, new CoverTexture());
            R3Mc.scene().interaction().listen(MusicPanel::event);
            R3Mc.onWorld(new WorldListener() {
                @Override
                public void worldJoined(String worldKey) {
                    restore();
                }

                @Override
                public void worldLeft(String worldKey) {
                    save();
                    if (panel != null) R3Mc.remove(panel);
                    panel = null;
                }
            });
        });
    }

    public static WorldObject panel() {
        return panel;
    }

    public static MusicControls controls() {
        return controls;
    }

    public static void open() {
        if (R3Mc.worldKey() == null) return;
        if (panel != null && R3Mc.scene().get(panel.id()) != null) {
            R3Mc.scene().interaction().beginMove(panel);
            return;
        }
        panel = build(WorldObject.create(KIND));
        R3Mc.place(panel);
    }

    public static void close() {
        if (panel == null) return;
        R3Mc.remove(panel);
        panel = null;
        save();
    }

    public static void togglePinned() {
        if (panel == null || panel.lockedByUser()) return;
        boolean pinned = panel.anchor() instanceof SoftFollowAnchor;
        panel.anchor(pinned ? FreeAnchor.INSTANCE : SoftFollowAnchor.soft(PINNED_DISTANCE, PINNED_ELEVATION));
        save();
    }

    public static void tick() {
        if (panel != null) controls.refresh();
    }

    private static WorldObject build(WorldObject o) {
        VuiPanelContent content = R3Mc.vuiContent(DOCUMENT, controls);
        o.content(content).rules(new InteractionRules()).dimension(R3Mc.scene().query().dimension());
        if (o.transform().width() == 1f && o.transform().height() == 1f) o.transform().size(WIDTH, HEIGHT);
        o.pixelsPerBlock(PIXELS_PER_BLOCK);
        controls.refresh();
        return o;
    }

    private static void event(InteractionEvent e) {
        if (panel == null || e.target() != panel) return;
        switch (e.kind()) {
            case PLACED, MOVED, RESIZED, UNDONE -> save();
            case PLACEMENT_CANCELLED -> close();
            case CONTEXT_MENU -> togglePinned();
            default -> {
            }
        }
    }

    private static void restore() {
        String key = R3Mc.worldKey();
        List<PlacementRecord> saved = key == null ? List.of() : R3Mc.placements().list(key, KIND);
        if (saved.isEmpty()) return;
        try {
            panel = build(PlacementCodec.decode(saved.get(0), InteractionRules.screen().limits()));
            R3Mc.add(panel);
        } catch (RuntimeException e) {
            panel = null;
        }
    }

    private static void save() {
        String key = R3Mc.worldKey();
        if (key == null) return;
        boolean here = panel != null && R3Mc.scene().get(panel.id()) != null;
        R3Mc.placements().replaceAll(key, KIND, here ? List.of(PlacementCodec.encode(panel, null)) : List.of());
        R3Mc.storage().saveWorld();
    }
}
