package dev.henny.vibeyclient.module.social.discord.core;

public record RpcInputs(
        boolean presenceShown,
        String status,
        RpcPlace place,
        String server,
        boolean serverAllowed,
        boolean streamerHidesServer,
        long startedAtMillis,
        String version) {}
