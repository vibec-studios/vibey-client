package dev.henny.cosmetico.golden;

import dev.henny.cosmetico.version.VersionGate;

public final class GoldenVersion {
    private GoldenVersion() {}

    public static String value() {
        return Golden.load("versions.txt").lines().findFirst().orElseThrow().substring("contentVersion ".length());
    }

    public static VersionGate gate() {
        return VersionGate.fixed(value());
    }
}
