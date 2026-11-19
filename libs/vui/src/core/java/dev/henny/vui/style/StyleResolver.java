package dev.henny.vui.style;

import dev.henny.vui.diag.ErrorSink;

import java.util.ArrayList;
import java.util.List;

public final class StyleResolver {
    private static final Prop[] PROPS = Prop.values();

    private final List<StyleSheet> sheets = new ArrayList<>();
    private Theme theme;
    private ErrorSink errors;
    private Rule[] matched = new Rule[32];
    private int[] ranks = new int[32];
    private int count;

    public StyleResolver(Theme theme, ErrorSink errors) {
        this.theme = theme;
        this.errors = errors;
    }

    public StyleResolver addSheet(StyleSheet sheet) {
        if (sheet != null) sheets.add(sheet);
        return this;
    }

    public void clearSheets() {
        sheets.clear();
    }

    public List<StyleSheet> sheets() {
        return sheets;
    }

    public Theme theme() {
        return theme;
    }

    public void theme(Theme next) {
        this.theme = next;
    }

    public void errors(ErrorSink sink) {
        this.errors = sink;
    }

    public Keyframes keyframes(String name) {
        for (int i = sheets.size() - 1; i >= 0; i--) {
            Keyframes k = sheets.get(i).keyframes(name);
            if (k != null) return k;
        }
        return null;
    }

    public void resolve(StyleTarget target, ComputedStyle parent, List<Declaration> inline, ComputedStyle out) {
        inheritOrReset(parent, out);
        collect(target);
        for (int i = 0; i < count; i++) apply(matched[i].declarations(), parent, out);
        if (inline != null) apply(inline, parent, out);
        for (int i = 0; i < count; i++) matched[i] = null;
    }

    public int matchedCount() {
        return count;
    }

    public List<Rule> matchingRules(StyleTarget target) {
        collect(target);
        List<Rule> out = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            out.add(matched[i]);
            matched[i] = null;
        }
        return out;
    }

    private void inheritOrReset(ComputedStyle parent, ComputedStyle out) {
        for (Prop p : PROPS) {
            if (p.inherited() && parent != null) out.copy(p, parent);
            else out.reset(p);
        }
    }

    private void collect(StyleTarget target) {
        count = 0;
        for (int s = 0; s < sheets.size(); s++) {
            List<Rule> rules = sheets.get(s).rules();
            for (int r = 0; r < rules.size(); r++) {
                Rule rule = rules.get(r);
                if (rule.selector().matches(target)) insert(rule, (s << 20) | rule.order());
            }
        }
    }

    private void insert(Rule rule, int rank) {
        if (count == matched.length) grow();
        int i = count++;
        int spec = rule.specificity();
        while (i > 0 && (matched[i - 1].specificity() > spec || matched[i - 1].specificity() == spec && ranks[i - 1] > rank)) {
            matched[i] = matched[i - 1];
            ranks[i] = ranks[i - 1];
            i--;
        }
        matched[i] = rule;
        ranks[i] = rank;
    }

    private void grow() {
        Rule[] m = new Rule[matched.length * 2];
        int[] r = new int[ranks.length * 2];
        System.arraycopy(matched, 0, m, 0, count);
        System.arraycopy(ranks, 0, r, 0, count);
        matched = m;
        ranks = r;
    }

    private void apply(List<Declaration> declarations, ComputedStyle parent, ComputedStyle out) {
        for (int d = 0; d < declarations.size(); d++) {
            Declaration decl = declarations.get(d);
            if (!decl.resolve(theme, errors)) continue;
            for (int i = 0; i < decl.size(); i++) {
                Prop p = decl.prop(i);
                StyleValue v = decl.value(i);
                if (v.isInherit()) {
                    if (parent != null) out.copy(p, parent);
                    else out.reset(p);
                } else if (v.isInitial()) {
                    out.reset(p);
                } else {
                    out.set(p, v);
                }
            }
        }
    }
}
