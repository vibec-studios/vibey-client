package dev.henny.vui.layout;

import dev.henny.vui.dom.Element;
import dev.henny.vui.style.Prop;

import java.util.Arrays;

final class GridPlacement {
    int count;
    int rows;
    int[] child = new int[16];
    int[] row = new int[16];
    int[] col = new int[16];
    int[] colSpan = new int[16];
    int[] rowSpan = new int[16];
    float[] rowHeight = new float[16];
    private boolean[] taken = new boolean[64];
    private int cols;

    void place(Element e, int columns) {
        cols = columns;
        count = 0;
        rows = 0;
        Arrays.fill(taken, false);
        int curRow = 0;
        int curCol = 0;
        for (int i = 0; i < e.childCount(); i++) {
            Element c = e.child(i);
            if (!Flow.inFlow(c)) continue;
            int cs = Math.max(1, Math.min(cols, (int) c.style().number(Prop.GRID_COLUMN_SPAN)));
            int rs = Math.max(1, (int) c.style().number(Prop.GRID_ROW_SPAN));
            while (!fits(curRow, curCol, cs, rs)) {
                if (++curCol + cs > cols) {
                    curCol = 0;
                    curRow++;
                }
            }
            add(i, curRow, curCol, cs, rs);
            curCol += cs;
            if (curCol >= cols) {
                curCol = 0;
                curRow++;
            }
        }
    }

    float spanWidth(int index, float cell, float gap) {
        return colSpan[index] * cell + (colSpan[index] - 1) * gap;
    }

    float spanHeight(int index, float gap) {
        float h = 0;
        for (int r = row[index]; r < row[index] + rowSpan[index]; r++) h += rowHeight[r];
        return h + (rowSpan[index] - 1) * gap;
    }

    float rowTop(int r, float gap) {
        float y = 0;
        for (int i = 0; i < r; i++) y += rowHeight[i] + gap;
        return y;
    }

    float totalHeight(float gap) {
        return rows == 0 ? 0 : rowTop(rows, gap) - gap;
    }

    void clearHeights() {
        if (rowHeight.length < rows) rowHeight = new float[Math.max(rows, rowHeight.length * 2)];
        Arrays.fill(rowHeight, 0, rows, 0f);
    }

    private boolean fits(int r, int c, int cs, int rs) {
        if (c + cs > cols) return false;
        for (int y = r; y < r + rs; y++) for (int x = c; x < c + cs; x++) if (taken(y, x)) return false;
        return true;
    }

    private boolean taken(int r, int c) {
        int i = r * cols + c;
        return i < taken.length && taken[i];
    }

    private void add(int childIndex, int r, int c, int cs, int rs) {
        if (count == child.length) grow();
        child[count] = childIndex;
        row[count] = r;
        col[count] = c;
        colSpan[count] = cs;
        rowSpan[count] = rs;
        count++;
        rows = Math.max(rows, r + rs);
        int needed = rows * cols;
        if (needed > taken.length) taken = Arrays.copyOf(taken, Math.max(needed, taken.length * 2));
        for (int y = r; y < r + rs; y++) for (int x = c; x < c + cs; x++) taken[y * cols + x] = true;
    }

    private void grow() {
        int n = child.length * 2;
        child = Arrays.copyOf(child, n);
        row = Arrays.copyOf(row, n);
        col = Arrays.copyOf(col, n);
        colSpan = Arrays.copyOf(colSpan, n);
        rowSpan = Arrays.copyOf(rowSpan, n);
    }
}
