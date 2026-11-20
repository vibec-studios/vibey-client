package dev.henny.vibeyclient.module.cosmetics.preview;

import dev.henny.cosmetico.mc.preview.McPreview;

public final class CosmeticoPreviews implements AvatarPreview {
    @Override
    public View create() {
        return new PreviewWidget(McPreview.create());
    }
}
