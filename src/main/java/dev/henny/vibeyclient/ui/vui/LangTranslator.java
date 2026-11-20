package dev.henny.vibeyclient.ui.vui;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vui.expr.Translator;
import dev.henny.vui.mc.resource.McTranslator;

final class LangTranslator implements Translator {
    private final Translator vanilla = new McTranslator();

    @Override
    public String translate(String key, Object[] args) {
        Object[] a = args == null ? new Object[0] : args;
        if (Lang.has(key)) return Lang.tr(key, a);
        return vanilla.translate(key, a);
    }
}
