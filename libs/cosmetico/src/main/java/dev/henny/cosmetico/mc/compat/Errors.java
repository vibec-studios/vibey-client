package dev.henny.cosmetico.mc.compat;

import dev.henny.cosmetico.mc.CosmeticoMod;

public final class Errors {
    private final String what;
    private int count;

    public Errors(String what) {
        this.what = what;
    }

    public void report(Throwable e) {
        if (count++ < 3) CosmeticoMod.LOG.error(what, e);
    }
}
