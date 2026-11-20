package dev.henny.r3.mc.vui;

import dev.henny.r3.mc.compat.Game;
import dev.henny.r3.mc.compat.Ids;
import dev.henny.vui.reload.ResourceRoot;
import net.minecraft.server.packs.resources.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

final class McResourceRoot implements ResourceRoot {
    private static final String ASSETS = "assets/";

    @Override
    public String read(String relativePath) {
        if (!relativePath.startsWith(ASSETS)) return null;
        String rest = relativePath.substring(ASSETS.length());
        int slash = rest.indexOf('/');
        if (slash <= 0) return null;
        var id = Ids.parse(rest.substring(0, slash) + ":" + rest.substring(slash + 1));
        if (id == null) return null;
        Optional<Resource> resource = Game.mc().getResourceManager().getResource(id);
        if (resource.isEmpty()) return null;
        try (InputStream in = resource.get().open()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String describe(String relativePath) {
        return "resources:" + relativePath;
    }
}
