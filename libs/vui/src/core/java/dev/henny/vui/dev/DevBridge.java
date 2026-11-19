package dev.henny.vui.dev;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dev.henny.vui.Vui;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.Executors;

public final class DevBridge implements AutoCloseable {
    public static final int DEFAULT_PORT = 7591;

    private final Vui vui;
    private final UiTasks tasks = new UiTasks();
    private final DevRoutes routes;
    private HttpServer server;

    public DevBridge(Vui vui, DevTarget target) {
        this.vui = vui;
        this.routes = new DevRoutes(vui, target, tasks);
        vui.onTick(tasks::drain);
    }

    public DevBridge start(int port) throws IOException {
        if (!vui.devMode()) throw new IllegalStateException("the dev bridge only runs in dev mode (-Dvui.dev=true)");
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 16);
        for (Map.Entry<String, DevRoutes.Route> r : routes.all().entrySet()) {
            DevRoutes.Route route = r.getValue();
            server.createContext(r.getKey(), ex -> handle(ex, route));
        }
        server.setExecutor(Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable, "vui-dev-bridge");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        return this;
    }

    public int port() {
        return server == null ? -1 : server.getAddress().getPort();
    }

    public UiTasks tasks() {
        return tasks;
    }

    private void handle(HttpExchange ex, DevRoutes.Route route) throws IOException {
        try {
            if (!ex.getRemoteAddress().getAddress().isLoopbackAddress()) {
                Responses.error(ex, 403, "localhost only");
                return;
            }
            route.handle(ex, new Query(ex.getRequestURI()));
        } catch (Exception e) {
            Responses.error(ex, 500, String.valueOf(e.getMessage() == null ? e : e.getMessage()));
        } finally {
            ex.close();
        }
    }

    @Override
    public void close() {
        if (server != null) server.stop(0);
        server = null;
    }
}
