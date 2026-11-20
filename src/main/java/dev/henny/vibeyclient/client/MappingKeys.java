package dev.henny.vibeyclient.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class MappingKeys {
    private MappingKeys() {}

    public static final int MOUSE_BASE = -100;

    public static int value(KeyMapping mapping) {
        if (mapping.isUnbound()) return -1;
        InputConstants.Key key = InputConstants.getKey(mapping.saveString());
        if (key.getType() == InputConstants.Type.MOUSE) return MOUSE_BASE + Keys.mouseFromNative(key.getValue());
        return Keys.fromNative(key.getValue());
    }

    public static void set(KeyMapping mapping, int value) {
        if (value <= MOUSE_BASE && value > MOUSE_BASE - 16) {
            mapping.setKey(InputConstants.Type.MOUSE.getOrCreate(Keys.mouseToNative(value - MOUSE_BASE)));
            KeyMapping.resetMapping();
            net.minecraft.client.Minecraft.getInstance().options.save();
            return;
        }
        ClientCompat.rebind(mapping, value < 0 ? -1 : Keys.toNativeAny(value));
    }

    public static String conflicts(KeyMapping mapping) {
        StringBuilder names = new StringBuilder();
        for (KeyMapping m : ClientCompat.conflicts(mapping)) {
            if (names.length() > 0) names.append(", ");
            names.append(ClientCompat.mappingName(m));
        }
        return names.toString();
    }
}
