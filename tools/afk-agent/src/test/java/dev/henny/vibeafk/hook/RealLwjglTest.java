package dev.henny.vibeafk.hook;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

class RealLwjglTest {
    private static List<File> jars(String set) {
        String p = System.getProperty("vibeafk." + set);
        assertNotNull(p, "test classpath property vibeafk." + set);
        List<File> out = new ArrayList<>();
        for (String s : p.split(File.pathSeparator)) if (!s.isEmpty()) out.add(new File(s));
        return out;
    }

    private static byte[] classBytes(List<File> jars, String internal) throws IOException {
        for (File f : jars) {
            try (ZipFile z = new ZipFile(f)) {
                ZipEntry e = z.getEntry(internal + ".class");
                if (e == null) continue;
                try (InputStream in = z.getInputStream(e)) {
                    ByteArrayOutputStream b = new ByteArrayOutputStream();
                    in.transferTo(b);
                    return b.toByteArray();
                }
            }
        }
        return null;
    }

    static final class Transforming extends URLClassLoader {
        private final String target;
        private final byte[] bytes;

        Transforming(List<File> jars, String target, byte[] bytes) throws Exception {
            super(urls(jars), ClassLoader.getPlatformClassLoader());
            this.target = target;
            this.bytes = bytes;
        }

        private static URL[] urls(List<File> jars) throws Exception {
            URL[] u = new URL[jars.size()];
            for (int k = 0; k < u.length; k++) u[k] = jars.get(k).toURI().toURL();
            return u;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            if (name.equals(target.replace('/', '.'))) return defineClass(name, bytes, 0, bytes.length);
            return super.findClass(name);
        }
    }

    private static void verify(String set, String internal, int expectedWrappers) throws Exception {
        List<File> jars = jars(set);
        byte[] original = classBytes(jars, internal);
        assertNotNull(original, internal + " in " + set);
        List<String> done = new ArrayList<>();
        byte[] out = LwjglTransformer.transform(internal, original, done);
        assertNotNull(out, "transformed " + internal);
        assertEquals(expectedWrappers, done.size(), internal + " wrapped " + done);
        try (Transforming loader = new Transforming(jars, internal, out)) {
            try {
                Class.forName(internal.replace('/', '.'), true, loader);
            } catch (VerifyError | ClassFormatError | IncompatibleClassChangeError e) {
                fail("JVM rejected transformed " + internal + ": " + e);
            } catch (Throwable expectedNativeFailure) {
                assertFalse(expectedNativeFailure.toString().contains("VerifyError"), expectedNativeFailure.toString());
            }
        }
    }

    @Test
    void glfw333() throws Exception {
        verify("lwjgl33", Target.GLFW, 5);
    }

    @Test
    void openal333() throws Exception {
        verify("lwjgl33", Target.AL10, 1);
    }

    @Test
    void vulkan333() throws Exception {
        verify("lwjgl33", Target.VK_SWAPCHAIN, 1);
    }

    @Test
    void sdlVideo343() throws Exception {
        verify("lwjgl34", Target.SDL_VIDEO, 4);
    }

    @Test
    void sdlEvents343() throws Exception {
        verify("lwjgl34", Target.SDL_EVENTS, 2);
    }

    @Test
    void lwjgl2Display() throws Exception {
        verify("lwjgl2", Target.LWJGL2_DISPLAY, 1);
    }

    @Test
    void lwjgl2OpenAl() throws Exception {
        verify("lwjgl2", Target.AL10, 1);
    }
}
