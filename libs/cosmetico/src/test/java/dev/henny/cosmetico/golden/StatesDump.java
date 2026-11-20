package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.anim.StateWeights;
import dev.henny.cosmetico.config.CosmeticoConfig;
import dev.henny.cosmetico.draw.EmoteVisibility;
import dev.henny.cosmetico.draw.ShownBundle;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.model.Slot;
import dev.henny.cosmetico.sim.PetGeometry;
import dev.henny.cosmetico.sim.StateStarts;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.Motion;

import java.util.List;
import java.util.UUID;

public final class StatesDump {
    private StatesDump() {}

    public static String dump(List<Bundle> bundles) {
        Out o = new Out();
        float[] w = new float[8];
        for (String has : Tables.HAS) {
            for (float[] in : Tables.INPUTS) {
                StateWeights.compute(Tables.bits(has), in[0], in[1], in[2], in[3], in[4], in[5], in[6], w);
                o.line("weights", has, Out.f(in), Out.f(w));
            }
        }
        Motion m = new Motion();
        for (float[] s : Tables.STATE_STEPS) {
            m.sprintTarget = s[1];
            m.glideTarget = s[2];
            m.idleLongTarget = s[3];
            m.happyTarget = s[4];
            m.sprint = s[5];
            m.glide = s[6];
            m.idleLong = s[7];
            m.happy = s[8];
            StateStarts.update(m, s[0]);
            o.line("starts", s[0], m.stateStart[4], m.stateStart[5], m.stateStart[6], m.stateStart[7]);
        }
        CosmeticoConfig config = new CosmeticoConfig();
        ShownBundle shown = new ShownBundle(TestHost.engine().items, config);
        for (Bundle b : bundles) {
            if (b.slot == Slot.PET) o.line("pet", b.id, PetGeometry.halfWidth(b), PetGeometry.side(b));
            if (b.slot != null && !b.hideWithArmor.isEmpty()) o.line("armor", b.id, armor(b, shown, config));
        }
        o.line("emoteRules", emoteRules(bundles));
        return o.toString();
    }

    private static String armor(Bundle b, ShownBundle shown, CosmeticoConfig config) {
        StringBuilder s = new StringBuilder();
        Avatar a = new Avatar(UUID.randomUUID(), false);
        for (int mask = 0; mask < 16; mask++) {
            a.frame.headArmor = (mask & 1) != 0;
            a.frame.chestArmor = (mask & 2) != 0;
            a.frame.legsArmor = (mask & 4) != 0;
            a.frame.feetArmor = (mask & 8) != 0;
            s.append(shown.hiddenByArmor(a, b) ? '1' : '0');
        }
        config.hideWithArmor = false;
        s.append(' ').append(shown.hiddenByArmor(a, b) ? '1' : '0');
        config.hideWithArmor = true;
        return s.toString();
    }

    private static String emoteRules(List<Bundle> bundles) {
        Bundle emote = bundles.stream().filter(Bundle::isEmote).findFirst().orElseThrow();
        UUID self = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        StringBuilder s = new StringBuilder();
        CosmeticoConfig c = new CosmeticoConfig();
        for (int mask = 0; mask < 32; mask++) {
            c.emotes = (mask & 1) != 0;
            c.emotesServerOff = (mask & 2) != 0;
            c.showOthersEmotes = (mask & 4) != 0;
            c.self = (mask & 8) != 0 ? self : null;
            Avatar a = new Avatar(self, (mask & 16) != 0);
            a.emote = new EmotePlay(emote.id, emote, 0);
            s.append(EmoteVisibility.visible(a, c) != null ? '1' : '0');
        }
        return s.toString();
    }
}
