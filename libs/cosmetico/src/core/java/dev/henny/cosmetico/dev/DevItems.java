package dev.henny.cosmetico.dev;

import com.google.gson.JsonObject;

import java.util.List;

public interface DevItems {
    PushResult push(byte[] bundleBytes, PushAction action, byte[] thumbnail);

    PushResult push(JsonObject bundle, PushAction action, byte[] thumbnail);

    boolean remove(String id);

    List<String> ids();
}
