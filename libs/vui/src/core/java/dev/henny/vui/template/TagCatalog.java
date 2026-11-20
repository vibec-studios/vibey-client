package dev.henny.vui.template;

import java.util.Collection;

public interface TagCatalog {
    boolean isBuiltIn(String tag);

    Collection<String> builtInTags();
}
