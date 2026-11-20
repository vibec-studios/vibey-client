package dev.henny.vui.mc.resource;

import dev.henny.vui.expr.Translator;
import net.minecraft.client.resources.language.I18n;

public final class McTranslator implements Translator {
    @Override
    public String translate(String key, Object[] args) {
        try {
            return I18n.get(key, args == null ? new Object[0] : args);
        } catch (RuntimeException e) {
            return key;
        }
    }
}
