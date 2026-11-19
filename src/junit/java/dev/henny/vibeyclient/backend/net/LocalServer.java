package dev.henny.vibeyclient.backend.net;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/** A tiny HTTP server on 127.0.0.1 for the network tests (JDK built-in, no internet). */
final class LocalServer implements AutoCloseable {
    interface Handler {
        void handle(HttpExchange ex, int hit) throws IOException;
    }

    final HttpServer server;
    final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();
    final List<String> requests = new CopyOnWriteArrayList<>();

    LocalServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r, "test-http");
            t.setDaemon(true);
            return t;
        }));
        server.start();
    }

    String base() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    LocalServer on(String path, Handler h) {
        server.createContext(path, ex -> {
            String key = ex.getRequestURI().getPath();
            int n = hits.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet();
            requests.add(ex.getRequestMethod() + " " + ex.getRequestURI() + " auth=" + ex.getRequestHeaders().getFirst("Authorization"));
            try {
                h.handle(ex, n);
            } finally {
                ex.close();
            }
        });
        return this;
    }

    int hits(String path) {
        AtomicInteger a = hits.get(path);
        return a == null ? 0 : a.get();
    }

    static void send(HttpExchange ex, int status, String body, String... headers) throws IOException {
        for (int i = 0; i + 1 < headers.length; i += 2) ex.getResponseHeaders().add(headers[i], headers[i + 1]);
        byte[] b = body == null ? new byte[0] : body.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, b.length == 0 ? -1 : b.length);
        if (b.length > 0) ex.getResponseBody().write(b);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
