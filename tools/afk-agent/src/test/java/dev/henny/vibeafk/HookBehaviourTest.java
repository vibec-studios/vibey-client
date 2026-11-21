package dev.henny.vibeafk;

import dev.henny.vibeafk.hook.LwjglTransformer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class HookBehaviourTest {
    @TempDir
    Path tmp;

    private Session session;
    private Core core;
    private MainThread main;
    private ClassLoader stubs;

    static final class StubLoader extends ClassLoader {
        private final File dir;

        StubLoader(File dir) {
            super(HookBehaviourTest.class.getClassLoader());
            this.dir = dir;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("org.lwjgl.")) return super.loadClass(name, resolve);
            synchronized (getClassLoadingLock(name)) {
                Class<?> c = findLoadedClass(name);
                if (c != null) return c;
                try {
                    String internal = name.replace('.', '/');
                    byte[] b = Files.readAllBytes(new File(dir, internal + ".class").toPath());
                    byte[] t = LwjglTransformer.transform(internal, b, new ArrayList<String>());
                    if (t != null) b = t;
                    return defineClass(name, b, 0, b.length);
                } catch (java.io.IOException e) {
                    throw new ClassNotFoundException(name, e);
                }
            }
        }
    }

    @BeforeEach
    void setUp() {
        File vibe = tmp.resolve(".vibe").toFile();
        session = new Session(vibe, null, System.currentTimeMillis(), "test", "vanilla");
        main = new MainThread(session);
        core = new Core(main);
        main.attach(core);
        Core.HOOK = core;
        String dir = System.getProperty("vibeafk.stubs").split(File.pathSeparator)[0];
        stubs = new StubLoader(new File(dir));
    }

    private Object call(String cls, String name, Class<?>[] types, Object... args) throws Exception {
        Method m = Class.forName(cls, true, stubs).getMethod(name, types);
        return m.invoke(null, args);
    }

    private Object stat(String cls, String field) throws Exception {
        Field f = Class.forName(cls, true, stubs).getField(field);
        return f.get(null);
    }

    private void background(boolean bg) {
        session.desired = new AfkConfig("s", bg, 2, true, true, false, 5, 10, 300, 6, 6, null);
    }

    private static final String GLFW = "org.lwjgl.glfw.GLFW";
    private static final String AL = "org.lwjgl.openal.AL10";
    private static final Class<?>[] LONG = {long.class};

    @Test
    void backgroundStartHidesFromCreationAndRestoresExactly() throws Exception {
        background(true);
        long w = (Long) call(GLFW, "glfwCreateWindow", new Class<?>[]{int.class, int.class, CharSequence.class, long.class, long.class},
                854, 480, "Minecraft", 0L, 0L);
        assertEquals(4242L, w);
        assertEquals(0, stat(GLFW, "visibleHint"));
        call(GLFW, "glfwShowWindow", LONG, w);
        assertEquals(0, stat(GLFW, "shows"), "the game's own show call is suppressed while hidden");

        call(AL, "alListenerf", new Class<?>[]{int.class, float.class}, 0x100A, 0.7f);
        call(AL, "alListenerf", new Class<?>[]{int.class, float.class}, 0x1001, 3f);
        assertEquals(3f, (Float) stat(AL, "other"), "other parameters pass through");

        call(GLFW, "glfwPollEvents", new Class<?>[0]);
        assertEquals(1, stat(GLFW, "polls"));
        assertTrue(core.sound.muted());
        assertEquals(0f, (Float) stat(AL, "gain"));
        assertTrue(core.throttle.on());

        long t0 = System.nanoTime();
        call(GLFW, "glfwSwapBuffers", LONG, w);
        call(GLFW, "glfwSwapBuffers", LONG, w);
        call(GLFW, "glfwSwapBuffers", LONG, w);
        long ms = (System.nanoTime() - t0) / 1_000_000;
        assertTrue(ms >= 900, "3 frames at 2 FPS take about 1 s, took " + ms);
        assertEquals(3, stat(GLFW, "swaps"));

        background(false);
        call(GLFW, "glfwPollEvents", new Class<?>[0]);
        assertEquals(1, stat(GLFW, "shows"), "agent shows the window");
        assertEquals(1, stat(GLFW, "focus"));
        assertFalse(core.sound.muted());
        assertEquals(0.7f, (Float) stat(AL, "gain"), "volume restored to the game's value");
        assertFalse(core.throttle.on());

        t0 = System.nanoTime();
        for (int k = 0; k < 5; k++) call(GLFW, "glfwSwapBuffers", LONG, w);
        assertTrue((System.nanoTime() - t0) / 1_000_000 < 200, "no throttle in the foreground");

        call(GLFW, "glfwShowWindow", LONG, w);
        assertEquals(2, stat(GLFW, "shows"), "the game's show call passes in the foreground");

        String events = new String(Files.readAllBytes(tmp.resolve(".vibe/afk-events.jsonl")), "UTF-8");
        assertTrue(events.contains("\"type\":\"mode\",\"mode\":\"background\""), events);
        assertTrue(events.contains("\"type\":\"mode\",\"mode\":\"foreground\""), events);
    }

    @Test
    void switchingToBackgroundLaterHidesTheWindow() throws Exception {
        background(false);
        long w = (Long) call(GLFW, "glfwCreateWindow", new Class<?>[]{int.class, int.class, CharSequence.class, long.class, long.class},
                854, 480, "Minecraft", 0L, 0L);
        assertEquals(-1, stat(GLFW, "visibleHint"));
        call(GLFW, "glfwPollEvents", new Class<?>[0]);
        assertEquals(0, stat(GLFW, "hides"));
        background(true);
        call(GLFW, "glfwPollEvents", new Class<?>[0]);
        assertEquals(1, stat(GLFW, "hides"));
        assertTrue(core.window.hidden());
        assertEquals(4242L, w);
    }

    @Test
    void sdlCreateGetsHiddenFlag() throws Exception {
        background(true);
        long w = (Long) call("org.lwjgl.sdl.SDLVideo", "SDL_CreateWindow", new Class<?>[]{CharSequence.class, int.class, int.class, long.class},
                "Minecraft", 854, 480, 0x2L);
        assertEquals(77L, w);
        assertEquals(0xAL, stat("org.lwjgl.sdl.SDLVideo", "lastFlags"));
        Object r = call("org.lwjgl.sdl.SDLVideo", "SDL_ShowWindow", LONG, w);
        assertEquals(Boolean.TRUE, r);
        assertEquals(0, stat("org.lwjgl.sdl.SDLVideo", "shows"));
        assertTrue(core.window.available());
    }

    @Test
    void hookExceptionsNeverReachTheGame() throws Exception {
        Core.HOOK = new java.util.function.BiFunction<String, Object, Object>() {
            @Override
            public Object apply(String s, Object o) {
                return "not a float";
            }
        };
        call(AL, "alListenerf", new Class<?>[]{int.class, float.class}, 0x100A, 0.5f);
        assertEquals(0.5f, (Float) stat(AL, "gain"));
    }
}
