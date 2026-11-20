package dev.henny.cosmetico.version;

import java.util.function.Supplier;

public final class VersionGate {
    private final Supplier<String> contentVersion;

    public VersionGate(Supplier<String> contentVersion) {
        this.contentVersion = contentVersion;
    }

    public static VersionGate fixed(String version) {
        return new VersionGate(() -> version);
    }

    public String version() {
        return contentVersion.get();
    }

    public boolean satisfies(String required) {
        return SemVer.compare(version(), required) >= 0;
    }
}
