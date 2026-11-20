package dev.henny.vui.style;

public enum PropKind {
    LENGTH, NUMBER, COLOR, KEYWORD, STRING, TIME, OBJECT;

    public boolean interpolates() {
        return this == LENGTH || this == NUMBER || this == COLOR;
    }
}
