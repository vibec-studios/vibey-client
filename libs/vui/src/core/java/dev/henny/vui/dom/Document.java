package dev.henny.vui.dom;

import dev.henny.vui.Vui;
import dev.henny.vui.bind.Scheduler;
import dev.henny.vui.contract.ContractCheck;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.event.EventSystem;
import dev.henny.vui.expr.ControllerScope;
import dev.henny.vui.layout.LayoutEngine;
import dev.henny.vui.layout.Size;
import dev.henny.vui.render.Canvas;
import dev.henny.vui.template.CompiledTemplate;

import java.util.List;

public final class Document {
    private final Vui vui;
    private final Object controller;
    private final RuntimeErrors errors = new RuntimeErrors();
    private final Scheduler scheduler = new Scheduler();
    private final DocumentEffects effects;
    private final DocumentStyles styles;
    private final Overlays overlays = new Overlays(this);
    private final MountRegistry mounts = new MountRegistry();
    private final DocumentHooks hooks = new DocumentHooks(this);
    private final EventSystem events;
    private final LayoutEngine layout;
    private final DocumentRenderer renderer;
    private final ControllerScope rootScope;
    private CompiledTemplate template;
    private Element root;
    private float width;
    private float height;
    private float time;
    private boolean layoutDirty = true;
    private boolean mounted;
    private Runnable escapeHandler;
    private Diagnostics reloadErrors = new Diagnostics();
    private Diagnostics contractWarnings = new Diagnostics();
    private String documentId;

    public Document(Vui vui, CompiledTemplate template, Object controller) {
        this.vui = vui;
        this.controller = controller;
        this.template = template;
        this.effects = new DocumentEffects(scheduler);
        this.styles = new DocumentStyles(this, vui.theme());
        this.events = new EventSystem(this);
        this.layout = new LayoutEngine(vui.backend().text());
        this.renderer = new DocumentRenderer(this);
        this.rootScope = new ControllerScope(controller, vui.functions());
        build();
    }

    private void build() {
        styles.resolver().theme(vui.theme());
        styles.sheets(vui.defaultStyles(), template.styles());
        root = new Element("root", vui.behaviors().get("root"), new SourcePos(template.fileName(), 1, 1));
        root.attach(this);
        new DocumentBuilder(this).build(root, template);
        mounted = true;
        Lifecycle.mount(root);
        invalidateStyle();
        invalidateLayout();
        contractWarnings = vui.devMode() ? checkContracts(true) : new Diagnostics();
    }

    public Diagnostics checkContracts(boolean actions) {
        Diagnostics out = new Diagnostics();
        ContractCheck.check(template.contract(), controller, actions, out);
        for (CompiledTemplate c : mounts.components()) ContractCheck.check(c.contract(), controller, actions, out);
        return out;
    }

    public void swap(CompiledTemplate next) {
        reloadErrors = new Diagnostics();
        TreeState saved = TreeState.capture(this);
        disposeTree();
        template = next;
        mounted = false;
        build();
        frame(0);
        saved.restore(this);
    }

    public void reloadFailed(Diagnostics diagnostics) {
        reloadErrors = diagnostics;
        errors.touch();
    }

    private void disposeTree() {
        events.focus().blur();
        events.tooltips().reset();
        Lifecycle.dispose(root);
        mounts.clear();
        errors.clear();
    }

    public void dispose() {
        disposeTree();
    }

    public void resize(float w, float h) {
        if (w == width && h == height) return;
        width = w;
        height = h;
        invalidateLayout();
    }

    public void frame(float dt) {
        time += dt;
        scheduler.flush();
        styles.pass(root);
        if (mounts.finishEntering()) styles.pass(root);
        if (styles.animate(dt)) styles.pass(root);
        mounts.updateLeaving();
        mounts.runFramed(dt);
        scheduler.flush();
        events.tooltips().frame(dt);
        layoutIfNeeded();
    }

