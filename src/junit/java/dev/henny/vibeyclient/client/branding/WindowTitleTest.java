package dev.henny.vibeyclient.client.branding;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WindowTitleTest {
    @Test
    void titleScreen() {
        assertEquals("Vibey Client 0.4.0 · Minecraft 1.21.11", WindowTitle.compose("Minecraft* 1.21.11", "0.4.0", "1.21.11"));
    }

    @Test
    void keepsVanillaPlace() {
        assertEquals("Vibey Client 0.4.0 · Minecraft 26.3 – Einzelspieler",
                WindowTitle.compose("Minecraft* 26.3 - Einzelspieler", "0.4.0", "26.3"));
        assertEquals("Vibey Client 0.4.0 · Minecraft 1.21.8 – Multiplayer (3rd-party Server)",
                WindowTitle.compose("Minecraft 1.21.8 - Multiplayer (3rd-party Server)", "0.4.0", "1.21.8"));
    }

    @Test
    void ignoresWhatLoadersPutIntoTheVanillaName() {
        assertEquals("Vibey Client 1 · Minecraft 26.3", WindowTitle.compose("Minecraft NeoForge* 26.3", "1", "26.3"));
        assertEquals("Vibey Client 1 · Minecraft 26.3 – Singleplayer",
                WindowTitle.compose("Minecraft NeoForge* 26.3 - Singleplayer", "1", "26.3"));
    }

    @Test
    void worksWhenAnotherModChangedTheTitle() {
        assertEquals("Vibey Client 1 · Minecraft 1.21.11", WindowTitle.compose("My Pack", "1", "1.21.11"));
        assertEquals("Vibey Client 1 · Minecraft 1.21.11", WindowTitle.compose(null, "1", "1.21.11"));
    }

    @Test
    void composingTwiceKeepsOneVibeyPrefix() {
        String once = WindowTitle.compose("Minecraft* 1.21.11", "1", "1.21.11");
        assertEquals(once, WindowTitle.compose(once, "1", "1.21.11"));
    }
}
