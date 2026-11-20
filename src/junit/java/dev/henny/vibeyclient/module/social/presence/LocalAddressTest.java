package dev.henny.vibeyclient.module.social.presence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAddressTest {
    @Test
    void privateIpv4IsLocal() {
        for (String h : new String[] {"127.0.0.1", "127.5.6.7", "10.0.0.8", "172.16.0.1", "172.31.255.255", "192.168.178.20",
                "169.254.1.1", "0.0.0.0", "100.64.0.1", "224.0.0.1"}) {
            assertTrue(LocalAddress.isLocal(h), h);
        }
    }

    @Test
    void publicIpv4IsNot() {
        for (String h : new String[] {"8.8.8.8", "172.15.0.1", "172.32.0.1", "192.169.0.1", "1.1.1.1", "100.128.0.1"}) {
            assertFalse(LocalAddress.isLocal(h), h);
        }
    }

    @Test
    void ipv6() {
        for (String h : new String[] {"::1", "[::1]", "::", "fc00::1", "fd12:3456::1", "fe80::1", "fe80::1%eth0",
                "::ffff:192.168.0.1", "0:0:0:0:0:0:0:1", "ff02::1"}) {
            assertTrue(LocalAddress.isLocal(h), h);
        }
        assertFalse(LocalAddress.isLocal("2001:db8::1"));
        assertFalse(LocalAddress.isLocal("[2a01:4f8::2]"));
        assertFalse(LocalAddress.isLocal("::ffff:8.8.8.8"));
    }

    @Test
    void localNames() {
        for (String h : new String[] {"localhost", "LOCALHOST", "minecraft-pc", "nas.local", "pc.lan", "box.home.arpa",
                "foo.localhost", "srv.internal", "192-168-1-5.nip.io", "10.0.0.2.sslip.io", "nip.io", "", " ", null, "1234"}) {
            assertTrue(LocalAddress.isLocal(h), String.valueOf(h));
        }
    }

    @Test
    void publicNames() {
        for (String h : new String[] {"play.example.org", "mc.hypixel.net", "mossy-otter-27.axolite.cc", "8-8-8-8.nip.io",
                "example.org."}) {
            assertFalse(LocalAddress.isLocal(h), h);
        }
    }
}
