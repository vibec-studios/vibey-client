package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.bundle.Bundle.*;
import dev.henny.vibeyclient.avatar.bundle.BundleParser;
import dev.henny.vibeyclient.avatar.bundle.NeedsUpdateException;

import java.util.ArrayList;
import java.util.List;

import static dev.henny.vibeyclient.avatar.golden.Out.f;
import static dev.henny.vibeyclient.avatar.golden.Out.i;

final class ParseDump {
    private ParseDump() {}

    static String dump(byte[] bytes) {
        Out o = new Out();
        try {
            bundle(o, BundleParser.parse(bytes));
        } catch (NeedsUpdateException e) {
            o.line("needsUpdate", e.required, e.getMessage());
        } catch (RuntimeException e) {
            o.line("error", e.getClass().getSimpleName(), e.getMessage());
        }
        return o.toString();
    }

    static void bundle(Out o, Bundle b) {
        o.line("id", b.id).line("kind", b.kind).line("name", b.name.en(), "|", b.name.de());
        o.line("description", b.description.en(), "|", b.description.de()).line("author", b.author).line("tags", b.tags);
        o.line("rarity", b.rarity).line("slot", b.slot);
        List<String> hide = new ArrayList<>();
        for (Armor a : Armor.values()) if (b.hideWithArmor.contains(a)) hide.add(a.name());
        o.line("hideWithArmor", hide);
        o.line("pet", b.pet == null ? "null" : b.pet.follow() + " " + b.pet.scale());
        o.line("cape", b.capeTexture, "shield", b.shieldTexture);
        EmoteOptions e = b.emote;
        o.line("emote", e == null ? "null" : e.animation() + " " + e.loop() + " " + e.thirdPerson() + " " + e.stopOnMove() + " " + e.hideHeldItems());
        o.line("sha256", b.sha256, "size", b.byteSize);
        for (String w : b.warnings) o.line("warning", w);
        for (OffsetRule r : b.offsetWhen) o.line("offsetWhen", r.slot(), f(r.offset()), f(r.rotation()));
        for (int k = 0; k < b.textures.length; k++) {
            Texture t = b.textures[k];
            o.line("texture", k, t.id, t.width, t.height, t.pngWidth, t.pngHeight, t.frameCount, t.frameTime, i(t.frames),
                    t.interpolate, t.animated(), t.cycleTicks(), BundleParser.sha256(t.png));
        }
        for (int k = 0; k < b.sprites.length; k++) {
            Sprite s = b.sprites[k];
            o.line("sprite", k, s.id(), s.texture(), s.u1(), s.v1(), s.u2(), s.v2());
        }
        for (int k = 0; k < b.sounds.length; k++) o.line("sound", k, b.sounds[k].id(), b.sounds[k].ogg().length, BundleParser.sha256(b.sounds[k].ogg()));
        if (b.model != null) model(o, b.model);
        for (Animation a : b.animations.values()) animation(o, a);
    }

    private static void model(Out o, Model m) {
        o.line("model", m.texture, "cubes", m.cubeCount, "bones", m.bones.length, "locators", m.locators.size());
        for (Bone b : m.bones) {
            o.line("bone", b.index, b.name, b.parent, b.attach, f(b.pivot), f(b.rotation), b.emissive, b.translucent,
                    b.effectiveEmissive, b.effectiveTranslucent, b.cubes.length);
            for (Cube c : b.cubes) {
                o.line("cube", f(c.from), f(c.to), c.inflate, c.mirror, f(c.pivot), f(c.rotation), f(c.box), c.texture);
                if (c.faces == null) continue;
                for (int k = 0; k < 6; k++) {
                    Face x = c.faces[k];
                    o.line("face", k, x == null ? "null" : x.u1() + " " + x.v1() + " " + x.u2() + " " + x.v2() + " " + x.texture());
                }
            }
        }
        for (Locator l : m.locators.values()) o.line("locator", l.name(), l.bone(), l.playerBone(), f(l.position()));
    }

    private static void animation(Out o, Animation a) {
        o.line("animation", a.name, a.length, a.loop, a.hold, a.hasVisibility, "tracks", a.tracks.length, "events", a.events.length);
        for (Track t : a.tracks) {
            o.line("track", t.bone(), t.playerBone());
            channel(o, "rotation", t.rotation());
            channel(o, "position", t.position());
            channel(o, "scale", t.scale());
        }
        for (Event e : a.events) {
            o.line("event", e.t, e.type, e.id, e.sprite, e.sound, e.locator == null ? "null" : e.locator.name(), e.count,
                    f(e.spread), f(e.velocity), e.lifetime, e.size, e.gravity, e.emissive, e.volume, e.pitch, e.bone, e.visible);
        }
    }

    private static void channel(Out o, String name, Channel c) {
        if (c == null) return;
        o.line("channel", name, c.size());
        for (int k = 0; k < c.size(); k++) {
            o.line("key", c.t[k], f(new float[]{c.v[k * 3], c.v[k * 3 + 1], c.v[k * 3 + 2]}),
                    f(new float[]{c.pre[k * 3], c.pre[k * 3 + 1], c.pre[k * 3 + 2]}), c.interp[k]);
        }
    }
}
