package dev.henny.vui.dom;

import dev.henny.vui.anim.Animations;
import dev.henny.vui.anim.StyleAnimator;
import dev.henny.vui.style.ComputedStyle;
import dev.henny.vui.style.Prop;
import dev.henny.vui.style.StateFlags;
import dev.henny.vui.style.StyleResolver;
import dev.henny.vui.style.StyleSheet;
import dev.henny.vui.style.Theme;
import dev.henny.vui.style.TransitionSpec;
import dev.henny.vui.template.CompiledTemplate;

import java.util.ArrayList;
import java.util.List;

public final class DocumentStyles {
    private static final Prop[] PROPS = Prop.values();

    private final Document document;
    private final StyleResolver resolver;
    private final ComputedStyle scratch = ComputedStyle.initial();
    private final List<Element> animating = new ArrayList<>();
    private final ComponentSheets componentSheets = new ComponentSheets();
    private StyleSheet defaults;
    private StyleSheet own;
    private boolean dirty = true;
    private boolean enterRules;
    private boolean leaveRules;
    private int descendantStates;
    private boolean siblingRules;
    private int restyles;

    DocumentStyles(Document document, Theme theme) {
        this.document = document;
        this.resolver = new StyleResolver(theme, document.errors());
    }

    public StyleResolver resolver() {
        return resolver;
    }

    public int restyles() {
        return restyles;
    }

    void sheets(StyleSheet defaults, StyleSheet own) {
        this.defaults = defaults;
        this.own = own;
        componentSheets.clear();
        order();
    }

    void addComponentSheet(CompiledTemplate component) {
        if (!componentSheets.add(component)) return;
        order();
        invalidate();
    }

    private void order() {
        resolver.clearSheets();
        resolver.addSheet(defaults);
        for (CompiledTemplate t : componentSheets.importsFirst()) resolver.addSheet(t.styles());
        resolver.addSheet(own);
        scanRules();
    }

    public boolean hasEnterRules() {
        return enterRules;
    }

    boolean hasLeaveRules() {
        return leaveRules;
    }

    void invalidate() {
        dirty = true;
    }

    boolean dirty() {
        return dirty;
    }

    void pass(Element root) {
        if (!dirty) return;
        dirty = false;
        visit(root, null, false);
    }

    private void visit(Element e, ComputedStyle parentStyle, boolean force) {
        boolean restyle = force || e.flags().style;
        boolean cascade = false;
        if (restyle) cascade = restyle(e, parentStyle) || e.flags().styleSubtree;
        if (cascade || e.flags().childStyle) {
            boolean after = false;
            for (int i = 0; i < e.childCount(); i++) {
                Element c = e.child(i);
                if (after) c.flags().styleSubtree = true;
                boolean changed = cascade || after || c.flags().style;
                visit(c, e.style(), cascade || after);
                after |= siblingRules && changed;
            }
        }
        e.flags().style = false;
        e.flags().childStyle = false;
        e.flags().styleSubtree = false;
    }

    public boolean usesSiblings() {
        return siblingRules;
    }

    public boolean statesAffectDescendants(int states) {
        return (descendantStates & states) != 0;
    }

    private boolean restyle(Element e, ComputedStyle parentStyle) {
        restyles++;
        resolver.resolve(e, parentStyle, e.inlineStyle(), scratch);
        ComputedStyle computed = e.computed();
        ComputedStyle used = e.style();
        boolean first = !e.flags().styledOnce;
        boolean layout = first;
        boolean inherited = first;
        Object transitions = scratch.object(Prop.TRANSITION);
        for (Prop p : PROPS) {
            if (!first && computed.sameAs(p, scratch)) continue;
            if (p.affectsLayout()) layout = true;
            if (p.inherited()) inherited = true;
            computed.copy(p, scratch);
            TransitionSpec spec = first ? null : find(transitions, p);
            if (spec != null && Animations.enabled() && spec.duration() > 0) {
                e.animatorOrCreate().transition(p, spec);
                track(e);
            } else {
                used.copy(p, scratch);
                if (e.animator() != null) e.animator().cancel(p);
            }
        }
        e.flags().styledOnce = true;
        Object animation = computed.object(Prop.ANIMATION);
        if (animation instanceof List<?> list && !list.isEmpty() || e.animator() != null) {
            StyleAnimator a = e.animatorOrCreate();
            a.animations(animation, resolver);
            if (a.running()) track(e);
        }
        if (layout) {
            document.invalidateLayout();
            if (e.text() != null) e.text().invalidate();
            if (e.parent() != null && e.parent().text() != null) e.parent().text().invalidate();
        }
        return inherited;
    }

    private static TransitionSpec find(Object transitions, Prop p) {
        if (!(transitions instanceof List<?> list)) return null;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) instanceof TransitionSpec t && t.covers(p)) return t;
        }
        return null;
    }

    private void track(Element e) {
        if (!animating.contains(e)) animating.add(e);
    }

    boolean animate(float dt) {
        boolean any = false;
        for (int i = animating.size() - 1; i >= 0; i--) {
            Element e = animating.get(i);
            StyleAnimator a = e.animator();
            if (a == null || e.flags().disposed) {
                animating.remove(i);
                continue;
            }
            if (a.step(dt, resolver, document.errors())) document.invalidateLayout();
            if (a.inheritedAnimating()) {
                for (int c = 0; c < e.childCount(); c++) e.child(c).markStyleDirty();
            }
            if (!a.running()) animating.remove(i);
            any = true;
        }
        return any;
    }

    boolean isAnimating(Element e) {
        return animating.contains(e) && e.animator() != null && e.animator().running();
    }

    int animatingCount() {
        return animating.size();
    }

    void forget(Element e) {
        animating.remove(e);
    }

    private void scanRules() {
        enterRules = false;
        leaveRules = false;
        descendantStates = 0;
        siblingRules = false;
        for (StyleSheet s : resolver.sheets()) {
            for (dev.henny.vui.style.Rule r : s.rules()) {
                descendantStates |= r.selector().stateMaskOfAncestors();
                siblingRules |= r.selector().usesSiblings();
                int mask = r.selector().stateMaskOfSubject();
                if ((mask & StateFlags.ENTER) != 0) enterRules = true;
                if ((mask & StateFlags.LEAVE) != 0) leaveRules = true;
            }
        }
    }
}
