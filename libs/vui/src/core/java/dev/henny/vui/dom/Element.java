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

public class Element extends TreeNode implements StyleTarget {
    public static final String TEXT_TAG = "#text";

    private final String tag;
    private final Behavior behavior;
    private final SourcePos pos;
    private final ClassList classes = new ClassList();
    private final Map<String, Object> props = new HashMap<>();
    private final List<Handler> handlers = new ArrayList<>();
    private final List<Effect> effects = new ArrayList<>();
    private final ComputedStyle computed = ComputedStyle.initial();
    private final ComputedStyle used = ComputedStyle.initial();
    private final LayoutBox box = new LayoutBox();
    private final ElementFlags flags = new ElementFlags();
    private String id;
    private int states;
    private Object key;
    private String ref;
    private Scope scope;
    private TextContent text;
    private final InlineStyles inline = new InlineStyles();
    private StyleAnimator animator;
    private Object behaviorState;
    private final ScrollOffset scroll = new ScrollOffset();
    int siblingIndex = -1;

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
        markSubtreeStyleDirty();
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

    public float scrollX() {
        return scroll.x();
    }

    public float scrollY() {
        return scroll.y();
    }

    public void scrollTo(float x, float y) {
        if (scroll.clampTo(box, x, y) && behavior != null) behavior.scrolled(this);
    }

    public void restoreScroll(float x, float y) {
        scroll.set(x, y);
    }

    public List<Declaration> inlineStyle() {
        return inline.merged();
    }

    public void staticStyle(List<Declaration> declarations) {
        inline.fixed(declarations);
        markStyleDirty();
    }

    public void boundStyle(List<Declaration> declarations) {
        inline.bound(declarations);
        markStyleDirty();
    }

    public Object prop(String name) {
        return props.get(name);
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
        if (document != null && document.styles().statesAffectDescendants(state)) flags.styleSubtree = true;
    }

    public boolean disabled() {
        return has(StateFlags.DISABLED);
    }

    public void addHandler(Handler handler) {
        handlers.add(handler);
    }

    public void markStyleDirty() {
        flags.style = true;
        flags.childStyle = true;
        for (Element e = parent; e != null && !e.flags.childStyle; e = e.parent) e.flags.childStyle = true;
        if (document != null) document.invalidateStyle();
    }

    public void markSubtreeStyleDirty() {
        flags.styleSubtree = true;
        markStyleDirty();
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
    public StyleTarget stylePreviousSibling() {
        Element p = parent;
        int i = siblingIndex - 1;
        if (p == null || i < 0 || i >= p.childCount() || p.child(siblingIndex) != this) return null;
        return p.child(i);
    }

    @Override
    public String toString() {
        return tag + (id != null ? "#" + id : "") + " @" + pos;
    }
}
