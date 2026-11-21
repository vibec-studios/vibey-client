package dev.henny.vibeafk.game;

import dev.henny.vibeafk.Fs;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.instrument.Instrumentation;
import java.util.HashSet;
import java.util.Set;

public final class BridgeLoader {
    public static final long GIVE_UP_MS = 600_000;

    private final Instrumentation inst;
    private final String loaderKind;
    private final long startedAt;
    private String mappingsPath;
    private Names mapped;
    private String mappingsError;
    private String status = "pending";
    private String error;
    private boolean failed;

    public BridgeLoader(Instrumentation inst, String loaderKind, long startedAt) {
        this.inst = inst;
        this.loaderKind = loaderKind;
        this.startedAt = startedAt;
    }

    public String status() {
        return status;
    }

    public String takeError() {
        String e = error;
        error = null;
        return e;
    }

    public boolean failed() {
        return failed;
    }

    public void setMappings(String path) {
        if (path == null ? mappingsPath == null : path.equals(mappingsPath)) return;
        mappingsPath = path;
        mapped = null;
        mappingsError = null;
    }

    public static final class Result {
        public final GameBridge bridge;
        public final String names;
        public final ClassLoader loader;

        Result(GameBridge bridge, String names, ClassLoader loader) {
            this.bridge = bridge;
            this.names = names;
            this.loader = loader;
        }
    }

    public Result attempt(long now, Opener opener) {
        if (failed) return null;
        Class<?>[] loaded = inst.getAllLoadedClasses();
        Class<?> direct = find(loaded, GameBridge.MINECRAFT);
        if (direct != null) {
            GameBridge b = GameBridge.create(new Reflect(Names.IDENTITY, opener, direct.getClassLoader()), direct);
            if (b != null) {
                status = "mojang";
                return new Result(b, "mojang", direct.getClassLoader());
            }
            if (now - startedAt > 20_000) {
                status = "forge".equals(loaderKind) ? "srg" : "unknown";
                return giveUp("names_unsupported", "Minecraft class found, but its members use unknown names (" + status + ")");
            }
            return null;
        }
        Names names = mapped(now);
        if (names != null) {
            String runtime = names.cls(GameBridge.MINECRAFT);
            Class<?> mc = runtime == null ? null : find(loaded, runtime);
            if (mc != null) {
                GameBridge b = GameBridge.create(new Reflect(names, opener, mc.getClassLoader()), mc);
                if (b != null) {
                    status = names.kind();
                    return new Result(b, names.kind(), mc.getClassLoader());
                }
                return giveUp("mappings_invalid", "mappings do not match this game version");
            }
        }
        if (now - startedAt > GIVE_UP_MS) {
            if (mappingsPath == null) return giveUp("mappings_missing", "no mappings in afk.json and the game is obfuscated");
            return giveUp("mappings_invalid", mappingsError != null ? mappingsError : "Minecraft class not found with these mappings");
        }
        return null;
    }

    private Result giveUp(String code, String message) {
        failed = true;
        if (!status.equals("srg")) status = "unknown";
        error = code + ": " + message;
        return null;
    }

    private Names mapped(long now) {
        if (mapped != null) return mapped;
        if (mappingsPath == null || mappingsError != null) return null;
        File f = new File(mappingsPath);
        if (!f.isFile()) {
            mappingsError = "mappings file not found: " + mappingsPath;
            return null;
        }
        InputStream in = null;
        try {
            in = new FileInputStream(f);
            ProGuard pg = ProGuard.parse(new InputStreamReader(in, Fs.UTF8), GameBridge.MEMBER_CLASSES);
            Tiny tiny = null;
            if ("fabric".equals(loaderKind) || "quilt".equals(loaderKind)) tiny = intermediary(pg);
            mapped = new MappedNames(pg, tiny);
            return mapped;
        } catch (Throwable t) {
            mappingsError = "cannot read mappings: " + t;
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

    private static Tiny intermediary(ProGuard pg) throws Exception {
        InputStream in = ClassLoader.getSystemResourceAsStream("mappings/mappings.tiny");
        if (in == null) return null;
        try {
            Set<String> owners = new HashSet<String>();
            for (String moj : GameBridge.MEMBER_CLASSES) {
                String obf = pg.mojToObf.get(moj);
                if (obf != null) owners.add(obf.replace('.', '/'));
            }
            return Tiny.parse(new InputStreamReader(in, Fs.UTF8), "official", "intermediary", owners);
        } finally {
            in.close();
        }
    }

    private static Class<?> find(Class<?>[] loaded, String name) {
        for (Class<?> c : loaded) if (name.equals(c.getName())) return c;
        return null;
    }
}
