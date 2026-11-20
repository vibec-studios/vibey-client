package dev.henny.vibeyclient.ui.hud.editor;

interface EditorHost {
    void close(boolean keep);

    void refresh();

    void toast(String text);

    float pointerX();

    float pointerY();
}
