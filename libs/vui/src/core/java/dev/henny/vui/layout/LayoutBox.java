package dev.henny.vui.layout;

public final class LayoutBox {
    public float x;
    public float y;
    public float width;
    public float height;
    public float marginTop;
    public float marginRight;
    public float marginBottom;
    public float marginLeft;
    public float paddingTop;
    public float paddingRight;
    public float paddingBottom;
    public float paddingLeft;
    public float borderTop;
    public float borderRight;
    public float borderBottom;
    public float borderLeft;
    public float contentWidth;
    public float contentHeight;
    public float measuredWidth;
    public float measuredHeight;
    public float flexBasis;
    public float flexMain;
    public float flexCross;
    public boolean flexFrozen;
    public int line;
    public int measureStamp = -1;
    public float measureMaxWidth;
    public float measureMaxHeight;

    public float innerX() {
        return x + borderLeft + paddingLeft;
    }

    public float innerY() {
        return y + borderTop + paddingTop;
    }

    public float innerWidth() {
        return Math.max(0, width - borderLeft - borderRight - paddingLeft - paddingRight);
    }

    public float innerHeight() {
        return Math.max(0, height - borderTop - borderBottom - paddingTop - paddingBottom);
    }

    public float horizontalExtras() {
        return borderLeft + borderRight + paddingLeft + paddingRight;
    }

    public float verticalExtras() {
        return borderTop + borderBottom + paddingTop + paddingBottom;
    }

    public boolean contains(float px, float py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public void set(float x, float y, float width, float height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public void zero() {
        set(0, 0, 0, 0);
        contentWidth = 0;
        contentHeight = 0;
    }

    @Override
    public String toString() {
        return "[" + fmt(x) + "," + fmt(y) + " " + fmt(width) + "x" + fmt(height) + "]";
    }

    private static String fmt(float v) {
        return v == Math.rint(v) ? Integer.toString((int) v) : String.format(java.util.Locale.ROOT, "%.1f", v);
    }
}
