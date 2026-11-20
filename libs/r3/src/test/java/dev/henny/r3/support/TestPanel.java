package dev.henny.r3.support;

import dev.henny.r3.panel.PanelContent;
import dev.henny.r3.panel.PointerButton;
import dev.henny.r3.render.PanelCanvas;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TestPanel implements PanelContent {
    private final Map<String, float[]> controls = new LinkedHashMap<>();
    public final List<String> log = new ArrayList<>();
    public String hovered;
    public int layouts;
    public int width;
    public int height;
    public boolean scrolls;
    public boolean dragsOnBody;
    public float lastX;
    public float lastY;

    public TestPanel control(String id, float x, float y, float w, float h) {
        controls.put(id, new float[] {x, y, w, h});
        return this;
    }

    @Override
    public void layout(int width, int height) {
        layouts++;
        this.width = width;
        this.height = height;
    }

    @Override
    public void draw(PanelCanvas canvas) {
        canvas.fill(0, 0, width, height, 0xFF202020);
        for (String id : controls.keySet()) canvas.text(id, 0, 0, id.equals(hovered) ? 0xFFFFFFFF : 0xFF808080);
    }

    @Override
    public String controlAt(float x, float y) {
        for (Map.Entry<String, float[]> e : controls.entrySet()) {
            float[] r = e.getValue();
            if (x >= r[0] && y >= r[1] && x < r[0] + r[2] && y < r[1] + r[3]) return e.getKey();
        }
        return null;
    }

    @Override
    public void hoverChanged(String controlId) {
        hovered = controlId;
        log.add("hover:" + controlId);
    }

    @Override
    public void pointerMove(float x, float y) {
        lastX = x;
        lastY = y;
    }

    @Override
    public void pointerExit() {
        log.add("exit");
    }

    @Override
    public boolean pointerDown(float x, float y, PointerButton button) {
        log.add("down:" + controlAt(x, y));
        return dragsOnBody;
    }

    @Override
    public void pointerUp(float x, float y, PointerButton button) {
        log.add("up");
    }

    @Override
    public boolean scroll(float x, float y, double amount) {
        if (scrolls) log.add("scroll:" + amount);
        return scrolls;
    }
}
