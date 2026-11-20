package dev.henny.vui.diag;

public record SourcePos(String file, int line, int col) {
    public static final SourcePos UNKNOWN = new SourcePos("<unknown>", 0, 0);

    public static SourcePos of(String file, int line, int col) {
        return new SourcePos(file, line, col);
    }

    public SourcePos shift(int columns) {
        return new SourcePos(file, line, col + columns);
    }

    public String shortText() {
        return shortFile() + ":" + line + ":" + col;
    }

    public String shortFile() {
        String f = file.replace('\\', '/');
        int assets = f.lastIndexOf("/assets/");
        if (assets >= 0) return f.substring(assets + 8);
        int slash = f.lastIndexOf('/');
        return slash < 0 ? f : f.substring(slash + 1);
    }

    @Override
    public String toString() {
        return file + ":" + line + ":" + col;
    }
}
