package dev.henny.cosmetico.parse;

import dev.henny.cosmetico.model.Animation;
import dev.henny.cosmetico.model.BundleLooks;
import dev.henny.cosmetico.model.EmoteOptions;

import java.util.Map;

final class EmoteReader {
    private EmoteReader() {}

    static BundleLooks check(BundleLooks looks, Map<String, Animation> animations) {
        EmoteOptions emote = looks.emote();
        if (emote == null) return looks;
        Animation main = animations.get(emote.animation());
        if (main == null) throw new BundleException("emote.animation \"" + emote.animation() + "\" does not exist");
        if (main.loop == emote.loop()) return looks;
        EmoteOptions fixed = new EmoteOptions(emote.animation(), main.loop, emote.thirdPerson(), emote.stopOnMove(), emote.hideHeldItems());
        return new BundleLooks(looks.slot(), looks.hideWithArmor(), looks.pet(), looks.capeTexture(), looks.shieldTexture(), fixed, looks.offsetWhen());
    }
}
