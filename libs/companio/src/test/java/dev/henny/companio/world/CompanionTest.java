package dev.henny.companio.world;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.def.LocoState;
import dev.henny.companio.def.MoveMode;
import dev.henny.companio.sense.Senses;
import dev.henny.companio.support.GridView;
import dev.henny.companio.support.TestPacks;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompanionTest {
    private final Senses senses = new Senses();
    private final GridView world = new GridView().floor(63);
    private double now;

    private Companion companion(String behaviors) {
        CompanionDef def = TestPacks.blob(behaviors);
        Companion c = new Companion(def, senses, 7, () -> "en_us");
        owner(0.5, 64, 0.5);
        c.spawn(0.5, 64, 2.5, 0);
        return c;
    }

    private void owner(double x, double y, double z) {
        senses.owner.present = true;
        senses.owner.x = x;
        senses.owner.y = y;
        senses.owner.z = z;
        senses.owner.eyeY = y + 1.62;
    }

    private void run(Companion c, double seconds) {
        for (int i = 0; i < Math.round(seconds * 20); i++) {
            now += 0.05;
            c.tick(world, now, 0.05f);
        }
    }

    @Test
    void followsTheOwnerAndStopsNearby() {
        Companion c = companion("[]");
        run(c, 0.5);
        owner(12.5, 64, 2.5);
        run(c, 0.5);
        assertTrue(c.body.speed() > 1, "walks after the owner");
        run(c, 6);
        double d = c.body.distance(12.5, 64, 2.5);
        assertTrue(d < 4.0, "close to the owner, distance " + d);
        run(c, 1);
        assertTrue(c.body.speed() < 0.3, "stands still near the owner");
        assertEquals(LocoState.IDLE, c.loco());
    }

    @Test
    void teleportsWhenFarAway() {
        Companion c = companion("[{\"id\":\"poof\",\"priority\":50,\"on\":\"teleport\",\"duration\":1}]");
        run(c, 0.2);
        owner(60.5, 64, 0.5);
        run(c, 0.2);
        assertTrue(c.body.distance(60.5, 64, 0.5) < 5, "teleported next to the owner");
        run(c, 0.1);
        assertEquals("poof", c.brain().current().def.id());
    }

    @Test
    void obeysStayAndSit() {
        Companion c = companion("[]");
        run(c, 0.2);
        c.command(Command.STAY);
        double x = c.body.x;
        owner(10.5, 64, 0.5);
        run(c, 2);
        assertEquals(x, c.body.x, 0.05);
        assertEquals(MoveMode.STAY, c.mode());
        c.command(Command.SIT);
        run(c, 0.2);
        assertEquals(LocoState.SIT, c.loco());
        c.command(Command.FOLLOW);
        run(c, 2);
        assertTrue(c.body.x > x + 2);
    }

    @Test
    void pickUpAndPlace() {
        Companion c = companion("[]");
        run(c, 0.2);
        c.pickUp();
        c.holdAt(3.5, 66, 3.5);
        run(c, 0.2);
        assertEquals(LocoState.HELD, c.loco());
        assertEquals(66, c.body.y, 1e-6, "no gravity while held");
        c.place(3.5, 66, 3.5);
        run(c, 1);
        assertEquals(64, c.body.y, 1e-6, "falls after being put down");
    }

    @Test
    void ridesOnTheShoulder() {
        Companion c = companion("[{\"id\":\"up\",\"priority\":10,\"move\":\"shoulder\",\"obeys\":false}]");
        run(c, 3);
        assertEquals(MoveMode.SHOULDER, c.riding());
        assertEquals(LocoState.RIDE, c.loco());
    }

    @Test
    void propsFollowBehaviors() {
        Companion c = companion("[{\"id\":\"hat\",\"priority\":10,\"when\":\"weather.rain\",\"props\":{\"hat\":true}}]");
        run(c, 0.1);
        boolean[] hidden = c.hiddenBones(null);
        assertTrue(hidden[2], "hat hidden by default");
        senses.raining = true;
        run(c, 0.1);
        assertFalse(c.hiddenBones(hidden)[2], "hat shown in the rain");
    }

    @Test
    void sizeIsClampedAndScalesTheBox() {
        Companion c = companion("[]");
        c.size(5f);
        assertEquals(2f, c.size());
        assertEquals(1.8f, c.body.height, 1e-5);
    }
}
