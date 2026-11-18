package dev.henny.vibeyclient.client;

import dev.henny.vibeyclient.api.ModuleManager;
import dev.henny.vibeyclient.api.VibeyModule;
import dev.henny.vibeyclient.api.setting.Settings.KeybindSetting;
import net.minecraft.client.KeyMapping;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Every key mapping of the client. Mappings are created while settings are declared (module constructors/onLoad)
 * and handed to the loader once at startup: Fabric registers them right after init, NeoForge in
 * {@code RegisterKeyMappingsEvent}. Keybinds of modules that cannot run (conflict, unavailable) are left out.
 * Creating one after that throws.
 */
public final class Keybinds {
    private Keybinds() {}

    private static final List<KeybindSetting> DECLARED = new ArrayList<>();
    private static final List<String> OWNERS = new ArrayList<>();
    private static final List<KeybindSetting> REGISTERED = new ArrayList<>();
    private static List<KeyMapping> mappings;

    public static KeyMapping create(String name, int defaultKey, String ownerGroup, KeybindSetting owner) {
        if (mappings != null) throw new IllegalStateException("Keybinds must be declared at startup (constructor/onLoad): " + name);
        DECLARED.add(owner);
        OWNERS.add(ownerGroup);
        return ClientCompat.newKeyMapping(name, defaultKey);
    }

    /** For the loader (after {@code VibeyCore.init}): the mappings to register. No new ones may be created after. */
    public static synchronized List<KeyMapping> freeze() {
        if (mappings == null) {
            List<KeyMapping> list = new ArrayList<>();
            for (int i = 0; i < DECLARED.size(); i++) {
                VibeyModule m = ModuleManager.get().get(OWNERS.get(i));
                if (m != null && !m.status().loadable()) continue;
                if (!DECLARED.get(i).shouldRegister()) continue;
                REGISTERED.add(DECLARED.get(i));
                list.add(DECLARED.get(i).mapping());
            }
            mappings = Collections.unmodifiableList(list);
        }
        return mappings;
    }

    /** Client tick: delivers clicks to {@link KeybindSetting#fire()} (only while its module is enabled). */
    static void tick() {
        for (int i = 0; i < REGISTERED.size(); i++) {
            KeybindSetting s = REGISTERED.get(i);
            while (s.mapping().consumeClick()) s.fire();
        }
    }
}
