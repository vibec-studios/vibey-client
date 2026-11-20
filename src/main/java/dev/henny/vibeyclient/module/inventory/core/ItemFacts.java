package dev.henny.vibeyclient.module.inventory.core;

import java.util.ArrayList;
import java.util.List;

public record ItemFacts(String name, String id, String mod, List<String> extras) {
    public static ItemFacts of(String name, String id, List<String> extras) {
        int colon = id.indexOf(':');
        String mod = colon < 0 ? "minecraft" : id.substring(0, colon);
        String path = colon < 0 ? id : id.substring(colon + 1);
        List<String> folded = new ArrayList<>(extras.size());
        for (String e : extras) folded.add(TextFold.fold(e));
        return new ItemFacts(TextFold.fold(name), TextFold.fold(path), TextFold.fold(mod), folded);
    }

    boolean contains(String term) {
        if (name.contains(term) || id.contains(term)) return true;
        for (String e : extras) if (e.contains(term)) return true;
        return false;
    }
}
