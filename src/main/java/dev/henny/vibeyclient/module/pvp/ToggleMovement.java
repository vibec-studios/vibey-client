package dev.henny.vibeyclient.module.pvp;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;

/**
 * Toggle sprint / toggle sneak: a press of the vanilla sprint (sneak) key switches a "held for you" state;
 * {@code KeyboardInput} then reports the key as held ({@code KeyboardInputMixin}). Vanilla's own Toggle mode (Controls
 * → Sprint/Sneak: Toggle) keeps working and wins; this only adds the HUD indicator and survives screens and deaths the
 * same way. Toggled sneak pauses while flying or riding (it would sink / dismount).
 */
public final class ToggleMovement {
    private ToggleMovement() {}

    private static boolean sprint, sneak;
    private static boolean sprintWasActive, sneakWasActive;

    static void tick(PvpModule m) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || p.isDeadOrDying()) {
            reset();
            return;
        }
        Options o = mc.options;
        boolean sprintOn = m.sprintActive() && !o.toggleSprint().get();
        boolean sneakOn = m.sneakActive() && !o.toggleCrouch().get();
        // clicks from before the feature was on must not toggle
        if (sprintOn && !sprintWasActive) drain(o.keySprint);
        if (sneakOn && !sneakWasActive) drain(o.keyShift);
        sprintWasActive = sprintOn;
        sneakWasActive = sneakOn;
        if (sprintOn) {
            while (o.keySprint.consumeClick()) sprint = !sprint;
        } else {
            sprint = false;
        }
        if (sneakOn) {
            while (o.keyShift.consumeClick()) sneak = !sneak;
            if (p.isPassenger()) sneak = false; // would dismount right away
        } else {
            sneak = false;
        }
    }

    private static void drain(net.minecraft.client.KeyMapping k) {
        while (k.consumeClick()) {
            // drop
        }
    }

    /** Sprint is toggled on and should be reported as held. */
    public static boolean sprintHeld() {
        return sprint;
    }

    /** Sneak is toggled on and should be reported as held (not while flying or riding). */
    public static boolean sneakHeld() {
        if (!sneak) return false;
        LocalPlayer p = Minecraft.getInstance().player;
        return p != null && !p.getAbilities().flying && !p.isPassenger();
    }

    static boolean sneakToggled() {
        return sneak;
    }

    public static void reset() {
        sprint = false;
        sneak = false;
    }

    /** For tests and the HUD preview. */
    public static void force(boolean sprintOn, boolean sneakOn) {
        sprint = sprintOn;
        sneak = sneakOn;
    }
}
