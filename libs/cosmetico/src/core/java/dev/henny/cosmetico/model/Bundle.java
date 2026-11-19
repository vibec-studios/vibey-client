package dev.henny.cosmetico.model;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class Bundle {
    public final String id;
    public final Kind kind;
    public final Text name, description;
    public final String author;
    public final List<String> tags;
    public final Rarity rarity;
    public final Slot slot;
    public final Set<Armor> hideWithArmor;
    public final PetOptions pet;
    public final int capeTexture, shieldTexture;
    public final EmoteOptions emote;
    public final Texture[] textures;
    public final Model model;
    public final Map<String, Animation> animations;
    public final Sprite[] sprites;
    public final Sound[] sounds;
    public final String sha256;
    public final int byteSize;
    public final List<String> warnings;
    public final List<OffsetRule> offsetWhen;

    public Bundle(BundleHeader h, BundleLooks looks, BundleContent c, String sha256, int byteSize, List<String> warnings) {
        this.id = h.id();
        this.kind = h.kind();
        this.name = h.name();
        this.description = h.description();
        this.author = h.author();
        this.tags = h.tags();
        this.rarity = h.rarity();
        this.slot = looks.slot();
        this.hideWithArmor = looks.hideWithArmor();
        this.pet = looks.pet();
        this.capeTexture = looks.capeTexture();
        this.shieldTexture = looks.shieldTexture();
        this.emote = looks.emote();
        this.offsetWhen = looks.offsetWhen() == null ? List.of() : looks.offsetWhen();
        this.textures = c.textures();
        this.model = c.model();
        this.animations = c.animations();
        this.sprites = c.sprites();
        this.sounds = c.sounds();
        this.sha256 = sha256;
        this.byteSize = byteSize;
        this.warnings = warnings;
    }

    public Animation animation(String name) {
        return animations.get(name);
    }

    public Animation emoteAnimation() {
        return emote == null ? null : animations.get(emote.animation());
    }

    public boolean isEmote() {
        return kind == Kind.EMOTE;
    }
}
