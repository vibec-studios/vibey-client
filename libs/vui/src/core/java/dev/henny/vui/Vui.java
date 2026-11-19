package dev.henny.vui;

import dev.henny.vui.component.Behaviors;
import dev.henny.vui.component.DefaultStyles;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.dom.Document;
import dev.henny.vui.expr.Functions;
import dev.henny.vui.markup.ComponentFile;
import dev.henny.vui.markup.ComponentLoader;
import dev.henny.vui.reload.DocumentRegistry;
import dev.henny.vui.reload.HotReloader;
import dev.henny.vui.render.Backend;
import dev.henny.vui.style.CssParser;
import dev.henny.vui.style.StyleSheet;
import dev.henny.vui.style.Theme;
import dev.henny.vui.style.VibeTheme;
import dev.henny.vui.template.CompiledTemplate;
import dev.henny.vui.template.TemplateCompiler;

import java.util.ArrayList;
import java.util.List;

public final class Vui {
    private final Backend backend;
    private final Functions functions = new Functions();
    private final Behaviors behaviors = Behaviors.standard();
    private final DocumentRegistry registry = new DocumentRegistry(this);
    private final HotReloader reloader = new HotReloader(registry);
    private final List<Runnable> tickTasks = new ArrayList<>();
    private Theme theme = VibeTheme.create();
    private StyleSheet defaultStyles;
    private Diagnostics defaultDiagnostics = new Diagnostics();
    private boolean dev = Boolean.getBoolean("vui.dev");
    private boolean errorOverlay = true;

    public Vui(Backend backend) {
        this.backend = backend;
        compileDefaults();
    }

    public Backend backend() {
        return backend;
    }

    public Functions functions() {
        return functions;
    }

    public Behaviors behaviors() {
        return behaviors;
    }

    public DocumentRegistry registry() {
        return registry;
    }

    public HotReloader reloader() {
        return reloader;
    }

    public Theme theme() {
        return theme;
    }

    public Vui theme(Theme next) {
        theme = next;
        compileDefaults();
        return this;
    }

    public StyleSheet defaultStyles() {
        return defaultStyles;
    }

    public Diagnostics defaultStyleDiagnostics() {
        return defaultDiagnostics;
    }

    public boolean devMode() {
        return dev;
    }

    public Vui devMode(boolean on) {
        dev = on;
        return this;
    }

    public boolean showErrorOverlay() {
        return errorOverlay;
    }

    public Vui showErrorOverlay(boolean on) {
        errorOverlay = on;
        return this;
    }

    public CompiledTemplate compile(ComponentFile file) {
        return TemplateCompiler.compile(file, theme, behaviors);
    }

    public CompiledTemplate compile(String id, String fileName, String source) {
        return compile(ComponentLoader.load(id, fileName, source));
    }

    public CompiledTemplate component(String id) {
        return registry.template(id);
    }

    public Document open(String id, Object controller) {
        Document doc = new Document(this, registry.template(id), controller);
        doc.documentId(id);
        reloader.track(doc);
        return doc;
    }

    public Document create(CompiledTemplate template, Object controller) {
        return new Document(this, template, controller);
    }

    public Document fromSource(String fileName, String source, Object controller) {
        return create(compile("inline:" + fileName, fileName, source), controller);
    }

    public void close(Document doc) {
        reloader.untrack(doc);
        doc.dispose();
    }

    public void onTick(Runnable task) {
        tickTasks.add(task);
    }

    public void tick() {
        if (dev) reloader.poll();
        for (int i = 0; i < tickTasks.size(); i++) tickTasks.get(i).run();
    }

    private void compileDefaults() {
        defaultDiagnostics = new Diagnostics();
        defaultStyles = CssParser.parse(DefaultStyles.css(), new SourcePos("<vui:defaults>", 1, 1), theme, defaultDiagnostics);
    }
}
