package dev.henny.vibeyclient.ui.vui.settings;

import dev.henny.vui.bind.State;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SettingValue extends State<Object> {
    private final Supplier<Object> read;
    private final Consumer<Object> write;

    public SettingValue(Supplier<Object> read, Consumer<Object> write) {
        super(read.get());
        this.read = read;
        this.write = write;
    }

    @Override
    public void set(Object next) {
        try {
            write.accept(next);
        } catch (RuntimeException e) {
            dev.henny.vibeyclient.Vibey.LOG.warn("Setting write failed: {}", e.toString());
        }
        sync();
    }

    public void sync() {
        super.set(read.get());
    }
}
