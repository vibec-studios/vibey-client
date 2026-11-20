package dev.henny.r3.mc.render;

import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
//? if >=1.21.9 {
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
//?} else {
/*import dev.henny.r3.mc.compat.Game;
import net.minecraft.client.renderer.MultiBufferSource;
*///?}

final class FontDraw {
    private static final Style BOLD = Style.EMPTY.withBold(true);

    private FontDraw() {}

    static FormattedCharSequence sequence(TextItem t) {
        if (t.font != null) return FormattedCharSequence.forward(t.text, FontStyles.of(t.font, t.bold));
        return FormattedCharSequence.forward(t.text, t.bold ? BOLD : Style.EMPTY);
    }

    static Font.DisplayMode mode(TextMode mode) {
        return switch (mode) {
            case NORMAL -> Font.DisplayMode.NORMAL;
            case POLYGON_OFFSET -> Font.DisplayMode.POLYGON_OFFSET;
            case SEE_THROUGH -> Font.DisplayMode.SEE_THROUGH;
        };
    }

    //? if >=1.21.9 {
    static void submit(OrderedSubmitNodeCollector out, TextItem t) {
        PoseStack pose = new PoseStack();
        pose.last().pose().set(t.pose);
        out.submitText(pose, t.x, t.y, sequence(t), t.shadow, mode(t.mode), t.light, t.argb, 0, 0);
    }
    //?} else {
    /*static void draw(MultiBufferSource buffers, TextItem t) {
        Game.mc().font.drawInBatch(sequence(t), t.x, t.y, t.argb, t.shadow, t.pose, buffers, mode(t.mode), 0, t.light);
    }
    *///?}
}
