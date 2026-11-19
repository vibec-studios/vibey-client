package dev.henny.vui.diag;

@FunctionalInterface
public interface ErrorSink {
    ErrorSink IGNORE = (pos, message, hint) -> {
    };

    void report(SourcePos pos, String message, String hint);
}
