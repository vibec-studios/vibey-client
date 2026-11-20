package dev.henny.companio.parse;

import dev.henny.companio.expr.Suggest;
import dev.henny.cosmetico.model.Bone;
import dev.henny.cosmetico.model.Bundle;

import java.util.ArrayList;
import java.util.List;

final class BoneNames {
    private BoneNames() {}

    static List<String> of(Bundle model) {
        List<String> names = new ArrayList<>();
        for (Bone b : model.model.bones) names.add(b.name);
        return names;
    }

    static String hint(Bundle model, String bone) {
        List<String> names = of(model);
        String h = Suggest.hint(bone, names);
        if (!h.isEmpty()) return h;
        return " (bones: " + String.join(", ", names.subList(0, Math.min(8, names.size()))) + (names.size() > 8 ? ", …" : "") + ")";
    }
}
