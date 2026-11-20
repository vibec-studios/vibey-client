package dev.henny.vui.style;

import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.SourcePos;

import java.util.List;

public final class Declaration {
    private static final Prop[] NO_PROPS = new Prop[0];
    private static final StyleValue[] NO_VALUES = new StyleValue[0];

    private final String property;
    private final String raw;
    private final SourcePos pos;
    private Prop[] props = NO_PROPS;
    private StyleValue[] values = NO_VALUES;
    private int resolvedFor = -1;

    public Declaration(String property, String raw, SourcePos pos) {
        this.property = PropertyNames.canonical(property);
        this.raw = raw.trim();
        this.pos = pos;
    }

    public String property() {
        return property;
    }

    public String raw() {
        return raw;
    }

    public SourcePos pos() {
        return pos;
    }

    public boolean isKnownProperty() {
        return PropertyNames.isKnown(property);
    }

    public int size() {
        return props.length;
    }

    public Prop prop(int i) {
        return props[i];
    }

    public StyleValue value(int i) {
        return values[i];
    }

    public boolean resolve(Theme theme, ErrorSink errors) {
        if (resolvedFor == theme.version()) return props.length > 0;
        resolvedFor = theme.version();
        props = NO_PROPS;
        values = NO_VALUES;
        try {
            String value = theme.substitute(raw);
            Prop single = Prop.byName(property);
            if (Shorthands.isShorthand(property)) {
                expand(value);
            } else if (single != null) {
                props = new Prop[]{single};
                values = new StyleValue[]{ValueParser.parse(single, value)};
            } else {
                String suggestion = PropertyNames.suggest(property);
                errors.report(pos, "unknown property '" + property + "'", suggestion == null ? null : "did you mean '" + suggestion + "'?");
            }
        } catch (StyleError e) {
            errors.report(pos, e.getMessage() + " (" + property + ": " + raw + ")", e.hint());
            props = NO_PROPS;
            values = NO_VALUES;
        }
        return props.length > 0;
    }

    private void expand(String value) {
        List<Shorthands.Longhand> longhands = Shorthands.expand(property, value);
        Prop[] p = new Prop[longhands.size()];
        StyleValue[] v = new StyleValue[longhands.size()];
        for (int i = 0; i < p.length; i++) {
            p[i] = longhands.get(i).prop();
            v[i] = ValueParser.parse(p[i], longhands.get(i).value());
        }
        props = p;
        values = v;
    }

    @Override
    public String toString() {
        return property + ": " + raw;
    }
}
