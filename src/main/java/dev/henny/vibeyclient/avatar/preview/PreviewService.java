package dev.henny.vibeyclient.avatar.preview;

/** The {@link AvatarPreview} service. */
public final class PreviewService implements AvatarPreview {
    @Override
    public View create() {
        return new PreviewView();
    }
}
