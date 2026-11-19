package dev.henny.vui.dom;

import dev.henny.vui.anim.StyleAnimator;
import dev.henny.vui.bind.Effect;
import dev.henny.vui.component.Behavior;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.event.Handler;
import dev.henny.vui.expr.Scope;
import dev.henny.vui.layout.LayoutBox;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Declaration;
import dev.henny.vui.style.StateFlags;
import dev.henny.vui.style.StyleTarget;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Element implements StyleTarget {
    public static final String TEXT_TAG = "#text";

    private final String tag;
    private final Behavior behavior;
    private final SourcePos pos;
    private final ArrayList<Element> children = new ArrayList<>();
    private final List<ChildSource> sources = new ArrayList<>();
    private final ClassList classes = new ClassList();
    private final Map<String, Object> props = new HashMap<>();
    private final List<Handler> handlers = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final ComputedStyle computed = ComputedStyle.initial();
    private final ComputedStyle used = ComputedStyle.initial();
    private final LayoutBox box = new LayoutBox();
    private final ElementFlags flags = new ElementFlags();
    private Document document;
    private Element parent;
    private String id;
    private int states;
    private Object key;
    private String ref;
    private Scope scope;
    private TextContent text;
    private List<Declaration> staticStyle = List.of();
    private List<Declaration> boundStyle = List.of();
    private List<Declaration> inlineStyle = List.of();
    private StyleAnimator animator;
    private Object behaviorState;
    private ChildSource owner;
    private float scrollX;
    private float scrollY;

    public Element(String tag, Behavior behavior, SourcePos pos) {
        this.tag = tag;
        this.behavior = behavior;
        this.pos = pos == null ? SourcePos.UNKNOWN : pos;
        if (tag.equals("text")) this.text = new TextContent(this);
    }

    public String tag() {
        return tag;
    }

    public Behavior behavior() {
        return behavior;
    }

    public SourcePos pos() {
        return pos;
    }

    public Document document() {
        return document;
    }

    public Element parent() {
        return parent;
    }

    public List<Element> children() {
        return children;
    }

    public int childCount() {
        return children.size();
    }

    public Element child(int i) {
        return children.get(i);
    }

    public List<ChildSource> sources() {
        return sources;
    }

    public ClassList classes() {
        return classes;
    }

    public Map<String, Object> props() {
        return props;
    }

    public List<Handler> handlers() {
        return handlers;
    }

    public List<Effect> effects() {
        return effects;
    }

    public ComputedStyle computed() {
        return computed;
    }

    public ComputedStyle style() {
        return used;
    }

    public LayoutBox box() {
        return box;
    }

    public ElementFlags flags() {
        return flags;
    }

    public String id() {
        return id;
    }

    public void id(String value) {
        if (java.util.Objects.equals(id, value)) return;
        id = value;
        markStyleDirty();
    }

    public Object key() {
        return key;
    }

    public void key(Object value) {
        key = value;
    }

    public String ref() {
        return ref;
    }

    public void ref(String value) {
        ref = value;
    }

    public Scope scope() {
        return scope;
    }

    public void scope(Scope value) {
        scope = value;
    }

    public TextContent text() {
        return text;
    }

    public void text(TextContent value) {
        text = value;
    }

    public StyleAnimator animator() {
        return animator;
    }

    public StyleAnimator animatorOrCreate() {
        if (animator == null) animator = new StyleAnimator(this);
        return animator;
    }

    @SuppressWarnings("unchecked")
    public <T> T behaviorState() {
        return (T) behaviorState;
    }

    public void behaviorState(Object value) {
        behaviorState = value;
    }

    public ChildSource owner() {
        return owner;
    }

    public void owner(ChildSource value) {
        owner = value;
    }

    public float scrollX() {
        return scrollX;
    }

    public float scrollY() {
        return scrollY;
    }

    public void scrollTo(float x, float y) {
        float maxX = Math.max(0, box.contentWidth - box.innerWidth());
        float maxY = Math.max(0, box.contentHeight - box.innerHeight());
        float nx = Math.max(0, Math.min(maxX, x));
        float ny = Math.max(0, Math.min(maxY, y));
        if (nx == scrollX && ny == scrollY) return;
        scrollX = nx;
        scrollY = ny;
        if (behavior != null) behavior.scrolled(this);
    }

    public void restoreScroll(float x, float y) {
        scrollX = x;
        scrollY = y;
    }

    public List<Declaration> inlineStyle() {
        return inlineStyle;
    }

    public void staticStyle(List<Declaration> declarations) {
        staticStyle = declarations;
        mergeInline();
    }

    public void boundStyle(List<Declaration> declarations) {
        boundStyle = declarations;
        mergeInline();
    }

    private void mergeInline() {
        if (boundStyle.isEmpty()) inlineStyle = staticStyle;
        else if (staticStyle.isEmpty()) inlineStyle = boundStyle;
        else {
            List<Declaration> all = new ArrayList<>(staticStyle);
            all.addAll(boundStyle);
            inlineStyle = all;
        }
        markStyleDirty();
    }

    public Object prop(String name) {
        return props.get(name);
    }

    public boolean hasProp(String name) {
        return props.containsKey(name);
    }

    public void prop(String name, Object value) {
        boolean had = props.containsKey(name);
        Object old = props.put(name, value);
        if (had && java.util.Objects.equals(old, value) && isPlainValue(value)) return;
        ElementProps.applied(this, name, value);
    }

    private static boolean isPlainValue(Object v) {
        return v == null || v instanceof CharSequence || v instanceof Number || v instanceof Boolean || v instanceof Enum<?>;
    }

    public boolean has(int state) {
        return (states & state) != 0;
    }

    public void state(int state, boolean on) {
        int next = on ? states | state : states & ~state;
        if (next == states) return;
        states = next;
        markStyleDirty();
        flags.styleSubtree = true;
    }

    public boolean disabled() {
        return has(StateFlags.DISABLED);
    }

    public void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public void attach(Document doc) {
        document = doc;
        for (int i = 0; i < children.size(); i++) children.get(i).attach(doc);
    }

    void parent(Element p) {
        parent = p;
    }

    public void appendChild(Element child) {
        sources.add(new StaticChild(child));
        rebuildChildren();
    }

    public void removeAllChildren() {
        for (ChildSource s : sources) s.dispose();
        sources.clear();
        rebuildChildren();
    }

    public void rebuildChildren() {
        ChildListBuilder.rebuild(this);
    }

    public void markStyleDirty() {
        flags.style = true;
        for (Element e = this; e != null && !e.flags.childStyle; e = e.parent) e.flags.childStyle = true;
        if (document != null) document.invalidateStyle();
    }

    public void markLayoutDirty() {
        if (document != null) document.invalidateLayout();
    }

    public boolean isVisibleInTree() {
        for (Element e = this; e != null; e = e.parent) if (!e.flags.shown || e.style().is(dev.henny.vui.style.Prop.DISPLAY, "none")) return false;
        return true;
    }

    public void click() {
        if (document != null) document.events().click(this);
    }

    public void emit(String type, Object detail) {
        if (document != null) document.events().emit(this, type, detail);
    }

    @Override
    public String styleTag() {
        return tag;
    }

    @Override
    public String styleId() {
        return id;
    }

    @Override
    public boolean hasClass(String name) {
        return classes.has(name);
    }

    @Override
    public int states() {
        return states;
    }

    @Override
    public StyleTarget styleParent() {
        return parent;
    }

    @Override
    public String toString() {
        return tag + (id != null ? "#" + id : "") + " @" + pos;
    }
}
