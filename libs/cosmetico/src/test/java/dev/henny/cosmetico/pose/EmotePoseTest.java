package dev.henny.cosmetico.pose;

import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.fixtures.Fixtures;
import dev.henny.cosmetico.model.PlayerBone;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmotePoseTest {
    @Test
    void headRotationBlendsByWeightInModelSpace() {
        EmotePlay e = new EmotePlay("t:e", Fixtures.parse(Fixtures.emote("t:e")), 0);
        e.update(0.5);
        PartPose head = new PartPose();
        EmotePartPose.blend(e, PlayerBone.HEAD, head);
        assertEquals(-5f * (float) (Math.PI / 180), head.xRot, 1e-6f);
        PartPose body = new PartPose().set(1, 2, 3, 0, 0, 0, 1, 1, 1);
        EmotePartPose.blend(e, PlayerBone.BODY, body);
        assertEquals(1f, body.x);
        assertFalse(new RootPose().compute(e, 0.9375f));
        assertTrue(HeldItems.hide(e));
        assertFalse(HeldItems.hide(null));
    }
}
