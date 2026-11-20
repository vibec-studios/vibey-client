package dev.henny.companio.pack;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.text.Lang;
import dev.henny.companio.text.TextRef;

import java.util.List;

public record Pack(String id, String version, TextRef name, TextRef description, String author, boolean builtin,
                   String location, String hash, byte[] icon, Lang lang, List<CompanionDef> companions,
                   List<Problem> problems, long stamp) {
    public boolean loaded() {
        return id != null && !companions.isEmpty();
    }

    public int errors() {
        int n = 0;
        for (Problem p : problems) if (p.error()) n++;
        return n;
    }

    public int warnings() {
        return problems.size() - errors();
    }

    public CompanionDef companion(String id) {
        for (CompanionDef c : companions) if (c.id().equals(id)) return c;
        return null;
    }
}
