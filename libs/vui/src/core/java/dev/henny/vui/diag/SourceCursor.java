package dev.henny.vui.diag;

public final class SourceCursor {
    private final String text;
    private final String file;
    private final int baseLine;
    private final int baseCol;
    private int index;
    private int line;
    private int col;

    public SourceCursor(String text, String file) {
        this(text, new SourcePos(file, 1, 1));
    }

    public SourceCursor(String text, SourcePos start) {
        this.text = text == null ? "" : text;
        this.file = start.file();
        this.baseLine = start.line();
        this.baseCol = start.col();
        this.line = baseLine;
        this.col = baseCol;
    }

    public String text() {
        return text;
    }

    public int index() {
        return index;
    }

    public boolean atEnd() {
        return index >= text.length();
    }

    public char peek() {
        return index < text.length() ? text.charAt(index) : '\0';
    }

    public char peek(int ahead) {
        int i = index + ahead;
        return i < text.length() ? text.charAt(i) : '\0';
    }

    public boolean startsWith(String s) {
        return text.startsWith(s, index);
    }

    public char next() {
        char c = text.charAt(index++);
        if (c == '\n') {
            line++;
            col = 1;
        } else {
            col++;
        }
        return c;
    }

    public void skip(int count) {
        for (int i = 0; i < count && !atEnd(); i++) next();
    }

    public void skipWhitespace() {
        while (!atEnd() && Character.isWhitespace(peek())) next();
    }

    public SourcePos pos() {
        return new SourcePos(file, line, col);
    }

    public String file() {
        return file;
    }

    public int indexOf(String s) {
        return text.indexOf(s, index);
    }

    public String readUntil(int end) {
        int start = index;
        while (index < end) next();
        return text.substring(start, end);
    }
}
