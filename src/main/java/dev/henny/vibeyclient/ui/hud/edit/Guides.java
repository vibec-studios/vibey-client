package dev.henny.vibeyclient.ui.hud.edit;

import dev.henny.vui.hud.SnapGuide;

import java.util.ArrayList;
import java.util.List;

public final class Guides {
    public record Readout(float x1, float y1, float x2, float y2, int value) {
        public boolean horizontal() {
            return y1 == y2;
        }
    }

    static final int MAX_GUIDES = 16;
    static final int MAX_READOUTS = 8;

    private final List<SnapGuide> lines = new ArrayList<>();
    private final List<Readout> readouts = new ArrayList<>();
    private float anchorX = Float.NaN;
    private float anchorY;
    private String sizeChip;

    public List<SnapGuide> lines() {
        return lines;
    }

    public List<Readout> readouts() {
        return readouts;
    }

    public boolean hasAnchorDot() {
        return !Float.isNaN(anchorX);
    }

    public float anchorX() {
        return anchorX;
    }

    public float anchorY() {
        return anchorY;
    }

    public String sizeChip() {
        return sizeChip;
    }

    void line(boolean vertical, float at, float from, float to) {
        if (lines.size() < MAX_GUIDES) lines.add(new SnapGuide(vertical, at, Math.min(from, to), Math.max(from, to)));
    }

    void readout(float x1, float y1, float x2, float y2, float v) {
        if (readouts.size() < MAX_READOUTS && Math.abs(v) >= 1) readouts.add(new Readout(x1, y1, x2, y2, Math.round(v)));
    }

    void anchor(float x, float y) {
        anchorX = x;
        anchorY = y;
    }

    void sizeChip(String s) {
        sizeChip = s;
    }

    void clearLines() {
        lines.clear();
    }

    void clearReadouts() {
        readouts.clear();
    }

    public void clear() {
        lines.clear();
        readouts.clear();
        anchorX = Float.NaN;
        sizeChip = null;
    }
}
