package dev.henny.vibeyclient.client.names;

import com.mojang.authlib.GameProfile;

import java.util.UUID;

public final class Profiles {
    private Profiles() {}

    public static UUID id(GameProfile p) {
        if (p == null) return null;
        //? if >=1.21.9 {
        return p.id();
        //?} else
        /*return p.getId();*/
    }

    public static String name(GameProfile p) {
        if (p == null) return null;
        //? if >=1.21.9 {
        return p.name();
        //?} else
        /*return p.getName();*/
    }
}
