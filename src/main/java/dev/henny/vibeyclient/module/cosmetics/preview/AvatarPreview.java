package dev.henny.vibeyclient.module.cosmetics.preview;

import dev.henny.cosmetico.preview.PreviewView;
import dev.henny.vibeyclient.ui.Gfx;

public interface AvatarPreview {
    View create();

    interface View extends PreviewView {
        void draw(Gfx g, int x, int y, int w, int h, float delta);
    }
}
