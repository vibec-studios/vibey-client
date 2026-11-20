package dev.henny.companio.tool;

import dev.henny.companio.def.CompanionDef;
import dev.henny.companio.pack.DirFiles;
import dev.henny.companio.pack.Pack;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.Problem;
import dev.henny.companio.pack.ZipFiles;
import dev.henny.companio.parse.PackReader;
import dev.henny.cosmetico.version.VersionGate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CheckPack {
    private CheckPack() {}

    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
            System.out.println("usage: CheckPack <pack folder or .zip> …");
            System.exit(2);
        }
        int errors = 0;
        for (String a : args) errors += check(Path.of(a));
        System.exit(errors > 0 ? 1 : 0);
    }

    static int check(Path path) throws IOException {
        PackFiles files = Files.isDirectory(path) ? new DirFiles(path) : ZipFiles.read(path);
        Pack p = PackReader.read(files, VersionGate.fixed("99.0.0"), false);
        System.out.println((p.errors() == 0 ? "ok   " : "FAIL ") + (p.id() == null ? path : p.id()) + "  " + p.companions().size()
                + " companion(s), " + p.errors() + " error(s), " + p.warnings() + " warning(s)");
        for (CompanionDef c : p.companions()) {
            System.out.println("     " + c.key() + ": " + c.behaviors().size() + " behaviors, " + c.props().size() + " props, "
                    + c.sounds().size() + " sounds, " + c.variants().size() + " variants");
        }
        for (Problem pr : p.problems()) System.out.println("     " + pr);
        return p.errors();
    }
}
