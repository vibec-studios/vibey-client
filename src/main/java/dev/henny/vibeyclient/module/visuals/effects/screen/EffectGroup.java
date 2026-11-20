package dev.henny.vibeyclient.module.visuals.effects.screen;

import java.util.List;

public record EffectGroup(String id, String title, String note, List<EffectItem> items) {
    public String getId() { return id; }
    public String getTitle() { return title; }
    public String getNote() { return note; }
    public List<EffectItem> getItems() { return items; }
}
