package dev.henny.r3.mc.vui;

import dev.henny.r3.mc.input.HeldInput;
import dev.henny.r3.mc.render.McFontMetrics;
import dev.henny.r3.mc.texture.McTextures;
import dev.henny.r3.vui.TextureResolver;
import dev.henny.vui.Vui;
import dev.henny.vui.reload.ClasspathRoot;
import dev.henny.vui.render.Backend;

public final class WorldVui {
    private final Vui vui;
    private final TextureResolver textures = McTextures::resource;

    public WorldVui(HeldInput held) {
        vui = new Vui(new Backend(McFontMetrics.INSTANCE, new McVuiInput(held), new McVuiTextures()));
        vui.registry().addRoot(new McResourceRoot());
        vui.registry().addRoot(new ClasspathRoot(WorldVui.class.getClassLoader()));
    }

    public Vui vui() {
        return vui;
    }

    public TextureResolver textures() {
        return textures;
    }
}
