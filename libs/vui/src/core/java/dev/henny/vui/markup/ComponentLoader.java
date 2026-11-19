package dev.henny.vui.markup;

public final class ComponentLoader {
    private ComponentLoader() {
    }

    public static ComponentFile load(String id, String fileName, String source) {
        if (fileName.endsWith(".json")) return JsonTreeReader.read(id, fileName, source);
        return SfcReader.read(id, fileName, source);
    }
}
