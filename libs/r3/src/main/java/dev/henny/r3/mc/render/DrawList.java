package dev.henny.r3.mc.render;

import dev.henny.r3.render.RenderMode;
import org.joml.Matrix4fc;

import java.util.ArrayList;
import java.util.List;

final class DrawList {
    private final List<Object> items = new ArrayList<>();
    private final List<QuadItem> quadPool = new ArrayList<>();
    private final List<TextItem> textPool = new ArrayList<>();
    private int quads;
    private int texts;

    void clear() {
        items.clear();
        quads = 0;
        texts = 0;
    }

    int size() {
        return items.size();
    }

    Object get(int i) {
        return items.get(i);
    }

    QuadItem quads(RenderMode mode, String texture) {
        if (!items.isEmpty() && items.get(items.size() - 1) instanceof QuadItem last && last.matches(mode, texture)) return last;
        if (quads == quadPool.size()) quadPool.add(new QuadItem());
        QuadItem q = quadPool.get(quads++).reset(mode, texture);
        items.add(q);
        return q;
    }

    void text(Matrix4fc pose, String text, float x, float y, int argb, int light, TextMode mode, boolean shadow, boolean bold) {
        if (text == null || text.isEmpty() || (argb >>> 24) < 4) return;
        if (texts == textPool.size()) textPool.add(new TextItem());
        items.add(textPool.get(texts++).set(pose, text, x, y, argb, light, mode, shadow, bold));
    }

    void styledText(Matrix4fc pose, String text, String font, boolean bold, float x, float y, int argb, int light, TextMode mode) {
        int before = items.size();
        text(pose, text, x, y, argb, light, mode, false, bold);
        if (items.size() > before) ((TextItem) items.get(items.size() - 1)).font = font;
    }
}
