package dev.henny.vui.reload;

public record DocumentId(String namespace, String path) {
    public static final String MARKUP = ".vui";
    public static final String JSON = ".vui.json";

    public static DocumentId parse(String id) {
        int colon = id.indexOf(':');
        if (colon <= 0 || colon == id.length() - 1) throw new IllegalArgumentException("document id must look like ns:path, got '" + id + "'");
        String path = id.substring(colon + 1);
        if (path.endsWith(JSON)) path = path.substring(0, path.length() - JSON.length());
        else if (path.endsWith(MARKUP)) path = path.substring(0, path.length() - MARKUP.length());
        return new DocumentId(id.substring(0, colon), path);
    }

    public static boolean isValid(String id) {
        int colon = id.indexOf(':');
        return colon > 0 && colon < id.length() - 1;
    }

    public String resource(String extension) {
        return "assets/" + namespace + "/vui/" + path + extension;
    }

    public static DocumentId fromResource(String relative) {
        String p = relative.replace('\\', '/');
        int assets = p.indexOf("assets/");
        if (assets < 0) return null;
        String rest = p.substring(assets + 7);
        int slash = rest.indexOf('/');
        if (slash < 0 || !rest.startsWith("vui/", slash + 1)) return null;
        String ns = rest.substring(0, slash);
        String file = rest.substring(slash + 5);
        if (file.endsWith(JSON)) file = file.substring(0, file.length() - JSON.length());
        else if (file.endsWith(MARKUP)) file = file.substring(0, file.length() - MARKUP.length());
        else return null;
        return new DocumentId(ns, file);
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }
}
