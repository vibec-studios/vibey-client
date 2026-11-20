package dev.henny.vibeyclient.module.clips.shots;

import dev.henny.vibeyclient.api.Lang;
import dev.henny.vibeyclient.api.Tasks;
import dev.henny.vibeyclient.api.service.Notify;
import dev.henny.vibeyclient.client.ClientCompat;
import dev.henny.vibeyclient.module.clips.shots.core.Shot;

import java.nio.file.Path;

public final class GalleryActions {
    private static volatile String lastCopy = "";

    private GalleryActions() {}

    static void copy(Shot s) {
        Path file = ShotFiles.file(s).toAbsolutePath();
        ShotClipboard.THREAD.execute(() -> {
            boolean image = ShotClipboard.copyImage(file);
            Tasks.onMain(() -> {
                if (!image) ClientCompat.setClipboard(file.toString());
                lastCopy = image ? "image" : "path";
                Notify.success(Lang.tr("clips.shots.title"), Lang.tr(image ? "clips.shots.copied" : "clips.shots.pathCopied"));
            });
        });
    }

    static void deleted() {
        Notify.info(Lang.tr("clips.shots.title"), Lang.tr("clips.shots.deleted"));
    }

    public static String lastCopy() {
        return lastCopy;
    }
}
