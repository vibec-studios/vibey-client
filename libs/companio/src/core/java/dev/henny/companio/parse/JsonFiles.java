package dev.henny.companio.parse;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.henny.companio.pack.PackFiles;
import dev.henny.companio.pack.Problems;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

final class JsonFiles {
    private JsonFiles() {}

    static JsonObject object(PackFiles files, String path, Problems p) {
        byte[] bytes = bytes(files, path, p);
        if (bytes == null) return null;
        try {
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.startsWith("﻿")) text = text.substring(1);
            JsonElement e = JsonParser.parseString(text);
            if (e.isJsonObject()) return e.getAsJsonObject();
            p.error("", "the file must contain one JSON object { … }");
        } catch (RuntimeException ex) {
            p.error("", "invalid JSON: " + shortMessage(ex));
        }
        return null;
    }

    static byte[] bytes(PackFiles files, String path, Problems p) {
        if (!files.exists(path)) {
            p.error("", "file is missing");
            return null;
        }
        try {
            return files.read(path);
        } catch (IOException ex) {
            p.error("", "cannot read: " + ex.getMessage());
            return null;
        }
    }

    private static String shortMessage(RuntimeException ex) {
        String m = ex.getCause() != null && ex.getCause().getMessage() != null ? ex.getCause().getMessage() : ex.getMessage();
        if (m == null) return ex.getClass().getSimpleName();
        int i = m.indexOf("\nSee https");
        return i > 0 ? m.substring(0, i) : m;
    }
}
