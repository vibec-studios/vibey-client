package dev.henny.companio.mc.client;

import dev.henny.companio.config.CompanioConfig;
import dev.henny.companio.config.ConfigCodec;
import dev.henny.companio.mc.CompanioMod;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class ConfigStore {
    private final Path file;
    private boolean dirty;

    public ConfigStore(Path file) {
        this.file = file;
    }

    public CompanioConfig load() {
        try {
            return ConfigCodec.read(Files.exists(file) ? Files.readString(file, StandardCharsets.UTF_8) : null);
        } catch (IOException e) {
            CompanioMod.LOG.warn("Cannot read {}", file, e);
            return new CompanioConfig();
        }
    }

    public void markDirty() {
        dirty = true;
    }

    public void saveIfDirty(CompanioConfig c) {
        if (dirty) save(c);
    }

    public void save(CompanioConfig c) {
        dirty = false;
        try {
            Files.createDirectories(file.getParent());
            Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(tmp, ConfigCodec.write(c), StandardCharsets.UTF_8);
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            CompanioMod.LOG.warn("Cannot write {}", file, e);
        }
    }
}
