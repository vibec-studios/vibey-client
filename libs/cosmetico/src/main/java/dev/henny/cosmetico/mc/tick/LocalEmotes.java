package dev.henny.cosmetico.mc.tick;

import dev.henny.cosmetico.Cosmetico;
import dev.henny.cosmetico.anim.EmotePlay;
import dev.henny.cosmetico.mc.compat.McCamera;
import dev.henny.cosmetico.state.Avatar;
import dev.henny.cosmetico.state.EmoteListener;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.UUID;

public final class LocalEmotes {
    private LocalEmotes() {}

    private static CameraType cameraBefore, cameraSet;
    private static double startX, startZ;
    private static int playingTicks;

    public static boolean play(String emoteId) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || emoteId == null) return false;
        Avatar a = Cosmetico.engine().avatars.get(player.getUUID());
        if (!Cosmetico.engine().emotes.play(a, emoteId)) return false;
        startX = player.getX();
        startZ = player.getZ();
        playingTicks = 0;
        EmotePlay e = a.emote;
        if (Cosmetico.config().thirdPersonEmotes && e.bundle.emote != null && e.bundle.emote.thirdPerson()
                && McCamera.get() == CameraType.FIRST_PERSON) {
            cameraBefore = CameraType.FIRST_PERSON;
            cameraSet = CameraType.THIRD_PERSON_BACK;
            McCamera.set(cameraSet);
        }
        return true;
    }

    public static String stop() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return null;
        Avatar a = Cosmetico.engine().avatars.peek(player.getUUID());
        if (a == null || a.emote == null || a.emote.stopping()) return null;
        String id = a.emote.id;
        Cosmetico.engine().emotes.stop(a);
        return id;
    }

    public static String playing() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player == null ? null : Cosmetico.emotes().playing(player.getUUID());
    }

    public static void forgetCamera() {
        cameraBefore = null;
        cameraSet = null;
    }

    static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        Avatar a = Cosmetico.engine().avatars.peek(mc.player.getUUID());
        if (a == null || a.emote == null || a.emote.stopping()) return;
        playingTicks++;
        EmotePlay e = a.emote;
        if (e.bundle.emote != null && e.bundle.emote.stopOnMove() && playingTicks > 4 && MoveCheck.moving(mc, startX, startZ)) stop();
    }

    static EmoteListener cameraRestore() {
        return new EmoteListener() {
            @Override
            public void ended(UUID player, String emoteId) {
                LocalPlayer self = Minecraft.getInstance().player;
                if (player != null && self != null && player.equals(self.getUUID())) restoreCamera();
            }
        };
    }

    private static void restoreCamera() {
        if (cameraBefore != null && McCamera.get() == cameraSet) McCamera.set(cameraBefore);
        forgetCamera();
    }
}
