package dev.henny.vui.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import dev.henny.vui.Vui;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.dom.Queries;
import dev.henny.vui.geom.Rect;
import dev.henny.vui.layout.ScreenRects;

import java.util.LinkedHashMap;
import java.util.Map;

final class DevRoutes {
    interface Route {
        void handle(HttpExchange ex, Query q) throws Exception;
    }

    private final Vui vui;
    private final DevTarget target;
    private final UiTasks tasks;
    private final DevKeys keys = new DevKeys();

    DevRoutes(Vui vui, DevTarget target, UiTasks tasks) {
        this.vui = vui;
        this.target = target;
        this.tasks = tasks;
    }

    Map<String, Route> all() {
        Map<String, Route> m = new LinkedHashMap<>();
        m.put("/tree", this::tree);
        m.put("/docs", this::docs);
        m.put("/errors", this::errors);
        m.put("/reload", this::reload);
        m.put("/open", this::open);
        m.put("/click", this::click);
        m.put("/key", this::key);
        m.put("/screenshot", this::screenshot);
        m.put("/", this::help);
        return m;
    }

    private void tree(HttpExchange ex, Query q) throws Exception {
        JsonObject body = tasks.call(() -> {
            Document doc = document(q);
            return doc == null ? null : TreeJson.document(doc, q.has("styles"));
        });
        if (body == null) Responses.error(ex, 404, "no open document");
        else Responses.json(ex, 200, body);
    }

    private void docs(HttpExchange ex, Query q) throws Exception {
        JsonArray body = tasks.call(() -> {
            JsonArray list = new JsonArray();
            for (Document d : target.documents()) {
                JsonObject o = new JsonObject();
                o.addProperty("id", d.documentId());
                o.addProperty("file", d.template().fileName());
                o.addProperty("errors", d.diagnostics().errors().size());
                list.add(o);
            }
            return list;
        });
        Responses.json(ex, 200, body);
    }

    private void errors(HttpExchange ex, Query q) throws Exception {
        JsonObject body = tasks.call(() -> {
            JsonObject o = new JsonObject();
            for (Document d : target.documents()) o.add(d.documentId(), TreeJson.diagnostics(d.diagnostics().all()));
            o.add("<vui:defaults>", TreeJson.diagnostics(vui.defaultStyleDiagnostics().all()));
            return o;
        });
        Responses.json(ex, 200, body);
    }

    private void reload(HttpExchange ex, Query q) throws Exception {
        JsonObject body = tasks.call(() -> {
            vui.reloader().reloadAll();
            JsonObject o = new JsonObject();
            o.addProperty("reloaded", vui.reloader().documents().size());
            JsonArray errs = new JsonArray();
            for (Document d : vui.reloader().documents()) errs.addAll(TreeJson.diagnostics(d.diagnostics().all()));
            o.add("errors", errs);
            return o;
        });
        Responses.json(ex, 200, body);
    }

    private void open(HttpExchange ex, Query q) throws Exception {
        String id = q.get("doc");
        if (id == null) {
            Responses.error(ex, 400, "missing ?doc=ns:path");
            return;
        }
        JsonObject body = tasks.call(() -> {
            Document d = target.open(id);
            return d == null ? null : TreeJson.document(d, false);
        });
        if (body == null) Responses.error(ex, 404, "could not open " + id);
        else Responses.json(ex, 200, body);
    }

    private void click(HttpExchange ex, Query q) throws Exception {
        JsonObject body = tasks.call(() -> {
            Document doc = document(q);
            if (doc == null) return null;
            Element e = q.has("id") ? doc.find(q.get("id")) : q.has("selector") ? Queries.first(doc.root(), q.get("selector")) : null;
            if (e == null) return null;
            Rect r = ScreenRects.of(e);
            float x = r.centerX();
            float y = r.centerY();
            doc.pointerMove(x, y);
            doc.pointerDown(x, y, 0, 0);
            doc.pointerUp(x, y, 0, 0);
            doc.frame(0);
            JsonObject o = new JsonObject();
            o.addProperty("clicked", e.tag() + (e.id() != null ? "#" + e.id() : ""));
            o.addProperty("at", e.pos().toString());
            return o;
        });
        if (body == null) Responses.error(ex, 404, "element not found (use ?id= or ?selector=)");
        else Responses.json(ex, 200, body);
    }

    private void key(HttpExchange ex, Query q) throws Exception {
        JsonObject body = tasks.call(() -> {
            Document doc = document(q);
            return doc == null ? null : keys.send(doc, q);
        });
        if (body == null) Responses.error(ex, 404, "no open document");
        else Responses.json(ex, 200, body);
    }

    private void screenshot(HttpExchange ex, Query q) throws Exception {
        byte[] png = tasks.call(target::screenshot);
        if (png == null) Responses.error(ex, 503, "screenshot not available");
        else Responses.png(ex, png);
    }

    private void help(HttpExchange ex, Query q) throws Exception {
        JsonObject o = new JsonObject();
        JsonArray endpoints = new JsonArray();
        for (String path : all().keySet()) endpoints.add(path);
        o.add("endpoints", endpoints);
        o.addProperty("docs", "libs/vui/docs/hot-reload.md");
        Responses.json(ex, 200, o);
    }

    private Document document(Query q) {
        String id = q.get("doc");
        if (id != null) for (Document d : target.documents()) if (id.equals(d.documentId())) return d;
        return target.active();
    }
}
