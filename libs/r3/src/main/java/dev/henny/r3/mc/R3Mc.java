package dev.henny.r3.mc;

import dev.henny.r3.anchor.Facing;
import dev.henny.r3.interact.InteractionRules;
import dev.henny.r3.mc.client.R3Client;
import dev.henny.r3.mc.client.WorldListener;
import dev.henny.r3.mc.store.R3Storage;
import dev.henny.r3.mc.texture.DynamicTextureHandle;
import dev.henny.r3.mc.texture.ExternalTextures;
import dev.henny.r3.render.TextureHandle;
import dev.henny.r3.object.WorldObject;
import dev.henny.r3.persist.PlacementStore;
import dev.henny.r3.scene.R3Scene;
import dev.henny.r3.text.Text3D;
import dev.henny.r3.vui.VuiPanelContent;
import dev.henny.vui.Vui;

public final class R3Mc {
    public static final String PANEL_KIND = "r3:panel";
    public static final String TEXT_KIND = "r3:text";
    public static final float DEFAULT_PANEL_WIDTH = 2f;
    public static final float DEFAULT_PANEL_HEIGHT = 1.125f;

    private R3Mc() {}

    public static R3Scene scene() {
        return R3Client.get().scene();
    }

    public static Vui vui() {
        return R3Client.get().vui().vui();
    }

    public static VuiPanelContent vuiContent(String vuiDocumentId, Object controller) {
        return VuiPanelContent.open(vui(), vuiDocumentId, controller, R3Client.get().vui().textures());
    }

    public static WorldObject panel(String vuiDocumentId, Object controller) {
        VuiPanelContent content = vuiContent(vuiDocumentId, controller);
        WorldObject panel = WorldObject.create(PANEL_KIND).content(content).rules(InteractionRules.screen());
        panel.transform().size(DEFAULT_PANEL_WIDTH, DEFAULT_PANEL_HEIGHT);
        return panel.dimension(scene().query().dimension());
    }

    public static WorldObject text(String text) {
        WorldObject o = WorldObject.create(TEXT_KIND).content(new Text3D(text).facing(Facing.CAMERA));
        return o.rules(new InteractionRules().interactive(false)).dimension(scene().query().dimension());
    }

    public static WorldObject add(WorldObject object) {
        return scene().add(object);
    }

    public static WorldObject place(WorldObject object) {
        scene().add(object);
        scene().interaction().beginPlacement(object);
        return object;
    }

    public static void remove(WorldObject object) {
        WorldObject removed = scene().remove(object.id());
        if (removed != null && removed.content() instanceof VuiPanelContent panel) panel.close();
    }

    public static DynamicTextureHandle dynamicTexture(String name, int width, int height) {
        return new DynamicTextureHandle(name, width, height);
    }

    public static void texture(String id, TextureHandle handle) {
        ExternalTextures.register(id, handle);
    }

    public static R3Storage storage() {
        return R3Client.get().storage();
    }

    public static PlacementStore placements() {
        return storage().placements();
    }

    public static String worldKey() {
        return R3Client.get().worldKey();
    }

    public static void onWorld(WorldListener listener) {
        R3Client.get().listen(listener);
    }
}
