package dev.henny.vui.dom;

import dev.henny.vui.diag.Diagnostic;
import dev.henny.vui.diag.Diagnostics;
import dev.henny.vui.diag.ErrorSink;
import dev.henny.vui.diag.Severity;
import dev.henny.vui.diag.SourcePos;

import java.util.HashSet;
import java.util.Set;

public final class RuntimeErrors implements ErrorSink {
    private static final int LIMIT = 200;

    private final Diagnostics diagnostics = new Diagnostics();
    private final Set<String> seen = new HashSet<>();
    private int version;

    @Override
    public void report(SourcePos pos, String message, String hint) {
        if (diagnostics.size() >= LIMIT) return;
        String key = pos + "|" + message;
        if (!seen.add(key)) return;
        diagnostics.add(new Diagnostic(Severity.ERROR, pos, message, hint));
        version++;
    }

    public Diagnostics diagnostics() {
        return diagnostics;
    }

    public int version() {
        return version;
    }

    public void touch() {
        version++;
    }

    public void clear() {
        diagnostics.clear();
        seen.clear();
        version++;
    }
}
