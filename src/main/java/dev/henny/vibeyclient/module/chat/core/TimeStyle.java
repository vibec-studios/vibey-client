package dev.henny.vibeyclient.module.chat.core;

public enum TimeStyle {
    H24("HH:mm"),
    H24_SECONDS("HH:mm:ss"),
    H12("h:mm a");

    private final String pattern;

    TimeStyle(String pattern) {
        this.pattern = pattern;
    }

    public String pattern() {
        return pattern;
    }
}
