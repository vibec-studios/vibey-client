package dev.henny.vui.template;

import dev.henny.vui.diag.Suggest;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

final class TagSuggestions {
    private TagSuggestions() {
    }

    static String suggest(String tag, Collection<String> builtIn, Set<String> imported) {
        List<String> all = new ArrayList<>(builtIn);
        all.addAll(imported);
        String best = Suggest.closest(tag, all, 2);
        if (best != null) return "did you mean <" + best + ">?";
        return "import it with <import name=\"" + tag + "\" src=\"ns:path\"/> or use a built-in element";
    }
}
