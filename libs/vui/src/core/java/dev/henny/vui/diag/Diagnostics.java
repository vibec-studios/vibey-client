package dev.henny.vui.diag;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class Diagnostics {
    private final List<Diagnostic> entries = new ArrayList<>();

    public Diagnostics error(SourcePos pos, String message, String hint) {
        return add(new Diagnostic(Severity.ERROR, pos, message, hint));
    }

    public Diagnostics error(SourcePos pos, String message) {
        return error(pos, message, null);
    }

    public Diagnostics warn(SourcePos pos, String message, String hint) {
        return add(new Diagnostic(Severity.WARNING, pos, message, hint));
    }

    public Diagnostics warn(SourcePos pos, String message) {
        return warn(pos, message, null);
    }

    public Diagnostics add(Diagnostic diagnostic) {
        entries.add(diagnostic);
        return this;
    }

    public Diagnostics addAll(Diagnostics other) {
        if (other != null && other != this) entries.addAll(other.entries);
        return this;
    }

    public boolean hasErrors() {
        for (Diagnostic d : entries) if (d.isError()) return true;
        return false;
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public List<Diagnostic> all() {
        return Collections.unmodifiableList(entries);
    }

    public List<Diagnostic> errors() {
        List<Diagnostic> out = new ArrayList<>();
        for (Diagnostic d : entries) if (d.isError()) out.add(d);
        return out;
    }

    public void clear() {
        entries.clear();
    }

    public String format() {
        StringBuilder sb = new StringBuilder();
        for (Diagnostic d : entries) sb.append(d.format()).append('\n');
        return sb.toString();
    }

    @Override
    public String toString() {
        return format();
    }
}
