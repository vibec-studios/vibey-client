package dev.henny.vui.markup;

import dev.henny.vui.contract.Contract;
import dev.henny.vui.contract.ContractReader;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.json.JsonValue;
import dev.henny.vui.json.PosJsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SfcReader {
    private final String id;
    private final String fileName;
    private final Diagnostics diagnostics = new Diagnostics();
    private final List<TemplateNode> template = new ArrayList<>();
    private final StringBuilder style = new StringBuilder();
    private final Map<String, Object> props = new LinkedHashMap<>();
    private final Map<String, String> components = new LinkedHashMap<>();
    private SourcePos stylePos;
    private Contract contract = Contract.EMPTY;
    private boolean sawTemplate;

    private SfcReader(String id, String fileName) {
        this.id = id;
        this.fileName = fileName;
    }

    public static ComponentFile read(String id, String fileName, String source) {
        SfcReader r = new SfcReader(id, fileName);
        try {
            r.readBlocks(MarkupParser.parse(source, fileName, r.diagnostics));
        } catch (RuntimeException e) {
            r.diagnostics.error(new SourcePos(fileName, 1, 1), "internal parser error: " + e, "please report this file");
        }
        return r.build();
    }

    private void readBlocks(List<TemplateNode> nodes) {
        for (TemplateNode node : nodes) {
            if (node instanceof TextNode t) {
                diagnostics.error(t.pos(), "text outside of <template>", "put it inside <template>…</template>");
                continue;
            }
            block((ElementNode) node);
        }
        if (!sawTemplate) diagnostics.error(new SourcePos(fileName, 1, 1), "no <template> block", "a .vui file needs <template>…</template>");
    }

    private void block(ElementNode e) {
        switch (e.tag()) {
            case "template" -> {
                sawTemplate = true;
                template.addAll(e.children());
            }
            case "style" -> {
                TextPart content = rawContent(e);
                if (content == null) return;
                if (stylePos == null) stylePos = content.pos();
                else style.append('\n');
                style.append(content.text());
            }
            case "props" -> readProps(e);
            case "contract" -> readContract(e);
            case "script" -> readScript(e);
            case "import" -> readImport(e);
            default -> diagnostics.error(e.pos(), "unknown top-level block <" + e.tag() + ">",
                    "use <template>, <style>, <props>, <contract>, <script lang=\"json\"> or <import>");
        }
    }

    private void readProps(ElementNode e) {
        JsonValue json = json(e);
        if (json == null) return;
        PropsBlock.readProps(json, props, diagnostics);
    }

    private void readContract(ElementNode e) {
        JsonValue json = json(e);
        if (json != null) contract = ContractReader.read(json, diagnostics);
    }

    private void readScript(ElementNode e) {
        String lang = e.staticValue("lang");
        if (lang != null && !lang.equals("json")) {
            diagnostics.error(e.pos(), "<script lang=\"" + lang + "\"> is not supported", "vui has no scripting; use lang=\"json\" and a Java controller");
            return;
        }
        JsonValue json = json(e);
        if (json == null) return;
        JsonValue c = json.isObject() ? json.get("contract") : null;
        if (c != null) contract = ContractReader.read(c, diagnostics);
        PropsBlock.readScript(json, props, components, diagnostics);
    }

    private void readImport(ElementNode e) {
        String src = e.staticValue("src");
        if (src == null || src.isBlank()) {
            diagnostics.error(e.pos(), "<import> needs src", "e.g. <import name=\"card\" src=\"mymod:widgets/card\"/>");
            return;
        }
        String name = e.staticValue("name");
        if (name == null) name = src.substring(Math.max(src.lastIndexOf('/'), src.lastIndexOf(':')) + 1);
        components.put(name, src);
    }

    private JsonValue json(ElementNode e) {
        TextPart content = rawContent(e);
        if (content == null || content.text().isBlank()) return null;
        return PosJsonParser.parse(content.text(), content.pos(), diagnostics);
    }

    private static TextPart rawContent(ElementNode e) {
        if (e.children().isEmpty()) return null;
        TemplateNode first = e.children().get(0);
        if (!(first instanceof TextNode t) || t.parts().isEmpty()) return null;
        return t.parts().get(0);
    }

    private ComponentFile build() {
        return new ComponentFile(id, fileName, template, style.toString(),
                stylePos == null ? new SourcePos(fileName, 1, 1) : stylePos, props, components, diagnostics, contract);
    }
}
