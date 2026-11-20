package dev.henny.vui.layout;

final class Justify {
    private static final Justify SHARED = new Justify();

    private float start;
    private float gap;

    float start() {
        return start;
    }

    float gap() {
        return gap;
    }

    static Justify of(String keyword, float free, int count, float baseGap) {
        Justify j = SHARED;
        j.start = 0;
        j.gap = baseGap;
        if (free <= 0 || count == 0) return j;
        switch (keyword) {
            case "center" -> j.start = free / 2f;
            case "end" -> j.start = free;
            case "space-between" -> {
                if (count > 1) j.gap = baseGap + free / (count - 1);
            }
            case "space-around" -> {
                float each = free / count;
                j.start = each / 2f;
                j.gap = baseGap + each;
            }
            case "space-evenly" -> {
                float each = free / (count + 1);
                j.start = each;
                j.gap = baseGap + each;
            }
            default -> {
            }
        }
        return j;
    }
}
