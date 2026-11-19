package dev.henny.vui.diag;

public record Diagnostic(Severity severity, SourcePos pos, String message, String hint) {

    public boolean isError() {
        return severity == Severity.ERROR;
    }

    public String format() {
        String base = pos + ": " + severity.label() + ": " + message;
        return hint == null || hint.isEmpty() ? base : base + " (hint: " + hint + ")";
    }

    @Override
    public String toString() {
        return format();
    }
}
