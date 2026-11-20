package dev.henny.vibeyclient.ui.vui.dialog;

public record ConfirmState(String icon, String title, String text, String yes, boolean danger) {
    public boolean open() {
        return true;
    }
}