    public void layoutIfNeeded() {
        if (!layoutDirty) return;
        styles.pass(root);
        layoutDirty = false;
        layout.layout(root, width, height);
        overlays.layout(layout, width, height);
    }

    public Size measureContent(float maxWidth, float maxHeight) {
        styles.pass(root);
        layout.beginPass(maxWidth, maxHeight);
        layout.measure(root, maxWidth, maxHeight);
        return new Size().set(root.box().measuredWidth, root.box().measuredHeight);
    }

    public void render(Canvas canvas) {
        layoutIfNeeded();
        renderer.render(canvas);
    }

    public boolean pointerMove(float x, float y) {
        return events.pointerMove(x, y, 0);
    }

    public boolean pointerMove(float x, float y, int mods) {
        return events.pointerMove(x, y, mods);
    }

    public boolean pointerDown(float x, float y, int button, int mods) {
        return events.pointerDown(x, y, button, mods);
    }

    public boolean pointerUp(float x, float y, int button, int mods) {
        return events.pointerUp(x, y, button, mods);
    }

    public boolean scroll(float x, float y, float dx, float dy, int mods) {
        return events.scroll(x, y, dx, dy, mods);
    }

    public boolean keyDown(int key, int scancode, int mods) {
        return events.keyDown(key, scancode, mods, false);
    }

    public boolean keyUp(int key, int scancode, int mods) {
        return events.keyUp(key, scancode, mods);
    }

    public boolean charTyped(int codepoint, int mods) {
        return events.charTyped(codepoint, mods);
    }

    public void refresh() {
        effects.refresh();
        scheduler.flush();
    }

    public void afterHandler() {
        effects.refresh();
    }

    public Element find(String id) {
        return Queries.byId(root, id);
    }

    public List<Element> query(String selector) {
        return Queries.select(root, selector);
    }

    public Element ref(String name) {
        return mounts.refs().get(name);
    }

    public void onEscape(Runnable handler) {
        escapeHandler = handler;
    }

    public boolean requestEscape() {
        if (escapeHandler == null) return false;
        escapeHandler.run();
        return true;
    }

    public Diagnostics diagnostics() {
        Diagnostics all = new Diagnostics().addAll(reloadErrors).addAll(contractWarnings);
        all.addAll(template.diagnostics());
        for (CompiledTemplate c : mounts.components()) all.addAll(c.diagnostics());
        all.addAll(errors.diagnostics());
        return all;
    }

    public String documentId() {
        return documentId != null ? documentId : template.id();
    }

    public void documentId(String id) {
        documentId = id;
    }

    public Vui vui() {
        return vui;
    }

    public Object controller() {
        return controller;
    }

    public ControllerScope rootScope() {
        return rootScope;
    }

    public CompiledTemplate template() {
        return template;
    }

    public Element root() {
        return root;
    }

    public EventSystem events() {
        return events;
    }

    public Overlays overlays() {
        return overlays;
    }

    public DocumentStyles styles() {
        return styles;
    }

    public DocumentEffects effects() {
        return effects;
    }

    public LayoutEngine layoutEngine() {
        return layout;
    }

    public RuntimeErrors errors() {
        return errors;
    }

    public float width() {
        return width;
    }

    public float height() {
        return height;
    }

    public float time() {
        return time;
    }

    public boolean isMounted() {
        return mounted;
    }

    public boolean isLayoutDirty() {
        return layoutDirty;
    }

    public boolean hasLeaveRules() {
        return styles.hasLeaveRules();
    }

    public void invalidateStyle() {
        styles.invalidate();
    }

    public void invalidateLayout() {
        layoutDirty = true;
    }

    public void leaving(Element e, ChildSource owner) {
        mounts.leaving(e, owner, styles);
    }

    public void wantsFrames(Element e, boolean on) {
        mounts.framed(e, on);
    }

    DocumentHooks hooks() {
        return hooks;
    }

    MountRegistry mounts() {
        return mounts;
    }
}
