package dev.henny.cosmetico.mc.thumbs;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.Engine;
import dev.henny.cosmetico.host.Workers;
import dev.henny.cosmetico.mc.render.Tex;
import dev.henny.cosmetico.registry.Item;
import dev.henny.cosmetico.thumbs.ThumbnailStore;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;

public final class McThumbnails {
    private McThumbnails() {}

    private static ThumbnailStore<Tex> store;

    public static void anchor(Class<?> resourceOwner) {
        ResourceAnchors.add(resourceOwner);
    }

    //? if >=26.1 {
    /*public static boolean draw(GuiGraphicsExtractor g, Item item, int x, int y, int size) {
    *///?} else
    public static boolean draw(GuiGraphics g, Item item, int x, int y, int size) {
        if (item == null) return false;
        Tex t = store().get(item);
        if (t == null) return false;
        GuiBlit.blit(g, t.id, x, y, size, size, t.width, t.height);
        return true;
    }

    private static ThumbnailStore<Tex> store() {
        if (store == null) {
            Engine e = Cosmetico.engine();
            store = new ThumbnailStore<>(e.items, e.devThumbnails, new TexThumbnails(), ResourceAnchors::read,
                    () -> Cosmetico.host().thumbnails(), Workers.create("cosmetico-thumbnails", 1), () -> Cosmetico.host().log());
        }
        return store;
    }
}
