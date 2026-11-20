package dev.henny.companio.world;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.support.GridView;
import dev.henny.companio.support.TestPacks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SeparationTest {
    private final Senses senses = new Senses();
    private final GridView world = new GridView().floor(63);
    private double now;

    private Companion buriedUnderAFloor() {
        for (int x = -8; x <= 8; x++) for (int z = -8; z <= 8; z++) for (int y = 66; y <= 68; y++) world.solid(x, y, z);
        CompanionDef def = TestPacks.blob("[]");
        Companion c = new Companion(def, senses, 7, () -> "en_us");
        senses.owner.present = true;
        senses.owner.onGround = true;
        senses.owner.x = 1.5;
        senses.owner.y = 69;
        senses.owner.z = 0.5;
        senses.owner.eyeY = 70.62;
        c.spawn(0.5, 64, 1.5, 0);
        return c;
    }

    private void run(Companion c, double seconds) {
        for (int i = 0; i < Math.round(seconds * 20); i++) {
            now += 0.05;
            c.tick(world, now, 0.05f);
        }
    }

    @Test
    void companionCutOffBelowItsOwnerTeleportsUp() {
        Companion c = buriedUnderAFloor();
        run(c, 2);
        assertTrue(c.body.y < 65, "still below for the first seconds, y " + c.body.y);
        run(c, 2);
        assertTrue(c.body.y >= 68.9, "teleported onto the owner's floor, y " + c.body.y);
        assertTrue(c.body.distance(1.5, 69, 0.5) < 4, "next to the owner");
    }

    @Test
    void flyingOwnerAboveDoesNotPullItUp() {
        Companion c = buriedUnderAFloor();
        senses.owner.onGround = false;
        senses.owner.flying = true;
        run(c, 5);
        assertTrue(c.body.y < 65, "stays on its own floor while the owner flies, y " + c.body.y);
    }
}
