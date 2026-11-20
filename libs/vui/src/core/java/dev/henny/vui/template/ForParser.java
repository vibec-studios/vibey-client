package dev.henny.vui.template;

import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.SourcePos;
import dev.henny.vui.expr.ExprCompiler;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ForParser {
    private static final Pattern FORM = Pattern.compile("^\\s*(\\(([^)]*)\\)|([A-Za-z_$][\\w$]*))\\s+(in|of)\\s+(.+)$", Pattern.DOTALL);
    private static final Pattern NAME = Pattern.compile("[A-Za-z_$][\\w$]*");

    private ForParser() {
    }

    public static ForSpec parse(String value, SourcePos pos, Diagnostics diagnostics) {
        if (value == null) {
            diagnostics.error(pos, "v-for needs a value", "e.g. v-for=\"item in items\"");
            return null;
        }
        Matcher m = FORM.matcher(value);
        if (!m.matches()) {
            diagnostics.error(pos, "cannot read v-for=\"" + value + "\"", "use 'item in items' or '(item, index) in items'");
            return null;
        }
        String[] names = m.group(2) != null ? m.group(2).split(",") : new String[]{m.group(3)};
        for (int i = 0; i < names.length; i++) {
            names[i] = names[i].trim();
            if (!NAME.matcher(names[i]).matches()) {
                diagnostics.error(pos, "'" + names[i] + "' is not a valid name in v-for", null);
                return null;
            }
        }
        if (names.length > 3) {
            diagnostics.error(pos, "v-for takes at most (item, key, index)", null);
            return null;
        }
        int sourceOffset = m.start(5);
        String item = names[0];
        String index = names.length == 2 ? names[1] : names.length == 3 ? names[2] : null;
        String key = names.length == 3 ? names[1] : null;
        return new ForSpec(item, index, key, ExprCompiler.expression(m.group(5).trim(), pos.shift(sourceOffset), diagnostics));
    }
}
