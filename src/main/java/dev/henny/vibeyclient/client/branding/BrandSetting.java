package dev.henny.vibeyclient.client.branding;

import dev.henny.vibeyclient.api.GlobalSettings;

final class BrandSetting {
    private BrandSetting() {}

    static boolean show() {
        try {
            return GlobalSettings.serverBrand.on();
        } catch (Throwable t) {
            return true;
        }
    }
}
