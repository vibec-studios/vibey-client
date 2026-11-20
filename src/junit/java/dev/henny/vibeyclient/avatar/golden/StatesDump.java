package dev.henny.vibeyclient.avatar.golden;

import dev.henny.vibeyclient.avatar.AvatarConfig;
import dev.henny.vibeyclient.avatar.AvatarState;
import dev.henny.vibeyclient.avatar.AvatarTicker;
import dev.henny.vibeyclient.avatar.Slot;
import dev.henny.vibeyclient.avatar.anim.EmotePlay;
import dev.henny.vibeyclient.avatar.anim.StateWeights;
import dev.henny.vibeyclient.avatar.bundle.Bundle;
import dev.henny.vibeyclient.avatar.render.AvatarDraw;
import dev.henny.vibeyclient.avatar.render.AvatarHooks;

import java.util.List;
import java.util.UUID;

final class StatesDump {
    private StatesDump() {}

    static String dump(List<Bundle> bundles) {
        Out o = new Out();
        float[] w = new float[8];
        for (String has : Tables.HAS) {
            for (float[] in : Tables.INPUTS) {
                StateWeights.compute(Tables.bits(has), in[0], in[1], in[2], in[3], in[4], in[5], in[6], w);
                o.line("weights", has, Out.f(in), Out.f(w));
            }
        }
        AvatarState st = new AvatarState(UUID.fromString("00000000-0000-0000-0000-000000000003"), false);
        for (float[] s : Tables.STATE_STEPS) {
            st.sprintTarget = s[1];
            st.glideTarget = s[2];
            st.idleLongTarget = s[3];
            st.happyTarget = s[4];
            st.sprint = s[5];
            st.glide = s[6];
            st.idleLong = s[7];
            st.happy = s[8];
            AvatarTicker.states(st, s[0]);
            o.line("starts", s[0], st.stateStart[4], st.stateStart[5], st.stateStart[6], st.stateStart[7]);
        }
        for (Bundle b : bundles) {
            if (b.slot == Slot.PET) o.line("pet", b.id, AvatarTicker.petHalfWidth(b), AvatarTicker.petSide(b));
            if (b.slot != null && !b.hideWithArmor.isEmpty()) o.line("armor", b.id, armor(b));
        }
        emoteRules(o, bundles);
        return o.toString();
    }

    private static String armor(Bundle b) {
        StringBuilder s = new StringBuilder();
        AvatarState st = new AvatarState(UUID.randomUUID(), false);
        for (int m = 0; m < 16; m++) {
            st.headArmor = (m & 1) != 0;
            st.chestArmor = (m & 2) != 0;
            st.legsArmor = (m & 4) != 0;
            st.feetArmor = (m & 8) != 0;
            s.append(AvatarDraw.hiddenByArmor(st, b) ? '1' : '0');
        }
        AvatarDraw.hideWithArmor = false;
        s.append(' ').append(AvatarDraw.hiddenByArmor(st, b) ? '1' : '0');
        AvatarDraw.hideWithArmor = true;
        return s.toString();
    }

    private static void emoteRules(Out o, List<Bundle> bundles) {
        Bundle emote = bundles.stream().filter(Bundle::isEmote).findFirst().orElseThrow();
        UUID self = UUID.fromString("00000000-0000-0000-0000-00000000000a");
        StringBuilder s = new StringBuilder();
        for (int m = 0; m < 32; m++) {
            AvatarConfig.emotes = (m & 1) != 0;
            AvatarConfig.emotesServerOff = (m & 2) != 0;
            AvatarConfig.showOthersEmotes = (m & 4) != 0;
            AvatarConfig.self = (m & 8) != 0 ? self : null;
            AvatarState st = new AvatarState(self, (m & 16) != 0);
            st.emote = new EmotePlay(emote.id, emote, 0);
            s.append(AvatarHooks.emote(st) != null ? '1' : '0');
        }
        AvatarConfig.emotes = true;
        AvatarConfig.emotesServerOff = false;
        AvatarConfig.showOthersEmotes = true;
        AvatarConfig.self = null;
        o.line("emoteRules", s);
    }
}
