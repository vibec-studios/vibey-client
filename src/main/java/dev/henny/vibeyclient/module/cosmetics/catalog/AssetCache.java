package dev.henny.vibeyclient.module.cosmetics.catalog;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.model.Bundle;
import dev.henny.cosmetico.parse.Bundles;
import dev.henny.vibeyclient.Vibey;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletionException;

final class AssetCache {
    private AssetCache() {}

    static Bundle fromDisk(CatalogEntry e) throws Exception {
        Path file = asset(e.hash());
        if (!Files.isRegularFile(file)) return null;
        byte[] bytes = Files.readAllBytes(file);
        if (!Bundles.sha256(bytes).equals(e.hash())) return null;
        return parse(e, bytes);
    }

    static Bundle verifyAndParse(CatalogEntry e, byte[] bytes) {
        try {
            if (!Bundles.sha256(bytes).equals(e.hash())) throw new IllegalStateException("asset has a wrong hash");
            Files.createDirectories(asset(e.hash()).getParent());
            CatalogIo.write(asset(e.hash()), bytes);
            return parse(e, bytes);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CompletionException(ex);
        }
    }

    private static Bundle parse(CatalogEntry e, byte[] bytes) {
        Bundle b = Bundles.parse(bytes, Cosmetico.versionGate());
        if (!b.id.equals(e.id())) CatalogLog.once(e.id() + "@" + e.hash() + "@id", "Catalog item {} contains bundle {}", e.id(), b.id);
        if (!b.warnings.isEmpty()) CatalogLog.once(e.hash() + "@warn", "Catalog item {}: {}", e.id(), String.join("; ", b.warnings));
        Cosmetico.engine().resources().prepare(b);
        return b;
    }

    private static Path asset(String hash) {
        return CatalogIo.cacheDir().resolve("assets").resolve(hash + ".vibey");
    }

    static String readBase() {
        try {
            Path f = CatalogIo.cacheDir().resolve("catalog.base");
            return Files.isRegularFile(f) ? Files.readString(f, StandardCharsets.UTF_8).trim() : null;
        } catch (IOException e) {
            return null;
        }
    }

    static void writeBase(String base) {
        try {
            if (!base.equals(readBase())) CatalogIo.write(CatalogIo.cacheDir().resolve("catalog.base"), base.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            Vibey.LOG.debug("catalog.base not written: {}", e.toString());
        }
    }
}
