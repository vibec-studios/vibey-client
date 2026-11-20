package dev.henny.r3.persist;

import dev.henny.r3.hint.SetHintStore;
import dev.henny.r3.input.Binding;
import dev.henny.r3.input.Gesture;
import dev.henny.r3.input.GestureBindings;
import dev.henny.r3.input.InputCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigStoreTest {
    @TempDir
    Path dir;

    @Test
    void hintsAndBindingsSurviveARestart() {
        ConfigStore store = new ConfigStore(dir);
        SetHintStore hints = new SetHintStore();
        hints.markSeen("r3.hint.hover");
        assertTrue(store.saveHints(hints));
        GestureBindings bindings = GestureBindings.defaults().set(Gesture.GRAB, Binding.of(InputCode.key(71)));
        assertTrue(store.saveBindings(bindings));

        ConfigStore again = new ConfigStore(dir);
        assertTrue(again.loadHints().seen("r3.hint.hover"));
        assertEquals(InputCode.key(71), again.loadBindings(GestureBindings.defaults()).get(Gesture.GRAB).code());
    }

    @Test
    void eachWorldKeyHasItsOwnFile() {
        ConfigStore store = new ConfigStore(dir);
        PlacementStore a = new PlacementStore();
        assertTrue(store.saveWorld(WorldKeys.server("play.example.net:25565"), a));
        assertTrue(store.saveWorld(WorldKeys.singleplayer("New World"), a));
        assertNotEquals(store.worldFile("mp:a"), store.worldFile("mp:b"));
        assertTrue(Files.exists(store.worldFile(WorldKeys.singleplayer("New World"))));
        assertEquals(0, store.loadWorld("sp:never").worldKeys().size());
    }

    @Test
    void brokenFilesFallBackToDefaults() throws Exception {
        Files.createDirectories(dir);
        Files.writeString(dir.resolve(ConfigStore.BINDINGS), "{nope");
        Files.writeString(dir.resolve(ConfigStore.HINTS), "[[[");
        ConfigStore store = new ConfigStore(dir);
        assertEquals(InputCode.MOUSE_MIDDLE, store.loadBindings(GestureBindings.defaults()).get(Gesture.GRAB).code());
        assertEquals(0, store.loadHints().ids().size());
    }
}
