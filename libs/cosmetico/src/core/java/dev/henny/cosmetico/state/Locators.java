package dev.henny.cosmetico.state;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class Locators {
    private final Map<String, double[]> positions = new HashMap<>();

    public double[] get(String name) {
        return positions.computeIfAbsent(name, k -> new double[]{Double.NaN, 0, 0});
    }

    public double[] peek(String name) {
        return positions.get(name);
    }

    public Set<String> names() {
        return positions.keySet();
    }

    public void clear() {
        positions.clear();
    }

    public void set(String name, double x, double y, double z) {
        double[] out = get(name);
        out[0] = x;
        out[1] = y;
        out[2] = z;
    }
}
