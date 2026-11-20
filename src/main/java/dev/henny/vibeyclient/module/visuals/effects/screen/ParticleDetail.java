package dev.henny.vibeyclient.module.visuals.effects.screen;

import dev.henny.vibeyclient.module.visuals.particles.ParticleBurst;
import dev.henny.vibeyclient.module.visuals.particles.StyleStore;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleNames;
import dev.henny.vibeyclient.module.visuals.particles.core.ParticleStyle;
import dev.henny.vibeyclient.module.visuals.particles.core.TintMath;
import dev.henny.vibeyclient.ui.vui.settings.SettingValue;
import dev.henny.vui.expr.Values;

import java.util.function.UnaryOperator;

public final class ParticleDetail {
    public static final int[] TINTS = {0xFFFFFF, 0xFF3B3B, 0xFF7A00, 0xFFE14D, 0x3BFF6B, 0x3FE0C5, 0x4D7BFF, 0x8B6CFF,
            0xFF5CE1};
    private static final long BURST_DELAY_MS = 220;

    public final String id;
    public final String name;
    public final boolean testable;
    public final SettingValue amount;
    public final SettingValue size;
    public final SettingValue tint;
    public final SettingValue strength;
    public final SettingValue hidden;
    private long burstAt;

    ParticleDetail(String id) {
        this.id = id;
        this.name = ParticleNames.pretty(id);
        this.testable = ParticleBurst.testable(id);
        amount = value(() -> style().amount(), v -> s -> s.withAmount(number(v)));
        size = value(() -> style().size(), v -> s -> s.withSize(number(v)));
        tint = value(() -> TintMath.hex(style().tint()), v -> s -> s.withTint(color(v, s.tint())).withStrength(Math.max(s.strength(), 60)));
        strength = value(() -> style().strength(), v -> s -> s.withStrength(number(v)));
        hidden = value(() -> style().hidden(), v -> s -> s.withHidden(Values.truthy(v)));
    }

    public ParticleStyle style() {
        return StyleStore.styles().get(id);
    }

    public String getAmountText() { return style().amount() + "%"; }
    public String getSizeText() { return style().size() + "%"; }
    public String getStrengthText() { return style().strength() + "%"; }
    public boolean isChanged() { return !style().vanilla(); }
    public String getSwatchColor() { return TintMath.hex(TintMath.apply(0xFFFFFFFF, style().tint(), style().strength())); }
    public int getSwatchPx() { return Math.max(6, Math.min(18, Math.round(12 * style().sizeFactor()))); }

    public void reset() {
        StyleStore.set(id, ParticleStyle.VANILLA);
        sync();
        burstSoon();
    }

    public boolean burst() {
        burstAt = 0;
        return ParticleBurst.spawn(id);
    }

    void sync() {
        amount.sync();
        size.sync();
        tint.sync();
        strength.sync();
        hidden.sync();
        if (burstAt > 0 && System.currentTimeMillis() >= burstAt) burst();
    }

    private void burstSoon() {
        if (testable) burstAt = System.currentTimeMillis() + BURST_DELAY_MS;
    }

    private SettingValue value(java.util.function.Supplier<Object> read,
                               java.util.function.Function<Object, UnaryOperator<ParticleStyle>> change) {
        return new SettingValue(read, v -> {
            StyleStore.set(id, change.apply(v).apply(style()));
            burstSoon();
        });
    }

    private static int number(Object v) {
        return v instanceof Number n ? Math.round(n.floatValue()) : Integer.parseInt(String.valueOf(v).trim());
    }

    private static int color(Object v, int fallback) {
        if (v instanceof Number n) return n.intValue() & 0xFFFFFF;
        return TintMath.parseHex(String.valueOf(v), fallback);
    }
}
