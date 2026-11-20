package dev.henny.vui.template;

public record SlotSpec(String name, String[] destructure, String variable) {

    public static SlotSpec parse(String name, String value) {
        if (value == null || value.isBlank()) return new SlotSpec(name, new String[0], null);
        String v = value.trim();
        if (v.startsWith("{") && v.endsWith("}")) {
            String[] names = v.substring(1, v.length() - 1).split(",");
            for (int i = 0; i < names.length; i++) names[i] = names[i].trim();
            return new SlotSpec(name, names, null);
        }
        return new SlotSpec(name, new String[0], v);
    }
}
