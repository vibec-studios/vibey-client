package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionEventKind;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.R3Mc;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.vui.VuiPanelContent;

final class ShotsPanel {
    static final String DOCUMENT = "vibeyclient:clips/shots-panel";
    static final float WIDTH = 3.2f, HEIGHT = 2f, PIXELS_PER_BLOCK = 120f;

    private static WorldObject panel;
    private static boolean listening;

    private ShotsPanel() {}

    static WorldObject place(GalleryController controller) {
        remove();
        listen();
        VuiPanelContent content = new VuiPanelContent(R3Mc.vui().open(DOCUMENT, controller), ShotsPanel::texture);
        WorldObject o = WorldObject.create(R3Mc.PANEL_KIND).content(content).rules(InteractionRules.screen())
                .dimension(R3Mc.scene().query().dimension());
        o.transform().size(WIDTH, HEIGHT);
        o.pixelsPerBlock(PIXELS_PER_BLOCK);
        panel = o;
        R3Mc.place(o);
        return o;
    }

    static WorldObject object() {
        return alive() ? panel : null;
    }

    static boolean alive() {
        return panel != null && R3Client.started() && R3Mc.scene().get(panel.id()) != null;
    }

    static void forget() {
        panel = null;
    }

    static void remove() {
        WorldObject o = panel;
        panel = null;
        if (o != null && R3Client.started()) R3Mc.remove(o);
    }

    private static TextureHandle texture(String id) {
        TextureHandle h = ShotTextures.handle(id);
        return h != null ? h : R3Client.get().vui().textures().resolve(id);
    }

    private static void listen() {
        if (listening) return;
        listening = true;
        R3Mc.scene().interaction().listen(ShotsPanel::event);
    }

    private static void event(InteractionEvent e) {
        if (panel == null || e.target() != panel) return;
        if (e.kind() == InteractionEventKind.CONTEXT_MENU || e.kind() == InteractionEventKind.PLACEMENT_CANCELLED) {
            Shots.removePanel();
        }
    }
}
