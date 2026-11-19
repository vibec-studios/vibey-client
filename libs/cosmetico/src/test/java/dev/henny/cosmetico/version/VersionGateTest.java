package dev.henny.cosmetico.version;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VersionGateTest {
    @Test
    void followsTheHostVersion() {
        AtomicReference<String> v = new AtomicReference<>("0.3.1");
        VersionGate gate = new VersionGate(v::get);
        assertFalse(gate.satisfies("0.4.0"));
        v.set("0.4.0-beta");
        assertFalse(gate.satisfies("0.4.0"));
        v.set("v0.4.0+abc");
        assertTrue(gate.satisfies("0.4.0"));
    }

    @Test
    void semverOrder() {
        assertEquals(-1, Integer.signum(SemVer.compare("1.0.0-alpha", "1.0.0")));
        assertEquals(1, Integer.signum(SemVer.compare("1.10", "1.9")));
        assertEquals(0, SemVer.compare("1.2", "1.2.0"));
        assertEquals(0, SemVer.compare("garbage", "0"));
    }
}
