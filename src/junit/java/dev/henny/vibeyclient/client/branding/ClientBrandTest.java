package dev.henny.vibeyclient.client.branding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientBrandTest {
    @Test
    void prefixesTheLoaderBrand() {
        assertEquals("vibey,fabric", ClientBrand.of("fabric", true));
        assertEquals("vibey,neoforge", ClientBrand.of("neoforge", true));
        assertEquals("vibey,quilt,fabric", ClientBrand.of("quilt,fabric", true));
    }

    @Test
    void vanillaOrEmptyBecomesVibey() {
        assertEquals("vibey", ClientBrand.of("vanilla", true));
        assertEquals("vibey", ClientBrand.of("", true));
        assertEquals("vibey", ClientBrand.of(null, true));
    }

    @Test
    void offKeepsTheLoaderBrand() {
        assertEquals("fabric", ClientBrand.of("fabric", false));
        assertEquals("neoforge", ClientBrand.of("neoforge", false));
    }

    @Test
    void neverDoublesThePrefix() {
        assertEquals("vibey,fabric", ClientBrand.of(ClientBrand.of("fabric", true), true));
    }

    @Test
    void staysShortAndPlain() {
        String brand = ClientBrand.of("neoforge", true);
        assertEquals(brand, brand.replaceAll("[^a-z,]", ""));
    }
}
