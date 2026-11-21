package dev.henny.vibeafk;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

public final class EventLog {
    public static final String FILE = "afk-events.jsonl";
    public static final String ROTATED = "afk-events.1.jsonl";
    public static final long DEFAULT_MAX = 2L * 1024 * 1024;

    private final File dir;
    private final long maxBytes;
    private final Clock clock;
    private long seq;
    private boolean broken;

    public interface Clock {
        long now();
    }

    public EventLog(File dir, long maxBytes, Clock clock) {
        this.dir = dir;
        this.maxBytes = maxBytes;
        this.clock = clock;
    }

    public synchronized long seq() {
        return seq;
    }

    public synchronized long emit(String type, Map<String, Object> fields) {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        o.put("seq", ++seq);
        o.put("ts", clock.now());
        o.put("type", type);
        if (fields != null) {
            for (Map.Entry<String, Object> e : fields.entrySet()) {
                String k = e.getKey();
                if (!k.equals("seq") && !k.equals("ts") && !k.equals("type")) o.put(k, e.getValue());
            }
        }
        byte[] line = (Json.write(o) + "\n").getBytes(Fs.UTF8);
        File f = new File(dir, FILE);
        try {
            if (!dir.isDirectory()) java.nio.file.Files.createDirectories(dir.toPath());
            if (f.length() > 0 && f.length() + line.length > maxBytes) {
                try {
                    rotate(f);
                } catch (IOException ignored) {
                }
            }
            FileOutputStream out = new FileOutputStream(f, true);
            try {
                out.write(line);
                out.flush();
            } finally {
                out.close();
            }
            broken = false;
        } catch (IOException e) {
            if (!broken) System.err.println("[vibe-afk] cannot write " + f + ": " + e);
            broken = true;
        }
        return seq;
    }

    private void rotate(File f) throws IOException {
        File old = new File(dir, ROTATED);
        Fs.deleteQuietly(old);
        if (!f.renameTo(old)) {
            java.nio.file.Files.move(f.toPath(), old.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
