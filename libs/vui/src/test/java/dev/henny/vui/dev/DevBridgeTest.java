package dev.henny.vui.dev;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.vui.Vui;
import dev.henny.vui.bind.Model;
import dev.henny.vui.dom.Document;
import dev.henny.vui.headless.Headless;
import dev.henny.vui.reload.MemoryRoot;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevBridgeTest {
    private final Headless headless = Headless.create();
    private Vui vui;
    private DevBridge bridge;
    private Document doc;
    private Model model;
    private volatile boolean running = true;
    private Thread ui;

    @BeforeEach
    void start() throws IOException {
        vui = headless.vui().devMode(true);
        vui.registry().addRoot(new MemoryRoot().put("test:page", """
                <template><column><text id="t">{{ n }}</text><button id="b" @click="n = n + 1">+</button><textfield id="f" v-model="name"/></column></template>"""));
        model = new Model().value("n", 0).value("name", "");
        doc = vui.open("test:page", model);
        doc.resize(200, 100);
        doc.frame(0);
        DevTarget target = new DevTarget() {
            @Override
            public List<Document> documents() {
                return vui.reloader().documents();
            }

            @Override
            public Document open(String id) {
                return vui.open(id, new Model());
            }

            @Override
            public byte[] screenshot() {
                try {
                    return headless.png(doc, 1);
                } catch (IOException e) {
                    return null;
                }
            }
        };
        bridge = new DevBridge(vui, target).start(0);
        ui = new Thread(() -> {
            while (running) {
                vui.tick();
                for (Document d : vui.reloader().documents()) d.frame(0.016f);
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    return;
                }
            }
        });
        ui.start();
    }

    @AfterEach
    void stop() throws InterruptedException {
        running = false;
        ui.join();
        bridge.close();
    }

    private HttpResponse<byte[]> get(String path) throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + bridge.port() + path)).GET().build();
        return client.send(req, HttpResponse.BodyHandlers.ofByteArray());
    }

    private JsonObject json(String path) throws Exception {
        HttpResponse<byte[]> r = get(path);
        return JsonParser.parseString(new String(r.body())).getAsJsonObject();
    }

    @Test
    void treeClickKeyScreenshotErrors() throws Exception {
        JsonObject tree = json("/tree?doc=test:page");
        assertEquals("test:page", tree.get("id").getAsString());
        assertTrue(tree.toString().contains("\"at\":\"memory:assets/test/vui/page.vui:1:"), tree.toString());
        JsonObject click = json("/click?id=b");
        assertEquals("button#b", click.get("clicked").getAsString());
        assertEquals(1.0, ((Number) model.get("n")).doubleValue());
        json("/click?selector=textfield");
        json("/key?text=hey");
        assertEquals("hey", model.get("name"));
        JsonObject key = json("/key?key=tab");
        assertTrue(key.get("handled").getAsBoolean());
        HttpResponse<byte[]> png = get("/screenshot");
        assertEquals(200, png.statusCode());
        assertEquals((byte) 0x89, png.body()[0]);
        JsonObject errors = json("/errors");
        assertEquals(0, errors.getAsJsonArray("test:page").size());
        assertEquals(404, get("/click?id=nope").statusCode());
        JsonObject reload = json("/reload");
        assertTrue(reload.get("reloaded").getAsInt() >= 1);
        assertTrue(json("/").getAsJsonArray("endpoints").size() >= 8);
    }

    @Test
    void refusesOutsideDevMode() {
        Vui plain = headless.vui();
        assertThrows(IllegalStateException.class, () -> new DevBridge(plain, null).start(0));
    }

    @Test
    void uiTasksRunOnTickThread() throws Exception {
        UiTasks tasks = new UiTasks();
        CompletableFuture<String> f = CompletableFuture.supplyAsync(() -> {
            try {
                return tasks.call(() -> Thread.currentThread().getName());
            } catch (Exception e) {
                return "error " + e;
            }
        });
        Thread.sleep(50);
        tasks.drain();
        assertEquals(Thread.currentThread().getName(), f.get());
    }
}
