package dev.henny.vui.dev;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.henny.vui.diag.Diagnostic;
import dev.henny.vui.dom.Document;
import dev.henny.vui.dom.Element;
import dev.henny.vui.expr.Values;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;

import java.util.List;
import java.util.Map;

public final class TreeJson {
    private TreeJson() {
    }

    public static JsonObject document(Document doc, boolean styles) {
        JsonObject o = new JsonObject();
        o.addProperty("id", doc.documentId());
        o.addProperty("file", doc.template().fileName());
        o.addProperty("width", doc.width());
        o.addProperty("height", doc.height());
        o.add("errors", diagnostics(doc.diagnostics().all()));
        o.add("root", element(doc.root(), styles));
        return o;
    }

    public static JsonArray diagnostics(List<Diagnostic> list) {
        JsonArray out = new JsonArray();
        for (Diagnostic d : list) {
            JsonObject o = new JsonObject();
            o.addProperty("severity", d.severity().label());
            o.addProperty("at", d.pos().toString());
            o.addProperty("message", d.message());
            if (d.hint() != null) o.addProperty("hint", d.hint());
            out.add(o);
        }
        return out;
    }

    public static JsonObject element(Element e, boolean styles) {
        JsonObject o = new JsonObject();
        o.addProperty("tag", e.tag());
        if (e.id() != null) o.addProperty("id", e.id());
        if (e.key() != null) o.addProperty("key", Values.display(e.key()));
        List<String> classes = e.classes().all();
        if (!classes.isEmpty()) o.addProperty("class", String.join(" ", classes));
        String states = StateFlags.names(e.states() & ~(StateFlags.FIRST | StateFlags.LAST));
        if (!states.isEmpty()) o.addProperty("states", states);
        o.addProperty("at", e.pos().toString());
        o.add("box", box(e.box()));
        if (e.text() != null) o.addProperty("text", e.text().chars().toString());
        if (!e.flags().shown) o.addProperty("shown", false);
        if (e.flags().overlay) o.addProperty("overlay", e.flags().overlayOpen ? "open" : "closed");
        JsonObject props = props(e.props());
        if (props.size() > 0) o.add("props", props);
        if (styles) o.add("style", style(e));
        if (e.text() == null && e.childCount() > 0) {
            JsonArray kids = new JsonArray();
            for (int i = 0; i < e.childCount(); i++) {
                Element c = e.child(i);
                if (!c.tag().equals(Element.TEXT_TAG)) kids.add(element(c, styles));
            }
            o.add("children", kids);
        }
        return o;
    }

    private static JsonObject box(LayoutBox b) {
        JsonObject o = new JsonObject();
        o.addProperty("x", b.x);
        o.addProperty("y", b.y);
        o.addProperty("w", b.width);
        o.addProperty("h", b.height);
        return o;
    }

    private static JsonObject props(Map<String, Object> props) {
        JsonObject o = new JsonObject();
        for (Map.Entry<String, Object> e : props.entrySet()) {
            Object v = e.getValue();
            if (e.getKey().startsWith("$")) continue;
            if (v instanceof Number n) o.addProperty(e.getKey(), n);
            else if (v instanceof Boolean b) o.addProperty(e.getKey(), b);
            else if (v instanceof CharSequence || v instanceof Enum<?>) o.addProperty(e.getKey(), Values.display(v));
        }
        return o;
    }

    private static JsonObject style(Element e) {
        JsonObject o = new JsonObject();
        dev.henny.vui.style.ComputedStyle initial = dev.henny.vui.style.ComputedStyle.initial();
        for (Prop p : Prop.values()) {
            if (e.style().sameAs(p, initial)) continue;
            o.addProperty(p.cssName(), e.style().describe(p));
        }
        return o;
    }
}
