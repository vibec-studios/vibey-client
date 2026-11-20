package dev.henny.vui.markup;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class JsonTreeReader {
    private final Diagnostics diagnostics = new Diagnostics();

    private JsonTreeReader() {
    }

    public static ComponentFile read(String id, String fileName, String source) {
        JsonTreeReader r = new JsonTreeReader();
        JsonValue root = PosJsonParser.parse(source, fileName, r.diagnostics);
        Map<String, Object> props = new LinkedHashMap<>();
        Map<String, String> components = new LinkedHashMap<>();
        List<TemplateNode> template = new ArrayList<>();
        String style = "";
        SourcePos stylePos = new SourcePos(fileName, 1, 1);
        if (root != null && !root.isObject()) r.diagnostics.error(root.pos(), "a .vui.json file must be an object", "e.g. { \"template\": {…} }");
        if (root != null && root.isObject()) {
            JsonValue p = root.get("props");
            if (p != null) PropsBlock.readProps(p, props, r.diagnostics);
            JsonValue c = root.get("components");
            if (c != null) PropsBlock.readComponents(c, components, r.diagnostics);
            JsonValue s = root.get("style");
            if (s != null) {
                style = r.style(s);
                stylePos = s.pos();
            }
            JsonValue t = root.get("template");
            if (t == null) r.diagnostics.error(root.pos(), "missing \"template\"", "add \"template\": { \"tag\": \"column\", … }");
            else r.nodes(t, template);
        }
        return new ComponentFile(id, fileName, template, style, stylePos, props, components, r.diagnostics);
    }

    private String style(JsonValue s) {
        if (s.isString()) return s.asString();
        StringBuilder sb = new StringBuilder();
        for (JsonValue line : s.asArray()) sb.append(line.asString()).append('\n');
        return sb.toString();
    }

    private void nodes(JsonValue v, List<TemplateNode> out) {
        if (v.isArray()) {
            for (JsonValue item : v.asArray()) node(item, out);
        } else {
            node(v, out);
        }
    }

    private void node(JsonValue v, List<TemplateNode> out) {
        if (v.isString()) {
            out.add(new TextNode(Interpolation.split(v.asString(), v.pos(), diagnostics), v.pos()));
            return;
        }
        if (!v.isObject()) {
            diagnostics.error(v.pos(), "a template node must be a string or an object", "e.g. { \"tag\": \"text\", \"children\": [\"Hi\"] }");
            return;
        }
        JsonValue tag = v.get("tag");
        if (tag == null || !tag.isString()) {
            diagnostics.error(v.pos(), "node needs a \"tag\"", null);
            return;
        }
        List<Attribute> attributes = new ArrayList<>();
        JsonValue attrs = v.get("attrs");
        if (attrs != null) {
            for (Map.Entry<String, JsonValue> e : attrs.asObject().entrySet()) {
                JsonValue value = e.getValue();
                String text = value.isNull() ? null : value.asString();
                attributes.add(AttributeNames.classify(e.getKey(), text, value.pos(), value.pos().shift(1), diagnostics));
            }
        }
        List<TemplateNode> children = new ArrayList<>();
        JsonValue kids = v.get("children");
        if (kids != null) nodes(kids, children);
        out.add(new ElementNode(tag.asString(), attributes, children, v.pos()));
    }
}
