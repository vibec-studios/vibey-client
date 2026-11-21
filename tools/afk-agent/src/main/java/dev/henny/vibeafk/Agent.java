package dev.henny.vibeafk;

import dev.henny.vibeafk.hook.LwjglTransformer;
import dev.henny.vibeafk.hook.Target;

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;

public final class Agent {
    private static volatile boolean started;

    private Agent() {}

    public static void premain(String args, Instrumentation inst) {
        start(args, inst, false);
    }

    public static void agentmain(String args, Instrumentation inst) {
        start(args, inst, true);
    }

    static synchronized void start(String args, Instrumentation inst, boolean late) {
        if (started) return;
        started = true;
        try {
            boot(args, inst, late);
        } catch (Throwable t) {
            System.err.println("[vibe-afk] agent disabled: " + t);
        }
    }

    static File vibeDir(String args) {
        String a = args == null ? "" : args.trim();
        if (a.startsWith("dir=")) a = a.substring(4).trim();
        if (a.length() >= 2 && a.startsWith("\"") && a.endsWith("\"")) a = a.substring(1, a.length() - 1);
        if (!a.isEmpty()) return new File(a);
        String prop = System.getProperty("vibe.afk.dir");
        if (prop != null && !prop.trim().isEmpty()) return new File(prop.trim());
        return new File(System.getProperty("user.dir"), ".vibe");
    }

    private static void boot(String args, Instrumentation inst, boolean late) {
        File dir = vibeDir(args).getAbsoluteFile();
        String loaderKind = Env.loader(System.getProperty("sun.java.command"), System.getProperty("java.class.path"));
        final Session s = new Session(dir, inst, System.currentTimeMillis(), version(), loaderKind);
        final MainThread main = new MainThread(s);
        final Core core = new Core(main);
        main.attach(core);
        Core.HOOK = core;
        final Controller controller = new Controller(s, core);
        controller.prepare();
        s.event("agent_start", Json.obj("run", s.run, "agent_version", s.version, "late", late ? Boolean.TRUE : null));
        inst.addTransformer(new LwjglTransformer(new LwjglTransformer.Listener() {
            @Override
            public void transformed(String owner, List<String> methods, ClassLoader loader) {
                if (owner.equals(Target.GLFW) || owner.equals(Target.SDL_VIDEO) || owner.equals(Target.LWJGL2_DISPLAY)) {
                    s.lwjglLegacy = owner.equals(Target.LWJGL2_DISPLAY);
                    s.lwjglLoader = loader;
                }
            }

            @Override
            public void failed(String owner, Throwable t) {
                s.error("hook_failed", owner + ": " + t);
            }
        }), false);
        List<String> already = new ArrayList<String>();
        for (Class<?> c : inst.getAllLoadedClasses()) {
            String n = c.getName().replace('.', '/');
            if (Target.owners().contains(n)) already.add(n);
        }
        if (!already.isEmpty()) s.error("hook_failed", "already loaded before the agent: " + already);
        controller.writeState(System.currentTimeMillis(), true);
        Thread t = new Thread(controller, "vibe-afk-control");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                controller.stop();
                s.event("agent_stop", null);
                controller.writeState(System.currentTimeMillis(), false);
            }
        }, "vibe-afk-stop"));
    }

    static String version() {
        InputStream in = Agent.class.getResourceAsStream("/dev/henny/vibeafk/version.txt");
        if (in == null) return "dev";
        try {
            byte[] b = new byte[64];
            int n = in.read(b);
            return n <= 0 ? "dev" : new String(b, 0, n, Fs.UTF8).trim();
        } catch (Exception e) {
            return "dev";
        } finally {
            try {
                in.close();
            } catch (Exception ignored) {
            }
        }
    }
}
