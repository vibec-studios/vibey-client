package dev.henny.vibeyclient.module.chat.filters;

public final class FilterRow {
    public final String id;
    public final String text;
    public final boolean regex;

    FilterRow(String text) {
        this.id = text;
        this.text = text;
        this.regex = text.length() > 2 && text.startsWith("/") && text.endsWith("/");
    }
}
