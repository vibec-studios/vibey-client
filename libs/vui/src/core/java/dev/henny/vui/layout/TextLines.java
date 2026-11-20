package dev.henny.vui.layout;

public final class TextLines {
    private int[] starts = new int[4];
    private int[] ends = new int[4];
    private float[] widths = new float[4];
    private int count;
    private boolean ellipsized;
    private float maxWidth;

    public void clear() {
        count = 0;
        ellipsized = false;
        maxWidth = 0;
    }

    public void add(int start, int end, float width) {
        if (count == starts.length) grow();
        starts[count] = start;
        ends[count] = end;
        widths[count] = width;
        count++;
        if (width > maxWidth) maxWidth = width;
    }

    public void replaceLast(int end, float width, boolean ellipsis) {
        if (count == 0) return;
        ends[count - 1] = end;
        widths[count - 1] = width;
        ellipsized = ellipsis;
        maxWidth = 0;
        for (int i = 0; i < count; i++) maxWidth = Math.max(maxWidth, widths[i]);
    }

    public int count() {
        return count;
    }

    public int start(int line) {
        return starts[line];
    }

    public int end(int line) {
        return ends[line];
    }

    public float width(int line) {
        return widths[line];
    }

    public boolean ellipsized() {
        return ellipsized;
    }

    public float maxWidth() {
        return maxWidth;
    }

    private void grow() {
        int n = starts.length * 2;
        starts = java.util.Arrays.copyOf(starts, n);
        ends = java.util.Arrays.copyOf(ends, n);
        widths = java.util.Arrays.copyOf(widths, n);
    }
}
