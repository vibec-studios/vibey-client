package dev.henny.vibeyclient.module.media.video;

import com.mojang.blaze3d.platform.InputConstants;
import dev.henny.vibeyclient.client.Keys;
import dev.henny.vibeyclient.ui.Gfx;
import dev.henny.vibeyclient.ui.Theme;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Locale;

/**
 * Item-free placing: the screen sticks to the block face under the crosshair (walls, floor, ceiling), the mouse
 * wheel changes its size, R turns floor/ceiling screens, right-click places, left-click or the screens key cancels.
 * Clicks are swallowed at the start of the tick, before vanilla would use them.
 */
public final class Placement {
    private static final float STEP = 0.25f;
    private final Video video;
    private MediaScreen draft;
    private boolean moving;
    private JsonBackup backup;
    private int lastSlot = -1;
    private int quarter;
    private boolean rWasDown;
    public boolean valid;

    private record JsonBackup(String dim, double x, double y, double z, Direction normal, Direction up) {}

    Placement(Video video) {
        this.video = video;
    }

    public boolean active() {
        return draft != null;
    }

    public MediaScreen draft() {
        return draft;
    }

    /** {@code moving}: an existing screen is picked up (cancel puts it back). */
    public void start(MediaScreen s, boolean moving) {
        cancel();
        this.draft = s;
        this.moving = moving;
        this.backup = moving ? new JsonBackup(s.dimension, s.x, s.y, s.z, s.normal, s.up) : null;
        this.quarter = 0;
        this.lastSlot = -1;
        this.valid = false;
        Minecraft.getInstance().setScreen(null);
    }

    public void cancel() {
        if (draft == null) return;
        if (moving && backup != null) {
            draft.dimension = backup.dim;
            draft.x = backup.x;
            draft.y = backup.y;
            draft.z = backup.z;
            draft.normal = backup.normal;
            draft.up = backup.up;
        }
        draft = null;
        backup = null;
    }

    /** START of the client tick: wheel (via the hotbar slot) and clicks. */
    void preTick(Minecraft mc) {
        if (draft == null || mc.player == null || mc.screen != null) {
            lastSlot = -1;
            return;
        }
        var inv = mc.player.getInventory();
        int slot = inv.getSelectedSlot();
        if (lastSlot >= 0 && slot != lastSlot) {
            int d = slot - lastSlot;
            if (d > 4) d -= 9;
            if (d < -4) d += 9;
            float a = draft.aspect();
            float step = InputConstants.isKeyDown(mc.getWindow(), Keys.LEFT_SHIFT) ? STEP / 4 : STEP;
            draft.width = Math.max(0.5f, Math.min(16f, draft.width - Math.signum(d) * step));
            draft.height = draft.width / a;
            inv.setSelectedSlot(lastSlot);
            slot = lastSlot;
        }
        lastSlot = slot;
        boolean place = false;
        boolean abort = false;
        while (mc.options.keyUse.consumeClick()) place = true;
        while (mc.options.keyAttack.consumeClick()) abort = true;
        if (abort) {
            cancel();
        } else if (place && valid) {
            MediaScreen s = draft;
            boolean wasMoving = moving;
            draft = null;
            backup = null;
            video.placed(s, wasMoving);
        }
    }

    /** End of the tick: follow the crosshair. */
    void tick(Minecraft mc) {
        if (draft == null || mc.player == null) return;
        boolean r = InputConstants.isKeyDown(mc.getWindow(), Keys.R);
        if (r && !rWasDown && mc.screen == null) quarter = (quarter + 1) % 4;
        rWasDown = r;
        HitResult hr = mc.hitResult;
        if (!(hr instanceof BlockHitResult bhr) || hr.getType() != HitResult.Type.BLOCK) {
            valid = false;
            return;
        }
        Direction face = bhr.getDirection();
        Direction up = Direction.UP;
        if (face.getAxis().isVertical()) {
            Direction facing = mc.player.getDirection();
            up = face == Direction.UP ? facing : facing.getOpposite();
            for (int i = 0; i < quarter; i++) up = up.getClockWise();
        }
        Vec3 loc = bhr.getLocation();
        double x = loc.x, y = loc.y, z = loc.z;
        if (face.getAxis() != Direction.Axis.X) x = snap(x);
        if (face.getAxis() != Direction.Axis.Y) y = snap(y);
        if (face.getAxis() != Direction.Axis.Z) z = snap(z);
        x += face.getStepX() * 0.02;
        y += face.getStepY() * 0.02;
        z += face.getStepZ() * 0.02;
        draft.dimension = video.dimension();
        draft.x = x;
        draft.y = y;
        draft.z = z;
        draft.normal = face;
        draft.up = up;
        valid = true;
    }

    private static double snap(double v) {
        return Math.round(v * 8) / 8.0;
    }

    void renderHud(Gfx g) {
        if (draft == null) return;
        String size = String.format(Locale.ROOT, "%.2f × %.2f m", draft.width, draft.height);
        String line = Video.tr("place.hint", size);
        int w = g.width(line) + 16;
        int x = (g.screenWidth() - w) / 2;
        int y = g.screenHeight() - 72;
        g.rect(x, y, w, 18, 0xC0101014);
        g.outline(x, y, w, 18, Theme.alpha(Theme.BRAND, 0.8f));
        g.text(line, x + 8, y + 5, valid ? Theme.TEXT : Theme.MUTED);
    }
}
