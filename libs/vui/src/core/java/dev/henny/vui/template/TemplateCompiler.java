package dev.henny.vui.template;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.CompiledExpr;
import dev.henny.vui.expr.ExprCompiler;
import dev.henny.vui.markup.Attribute;
import dev.henny.vui.markup.ComponentFile;
import dev.henny.vui.markup.ElementNode;
import dev.henny.vui.markup.TemplateNode;
import dev.henny.vui.markup.TextNode;
import dev.henny.vui.markup.TextPart;
import dev.henny.vui.style.CssParser;
import dev.henny.vui.style.Declaration;
import dev.henny.vui.style.StyleSheet;
import dev.henny.vui.style.Theme;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TemplateCompiler {
    private final Diagnostics diagnostics;
    private final TagCatalog catalog;
    private final Map<String, String> components;

    private TemplateCompiler(Diagnostics diagnostics, TagCatalog catalog, Map<String, String> components) {
        this.diagnostics = diagnostics;
        this.catalog = catalog;
        this.components = components;
    }

    public static CompiledTemplate compile(ComponentFile file, Theme theme, TagCatalog catalog) {
        Diagnostics d = new Diagnostics().addAll(file.diagnostics());
        TemplateCompiler c = new TemplateCompiler(d, catalog, file.components());
        List<CNode> roots = c.nodes(file.template());
        StyleSheet styles = CssParser.parse(file.style(), file.stylePos(), theme, d);
        return new CompiledTemplate(file.id(), file.fileName(), roots, styles, file.props(), file.components(), d);
    }

    private List<CNode> nodes(List<TemplateNode> nodes) {
        List<CNode> out = new ArrayList<>();
        for (TemplateNode n : nodes) {
            if (n instanceof TextNode t) out.add(text(t));
            else out.add(element((ElementNode) n));
        }
        BranchCheck.check(out, diagnostics);
        return out;
    }

    private CText text(TextNode t) {
        List<Object> parts = new ArrayList<>();
        for (TextPart p : t.parts()) {
            parts.add(p.expression() ? ExprCompiler.expression(p.text(), p.pos(), diagnostics) : p.text());
        }
        return new CText(parts, t.pos());
    }

    private CElement element(ElementNode e) {
        checkTag(e);
        String id = null;
        String staticClass = null;
        List<Declaration> style = new ArrayList<>();
        Map<String, String> attrs = new LinkedHashMap<>();
        List<CBinding> bindings = new ArrayList<>();
        List<CEvent> events = new ArrayList<>();
        Branch branch = Branch.NONE;
        CompiledExpr condition = null;
        ForSpec forSpec = null;
        CompiledExpr key = null;
        CompiledExpr show = null;
        CModel model = null;
        String ref = null;
        SlotSpec slot = null;
        for (Attribute a : e.attributes()) {
            SourcePos vp = a.valuePos();
            switch (a.kind()) {
                case STATIC -> {
                    if (a.name().equals("id")) id = a.value();
                    else if (a.name().equals("class")) staticClass = a.value();
                    else if (a.name().equals("style")) style.addAll(CssParser.parseInline(nonNull(a.value()), vp, diagnostics));
                    else if (StyleAttributes.isStyle(a.name()) && a.value() != null) {
                        style.add(new Declaration(a.name(), a.value(), vp));
                        attrs.put(a.name(), a.value());
                    }
                    else attrs.put(a.name(), a.value() == null ? "true" : a.value());
                }
                case BIND -> {
                    CompiledExpr expr = ExprCompiler.expression(a.value(), vp, diagnostics);
                    if (a.name().equals("key")) key = expr;
                    else bindings.add(new CBinding(a.name(), expr));
                }
                case HTML -> bindings.add(new CBinding("text", ExprCompiler.expression(a.value(), vp, diagnostics)));
                case EVENT -> events.add(new CEvent(a.name(), a.modifiers(), ExprCompiler.handler(nonNull(a.value()), vp, diagnostics)));
                case IF -> {
                    branch = Branch.IF;
                    condition = ExprCompiler.expression(a.value(), vp, diagnostics);
                }
                case ELSE_IF -> {
                    branch = Branch.ELSE_IF;
                    condition = ExprCompiler.expression(a.value(), vp, diagnostics);
                }
                case ELSE -> branch = Branch.ELSE;
                case FOR -> forSpec = ForParser.parse(a.value(), vp, diagnostics);
                case SHOW -> show = ExprCompiler.expression(a.value(), vp, diagnostics);
                case MODEL -> model = model(a);
                case REF -> ref = a.value();
                case KEY -> key = ExprCompiler.constant(a.value(), vp);
                case SLOT -> slot = SlotSpec.parse(a.name().isEmpty() ? "default" : a.name(), a.value());
            }
        }
        List<CNode> children = nodes(e.children());
        return new CElement(e.tag(), e.pos(), id, staticClass, style, attrs, bindings, events, branch, condition,
                forSpec, key, show, model, ref, slot, children);
    }

    private CModel model(Attribute a) {
        CompiledExpr target = ExprCompiler.expression(a.value(), a.valuePos(), diagnostics);
        if (target.isValid() && !target.isAssignable()) {
            diagnostics.error(a.valuePos(), "v-model needs something writable, not '" + a.value() + "'", "use a name like 'volume' or a path like 'settings.volume'");
        }
        return new CModel(a.name(), a.modifiers(), target);
    }

    private void checkTag(ElementNode e) {
        String tag = e.tag();
        if (tag.equals("template") || tag.equals("slot") || components.containsKey(tag) || catalog.isBuiltIn(tag)) return;
        String hint = TagSuggestions.suggest(tag, catalog.builtInTags(), components.keySet());
        diagnostics.error(e.pos(), "unknown element <" + tag + ">", hint);
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
