package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.Vibey;

import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

final class ShotClipboard {
    private static final String ENV = "VIBEY_SHOT";
    private static final String WINDOWS = "Add-Type -AssemblyName System.Windows.Forms; Add-Type -AssemblyName System.Drawing; "
            + "$i=[System.Drawing.Image]::FromFile($env:VIBEY_SHOT); [System.Windows.Forms.Clipboard]::SetImage($i); $i.Dispose()";
    private static final String MAC = "set the clipboard to (read (POSIX file (system attribute \"VIBEY_SHOT\")) as «class PNGf»)";

    private ShotClipboard() {}

    static boolean copyImage(Path file) {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        String png = file.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".png") ? "image/png" : "image/jpeg";
        if (os.contains("win")) return run(List.of("powershell", "-NoProfile", "-STA", "-Command", WINDOWS), file, false);
        if (os.contains("mac")) return png.equals("image/png") && run(List.of("osascript", "-e", MAC), file, false);
        return run(List.of("wl-copy", "--type", png), file, true)
                || run(List.of("xclip", "-selection", "clipboard", "-t", png, "-i", file.toString()), file, false);
    }

    private static boolean run(List<String> cmd, Path file, boolean stdin) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd).redirectErrorStream(true);
            pb.environment().put(ENV, file.toAbsolutePath().toString());
            if (stdin) pb.redirectInput(file.toFile());
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            if (!stdin) p.getOutputStream().close();
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return false;
            }
            return p.exitValue() == 0;
        } catch (Exception e) {
            Vibey.LOG.debug("Screenshots: {} failed: {}", cmd.get(0), e.toString());
            return false;
        }
    }
}
