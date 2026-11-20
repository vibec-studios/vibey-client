package dev.henny.r3.mc;

import dev.henny.r3.mc.client.R3Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public final class R3Mod {
    public static final String ID = "r3";
    public static final Logger LOG = LoggerFactory.getLogger(ID);
    private static final List<Runnable> ready = new ArrayList<>();
    private static String version = "?";

    private R3Mod() {}

    public static void init(String loadedVersion) {
        version = loadedVersion;
        LOG.info("r3 {} loaded", version);
    }

    public static String version() {
        return version;
    }

    public static synchronized void whenReady(Runnable task) {
        if (R3Client.started()) task.run();
        else ready.add(task);
    }

    static synchronized void startClient() {
        if (R3Client.started()) return;
        R3Client.get();
        List<Runnable> tasks = new ArrayList<>(ready);
        ready.clear();
        for (Runnable task : tasks) task.run();
    }
}
