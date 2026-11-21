package dev.henny.vibeafk;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Locale;
import java.util.Map;

public final class Env {
    private Env() {}

    public static String loader(String command, String classPath) {
        String cmd = command == null ? "" : command;
        String main = cmd.trim().split("\\s+", 2)[0];
        String cp = (classPath == null ? "" : classPath).toLowerCase(Locale.ROOT).replace('\\', '/');
        if (main.startsWith("org.quiltmc.") || cp.contains("quilt-loader")) return "quilt";
        if (main.startsWith("net.fabricmc.") || cp.contains("fabric-loader")) return "fabric";
        String all = (cmd + " " + cp).toLowerCase(Locale.ROOT);
        if (all.contains("net.neoforged") || all.contains("/neoforged/") || all.contains("neoforge")) return "neoforge";
        if (all.contains("minecraftforge") || all.contains("fmlclient") || all.contains("forgeclient")
                || main.startsWith("cpw.mods.") || main.equals("net.minecraft.launchwrapper.Launch")) return "forge";
        if (main.equals("net.minecraft.client.main.Main") || cp.contains("minecraft")) return "vanilla";
        return "unknown";
    }

    public static String argument(String command, String name) {
        if (command == null) return null;
        String[] parts = command.trim().split("\\s+");
        for (int k = 0; k + 1 < parts.length; k++) if (parts[k].equals(name)) return parts[k + 1];
        return null;
    }

    public static String lwjglVersion(ClassLoader loader, boolean legacy) {
        try {
            if (legacy) {
                Class<?> sys = Class.forName("org.lwjgl.Sys", false, loader);
                Object v = sys.getMethod("getVersion").invoke(null);
                return v == null ? "2" : v.toString();
            }
            Class<?> ver = Class.forName("org.lwjgl.Version", false, loader);
            Object v = ver.getMethod("getVersion").invoke(null);
            return v == null ? "3" : v.toString().split("\\s+")[0];
        } catch (Throwable t) {
            return legacy ? "2" : "3";
        }
    }

    public static String mcVersion(ClassLoader loader) {
        if (loader == null) return null;
        InputStream in = null;
        try {
            in = loader.getResourceAsStream("version.json");
            if (in == null) return null;
            Reader r = new InputStreamReader(in, Fs.UTF8);
            StringBuilder b = new StringBuilder();
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0 && b.length() < 65536) b.append(buf, 0, n);
            Map<String, Object> o = Json.parseObject(b.toString());
            Object id = o.get("id");
            if (!(id instanceof String)) id = o.get("name");
            return id instanceof String ? (String) id : null;
        } catch (Throwable t) {
            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
        }
    }
}
