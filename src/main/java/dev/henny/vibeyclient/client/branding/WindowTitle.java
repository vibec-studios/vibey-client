package dev.henny.vibeyclient.client.branding;

public final class WindowTitle {
    public static final String NAME = "Vibey Client";
    private static final String VANILLA_SEPARATOR = " - ";

    private WindowTitle() {}

    public static String compose(String vanilla, String version, String minecraft) {
        StringBuilder out = new StringBuilder(NAME);
        if (version != null && !version.isBlank()) out.append(' ').append(version.trim());
        if (minecraft != null && !minecraft.isBlank()) out.append(" · Minecraft ").append(minecraft.trim());
        String place = place(vanilla);
        if (!place.isEmpty()) out.append(" – ").append(place);
        return out.toString();
    }

    static String place(String vanilla) {
        if (vanilla == null) return "";
        int cut = vanilla.indexOf(VANILLA_SEPARATOR);
        return cut < 0 ? "" : vanilla.substring(cut + VANILLA_SEPARATOR.length()).trim();
    }
}
