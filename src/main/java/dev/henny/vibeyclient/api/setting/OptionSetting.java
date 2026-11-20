package dev.henny.vibeyclient.api.setting;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class OptionSetting extends Setting<String, OptionSetting> {
    public record Choice(String value, String label) {}

    private final Supplier<String> read;
    private final Consumer<String> write;
    private final Supplier<List<Choice>> choices;

    public OptionSetting(String key, Supplier<String> read, Consumer<String> write, Supplier<List<Choice>> choices) {
        super(key, "");
        this.read = read;
        this.write = write;
        this.choices = choices;
    }

    public List<Choice> choices() {
        List<Choice> c = choices.get();
        return c == null ? List.of() : c;
    }

    @Override
    public String get() {
        String v = read.get();
        return v == null ? "" : v;
    }

    @Override
    public void set(String newValue) {
        if (newValue == null || newValue.equals(get())) return;
        write.accept(newValue);
    }

    @Override
    public boolean persisted() {
        return false;
    }

    @Override
    public JsonElement toJson() {
        return JsonNull.INSTANCE;
    }

    @Override
    protected String fromJson(JsonElement json) {
        return null;
    }
}
