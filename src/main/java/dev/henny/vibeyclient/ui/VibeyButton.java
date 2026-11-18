package dev.henny.vibeyclient.ui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
//? if >=26.1 {
/*import net.minecraft.client.gui.GuiGraphicsExtractor;
*///?} else
import net.minecraft.client.gui.GuiGraphics;
//? if >=1.21.9 {
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
//?}

/**
 * A real (vanilla) widget in the Vibe look, for vanilla screens we decorate (pause menu row, classic title screen):
 * square, 1 px line, icon + label, 150 ms hover, keyboard focus ring, works with Tab navigation and narration.
 * Version glue for render/click/key lives here so callers only give a label, an {@link Icons} name and an action.
 */
public class VibeyButton extends AbstractWidget {
    private final String icon;
    private final Runnable action;
    private final Anim hover = new Anim(0f);
    private boolean iconOnly;
    private boolean accent;
    private boolean subtle;
    private java.util.function.IntSupplier badge = () -> 0;

    public VibeyButton(int x, int y, int w, int h, Component label, String icon, Runnable action) {
        super(x, y, w, h, label);
        this.icon = icon;
        this.action = action;
    }

    /** Draws only the icon (label still used for narration and the tooltip). */
    public VibeyButton iconOnly(boolean v) {
        this.iconOnly = v;
        // vanilla's deferred tooltip: drawn after everything else on the screen (the title logo, other widgets)
        setTooltip(v ? net.minecraft.client.gui.components.Tooltip.create(getMessage()) : null);
        return this;
    }

    /** Brand-coloured left bar (the "Vibey" entry). */
    public VibeyButton accent(boolean v) {
        this.accent = v;
        return this;
    }

    public VibeyButton badge(int n) {
        this.badge = () -> n;
        return this;
    }

    /** Badge read every frame (e.g. unread messages). */
    public VibeyButton badge(java.util.function.IntSupplier n) {
        this.badge = n;
        return this;
    }

    /** Lighter look for icon buttons on top of the vanilla title panorama. */
    public VibeyButton subtle(boolean v) {
        this.subtle = v;
        return this;
    }

    /** Width needed for {@code label} with an icon. */
    public static int widthFor(String label) {
        return Minecraft.getInstance().font.width(label) + 28;
    }

    protected void draw(Gfx g, int mouseX, int mouseY) {
        int x = getX(), y = getY(), w = getWidth(), h = getHeight();
        boolean hot = isHoveredOrFocused() && active;
        float t = hover.to(hot ? 1f : 0f);
        if (subtle) {
            g.rect(x, y, w, h, Theme.mix(0x8C000000, 0xE618181F, t));
            g.outline(x, y, w, h, Theme.mix(0x3DFFFFFF, Theme.BRAND, t));
        } else {
            g.rect(x, y, w, h, Theme.mix(0xE60B0B0E, 0xF018181F, t));
            g.outline(x, y, w, h, Theme.mix(Theme.LINE2, accent ? Theme.BRAND : 0x50FFFFFF, t));
            if (accent) g.rect(x, y, 2, h, Theme.BRAND);
        }
        int fg = !active ? Theme.DIM : Theme.mix(subtle ? 0xFFD4D4D8 : Theme.MUTED, Theme.TEXT, Math.max(t, accent ? 0.6f : 0f));
        int iy = y + (h - Icons.SIZE) / 2;
        String label = getMessage().getString();
        if (iconOnly || label.isEmpty()) {
            if (icon != null) Icons.draw(g, icon, x + (w - Icons.SIZE) / 2, iy, accent ? Theme.mix(Theme.BRAND, 0xFFFFFFFF, t) : fg);
        } else {
            int tw = g.width(label);
            int content = (icon != null ? Icons.SIZE + 6 : 0) + tw;
            int cx = x + Math.max(6, (w - content) / 2);
            if (icon != null) {
                Icons.draw(g, icon, cx, iy, accent ? Theme.mix(Theme.BRAND, 0xFFFFFFFF, t) : fg);
                cx += Icons.SIZE + 6;
            }
            g.text(g.clip(label, x + w - 4 - cx), cx, y + (h - 8) / 2, fg);
        }
        int n;
        try {
            n = badge.getAsInt();
        } catch (RuntimeException e) {
            n = 0;
        }
        if (n > 0) {
            String b = n > 9 ? "9+" : String.valueOf(n);
            int bw = Math.max(9, g.width(b) + 4);
            g.rect(x + w - bw - 2, y + 2, bw, 9, Theme.BRAND);
            g.text(b, x + w - bw - 2 + (bw - g.width(b)) / 2 + 1, y + 3, 0xFFFFFFFF);
        }
        if (isHovered() && !iconOnly && g.width(label) > w - 28 && !label.isEmpty()) {
            Ui.drawTooltipBox(g, label, mouseX, mouseY);
        }
    }

    protected void press() {
        action.run();
    }

    //? if >=26.1 {
    /*@Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g, Minecraft.getInstance().font), mouseX, mouseY);
    }
    *///?} else {
    @Override
    protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float delta) {
        draw(new Gfx(g, Minecraft.getInstance().font), mouseX, mouseY);
    }
    //?}

    //? if >=1.21.9 {
    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        press();
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        // Enter / Space / keypad Enter; 26.3 reports SDL scancodes (40 / 44 / 88), so ask the event itself there
        //? if >=26.3 {
        /*boolean select = event.isSelection();
        *///?} else
        boolean select = event.key() == 257 || event.key() == 32 || event.key() == 335;
        if (active && visible && isFocused() && select) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            press();
            return true;
        }
        return false;
    }
    //?} else {
    /*@Override
    public void onClick(double mouseX, double mouseY) {
        press();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (active && visible && isFocused() && (keyCode == 257 || keyCode == 32 || keyCode == 335)) {
            playDownSound(Minecraft.getInstance().getSoundManager());
            press();
            return true;
        }
        return false;
    }
    *///?}

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        defaultButtonNarrationText(out);
    }
}
