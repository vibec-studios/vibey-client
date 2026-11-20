package dev.henny.vibeyclient.module.media.video.world;

import dev.henny.r3.interact.InteractionEvent;
import dev.henny.r3.interact.InteractionListener;
import dev.henny.r3.object.WorldObject;
import dev.henny.vibeyclient.module.media.video.MediaScreen;
import dev.henny.vibeyclient.module.media.video.Video;
import dev.henny.vibeyclient.module.media.video.controls.ScreenControls;

public final class ScreenEvents implements InteractionListener {
    private final Video video;

    public ScreenEvents(Video video) {
        this.video = video;
    }

    @Override
    public void onEvent(InteractionEvent e) {
        MediaScreen s = video.find(e.target());
        if (s == null) return;
        switch (e.kind()) {
            case PLACED -> video.actions.placed(s);
            case MOVED, RESIZED, UNDONE -> {
                ScreenObjects.fitLayout((WorldObject) e.target());
                video.actions.changed(s);
            }
            case PLACEMENT_CANCELLED -> cancelled(s);
            case CONTEXT_MENU -> {
                ScreenControls c = ScreenControls.of(s);
                if (c != null) c.openMore();
            }
            default -> {
            }
        }
    }

    private void cancelled(MediaScreen s) {
        if (s.id.equals(video.overlayId)) video.unplace(s);
        else video.actions.remove(s);
    }
}
