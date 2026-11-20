package dev.henny.companio.pack;

import java.util.ArrayList;
import java.util.List;

public final class Problems {
    private final List<Problem> list = new ArrayList<>();
    private String file = "";

    public Problems file(String file) {
        this.file = file;
        return this;
    }

    public String file() {
        return file;
    }

    public void error(String path, String message) {
        list.add(new Problem(file, path, message, true));
    }

    public void warn(String path, String message) {
        list.add(new Problem(file, path, message, false));
    }

    public int errorCount() {
        int n = 0;
        for (Problem p : list) if (p.error()) n++;
        return n;
    }

    public int mark() {
        return errorCount();
    }

    public boolean newErrorsSince(int mark) {
        return errorCount() > mark;
    }

    public List<Problem> all() {
        return List.copyOf(list);
    }
}
