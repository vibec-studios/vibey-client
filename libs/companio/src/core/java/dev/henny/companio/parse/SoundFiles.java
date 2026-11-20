package dev.henny.companio.parse;

import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.PackLimits;
import dev.henny.companio.pack.Problems;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SoundFiles {
    private SoundFiles() {}

    static Map<String, byte[]> read(PackFiles files, List<String> list, Problems p) {
        Map<String, byte[]> out = new LinkedHashMap<>();
        for (String path : list) {
            if (!path.endsWith(".ogg")) continue;
            p.file(path);
            try {
                byte[] b = files.read(path);
                if (b.length > PackLimits.MAX_SOUND_BYTES) p.error("", "sound is " + b.length / 1024 + " KiB, at most " + PackLimits.MAX_SOUND_BYTES / 1024 + " KiB");
                else if (!Png.ogg(b)) p.error("", "not an OGG Vorbis file");
                else out.put(path, b);
            } catch (IOException e) {
                p.error("", "cannot read: " + e.getMessage());
            }
        }
        return out;
    }
}
