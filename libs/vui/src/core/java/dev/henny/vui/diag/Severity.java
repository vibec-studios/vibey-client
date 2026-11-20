package dev.henny.vui.diag;

public enum Severity {
    ERROR, WARNING, INFO;

    public String label() {
        return name().toLowerCase();
    }
}
