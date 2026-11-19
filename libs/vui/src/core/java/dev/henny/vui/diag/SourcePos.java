package dev.henny.vui.diag;

public record SourcePos(String file, int line, int col) {
    public static final SourcePos UNKNOWN = new SourcePos("<unknown>", 0, 0);

    public static SourcePos of(String file, int line, int col) {
        return new SourcePos(file, line, col);
    }

    public SourcePos shift(int columns) {
        return new SourcePos(file, line, col + columns);
    }

    @Override
    public String toString() {
        return file + ":" + line + ":" + col;
    }
}
