package dev.henny.companio.pack;

public record Problem(String file, String path, String message, boolean error) {
    public String format() {
        StringBuilder sb = new StringBuilder();
        if (file != null && !file.isEmpty()) sb.append(file).append(':');
        if (path != null && !path.isEmpty()) sb.append(sb.isEmpty() ? "" : " ").append(path);
        if (!sb.isEmpty()) sb.append(" → ");
        return sb.append(message).toString();
    }

    @Override
    public String toString() {
        return (error ? "error: " : "warning: ") + format();
    }
}
