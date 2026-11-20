package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;
import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.BundleContent;
import dev.henny.cosmetico.model.BundleHeader;
import dev.henny.cosmetico.model.BundleLooks;
import dev.henny.cosmetico.model.Model;
import dev.henny.cosmetico.model.Sound;
import dev.henny.cosmetico.model.Sprite;
import dev.henny.cosmetico.model.Texture;
import dev.henny.cosmetico.version.VersionGate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

final class BundleReader {
    private final VersionGate gate;

    BundleReader(VersionGate gate) {
        this.gate = gate;
    }

    Bundle read(JsonObject o, String sha, int size) {
        List<String> warnings = new ArrayList<>();
        BundleHeader header = HeaderReader.read(o, gate);
        Assets assets = Assets.read(o, warnings);
        BundleLooks looks = LooksReader.read(o, header.kind(), assets, warnings);
        Model model = ModelReader.read(o, header.kind(), assets, warnings);
        Map<String, Animation> animations = AnimationReader.readAll(o, header.kind(), model, assets, warnings);
        looks = EmoteReader.check(looks, animations);
        PriceGuard.check(o);
        BundleContent content = new BundleContent(assets.textures.toArray(new Texture[0]), model, animations,
                assets.sprites.toArray(new Sprite[0]), assets.sounds.toArray(new Sound[0]));
        return new Bundle(header, looks, content, sha, size, List.copyOf(warnings));
    }
}
