package dev.henny.vibeyclient.access;

public final class AccessFailure extends RuntimeException {
    public enum Kind { NO_LOGIN, UNREACHABLE, DENIED }

    public final Kind kind;
    public final String code;

    public AccessFailure(Kind kind, String code, String message) {
        super(message, null, false, false);
        this.kind = kind;
        this.code = code;
    }

    public static AccessFailure of(Throwable t) {
        Throwable c = t;
        while (c != null && !(c instanceof AccessFailure) && c.getCause() != null && c.getCause() != c) c = c.getCause();
        if (c instanceof AccessFailure f) return f;
        return new AccessFailure(Kind.UNREACHABLE, "network", t == null ? "?" : String.valueOf(t.getMessage()));
    }
}
