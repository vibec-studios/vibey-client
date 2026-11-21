package dev.henny.vibeyclient.module.multiplayer.voice;

import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.VoiceHostEvent;
import dev.henny.vibeyclient.module.multiplayer.MultiplayerModule;
import dev.henny.vibeyclient.module.multiplayer.host.HostSession;
import dev.henny.vibeyclient.module.multiplayer.host.Voice;

@ForgeVoicechatPlugin
public final class VoicePlugin implements VoicechatPlugin {
    @Override
    public String getPluginId() {
        return "vibeyclient";
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        try {
            registration.registerEvent(VoiceHostEvent.class, VoicePlugin::onVoiceHost);
        } catch (Throwable t) {
            MultiplayerModule.LOG.info("Simple Voice Chat without VoiceHostEvent, voice over the relay is off: {}", t.toString());
        }
    }

    private static void onVoiceHost(VoiceHostEvent event) {
        String host = Voice.voiceHost();
        if (host != null && HostSession.isHosting()) event.setVoiceHost(host);
    }
}
