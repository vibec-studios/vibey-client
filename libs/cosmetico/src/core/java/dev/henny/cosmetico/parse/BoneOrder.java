package dev.henny.cosmetico.parse;

import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static dev.henny.cosmetico.parse.Json.str;

final class BoneOrder {
    private BoneOrder() {}

    static List<String> of(Map<String, JsonObject> byName) {
        List<String> order = new ArrayList<>();
        Set<String> done = new HashSet<>();
        for (String n : byName.keySet()) visit(n, byName, done, new HashSet<>(), order);
        return order;
    }

    private static void visit(String n, Map<String, JsonObject> byName, Set<String> done, Set<String> stack, List<String> out) {
        if (done.contains(n)) return;
        if (!stack.add(n)) throw new BundleException("bone \"" + n + "\": parent cycle");
        String p = str(byName.get(n), "parent");
        if (p != null) {
            if (!byName.containsKey(p)) throw new BundleException("bone \"" + n + "\": parent \"" + p + "\" does not exist");
            visit(p, byName, done, stack, out);
        }
        stack.remove(n);
        done.add(n);
        out.add(n);
    }
}
