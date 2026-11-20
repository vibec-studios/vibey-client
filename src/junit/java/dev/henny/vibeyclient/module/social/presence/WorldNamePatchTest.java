package dev.henny.vibeyclient.module.social.presence;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorldNamePatchTest {
    @Test
    void presenceNameIsAdoptedWithoutPatch() {
        assertNull(WorldNamePatch.name("Inselwelt", null, "Inselwelt", 0));
        assertNull(WorldNamePatch.name("Inselwelt", null, "Inselwelt", WorldNamePatch.GRACE_MS - 1));
        assertNull(WorldNamePatch.name("Inselwelt", "Inselwelt", "Inselwelt", 0));
    }

    @Test
    void patchWhenNotAdoptedInTime() {
        assertEquals("Inselwelt", WorldNamePatch.name("Inselwelt", null, "Inselwelt", WorldNamePatch.GRACE_MS));
        assertEquals("Inselwelt", WorldNamePatch.name("Inselwelt", null, null, 0));
    }

    @Test
    void patchOnRename() {
        assertEquals("Neue Insel", WorldNamePatch.name(" Neue Insel ", "Inselwelt", "Neue Insel", 0));
    }

    @Test
    void nothingWithoutWorld() {
        assertNull(WorldNamePatch.name(null, null, null, 99_999));
        assertNull(WorldNamePatch.name("  ", "x", null, 99_999));
    }
}
