package dev.henny.companio.def;

public record SoundDef(String id, String file, String event, float volume, float pitchMin, float pitchMax) {
    public boolean embedded() {
        return file != null;
    }
}
