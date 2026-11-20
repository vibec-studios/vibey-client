package dev.henny.vibeyclient.module.social.discord.core;

import com.google.gson.JsonObject;

public final class RpcPayload {
    public static final String LARGE_IMAGE = "vibey";
    public static final int MAX_TEXT = 128;

    private RpcPayload() {}

    public static JsonObject build(RpcInputs in, RpcLabels labels) {
        if (!in.presenceShown() || "invisible".equals(in.status())) return null;
        JsonObject a = new JsonObject();
        a.addProperty("details", clip(labels.place(in.place())));
        String state = state(in, labels);
        if (state != null) a.addProperty("state", clip(state));
        if (in.startedAtMillis() > 0) {
            JsonObject ts = new JsonObject();
            ts.addProperty("start", in.startedAtMillis() / 1000L);
            a.add("timestamps", ts);
        }
        JsonObject assets = new JsonObject();
        assets.addProperty("large_image", LARGE_IMAGE);
        if (in.version() != null && !in.version().isBlank()) assets.addProperty("large_text", clip(in.version()));
        a.add("assets", assets);
        return a;
    }

    public static boolean showsServer(RpcInputs in) {
        return in.place() == RpcPlace.MULTIPLAYER && in.serverAllowed() && !in.streamerHidesServer()
                && in.server() != null && !in.server().isBlank();
    }

    private static String state(RpcInputs in, RpcLabels labels) {
        if (showsServer(in)) return in.server();
        if ("away".equals(in.status())) return labels.away();
        if ("busy".equals(in.status())) return labels.busy();
        return null;
    }

    private static String clip(String s) {
        return s.length() <= MAX_TEXT ? s : s.substring(0, MAX_TEXT);
    }
}
