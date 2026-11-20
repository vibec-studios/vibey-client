package dev.henny.vibeyclient.module.social.discord.core;

public record RpcLabels(String menu, String singleplayer, String multiplayer, String hosting, String away, String busy) {
    public static final RpcLabels EN = new RpcLabels("In the menus", "Singleplayer", "Multiplayer", "Hosting a world", "Away", "Busy");

    public String place(RpcPlace p) {
        return switch (p) {
            case MENU -> menu;
            case SINGLEPLAYER -> singleplayer;
            case MULTIPLAYER -> multiplayer;
            case HOSTING -> hosting;
        };
    }
}
