package dev.henny.vibeyclient.module.media.video.controls;

import net.minecraft.client.Minecraft;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.util.function.Consumer;

final class FilePicker {
    private static final String[] PATTERNS = {"*.mp4", "*.webm", "*.mkv", "*.mov", "*.avi", "*.m4v", "*.mp3", "*.ogg", "*.wav", "*.flac", "*.gif"};
    private static volatile boolean open;

    private FilePicker() {}

    static void pick(String title, String filterName, Consumer<String> chosen) {
        if (open) return;
        open = true;
        Thread t = new Thread(() -> {
            String path = dialog(title, filterName);
            Minecraft.getInstance().execute(() -> {
                open = false;
                if (path != null) chosen.accept(path);
            });
        }, "Vibey video file picker");
        t.setDaemon(true);
        t.start();
    }

    private static String dialog(String title, String filterName) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer filters = stack.mallocPointer(PATTERNS.length);
            for (String p : PATTERNS) filters.put(stack.UTF8(p));
            filters.flip();
            return TinyFileDialogs.tinyfd_openFileDialog(title, null, filters, filterName, false);
        } catch (Throwable e) {
            return null;
        }
    }
}
