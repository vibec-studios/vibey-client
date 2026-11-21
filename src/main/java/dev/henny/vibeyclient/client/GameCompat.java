package dev.henny.vibeyclient.client;

import com.mojang.authlib.GameProfile;

public final class GameCompat {
    private GameCompat() {}

    public static String profileName(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.name();
        //?} else
        /*return profile.getName();*/
    }

    public static java.util.UUID profileId(GameProfile profile) {
        //? if >=1.21.9 {
        return profile.id();
        //?} else
        /*return profile.getId();*/
    }

    public static long windowHandle() {
        //? if >=1.21.9 {
        return net.minecraft.client.Minecraft.getInstance().getWindow().handle();
        //?} else
        /*return net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();*/
    }
}
