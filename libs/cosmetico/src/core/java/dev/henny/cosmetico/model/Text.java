package dev.henny.cosmetico.model;

public record Text(String en, String de) {
    public static final Text EMPTY = new Text("", null);

    public String get(String language) {
        if ("de".equals(language) && de != null && !de.isEmpty()) return de;
        return en == null ? "" : en;
    }
}
