package dev.henny.vui.event;

import dev.henny.vui.dom.Element;

public class Event {
    public static final int CAPTURE = 1;
    public static final int TARGET = 2;
    public static final int BUBBLE = 3;

    private String type;
    private Element target;
    private Element currentTarget;
    private Object detail;
    private boolean bubbles;
    private boolean stopped;
    private boolean prevented;
    private int phase;

    public Event reset(String type, Element target, boolean bubbles, Object detail) {
        this.type = type;
        this.target = target;
        this.currentTarget = target;
        this.bubbles = bubbles;
        this.detail = detail;
        this.stopped = false;
        this.prevented = false;
        this.phase = TARGET;
        return this;
    }

    public String type() {
        return type;
    }

    public Element target() {
        return target;
    }

    public Element currentTarget() {
        return currentTarget;
    }

    public Object detail() {
        return detail;
    }

    public boolean bubbles() {
        return bubbles;
    }

    public int phase() {
        return phase;
    }

    public void stopPropagation() {
        stopped = true;
    }

    public void preventDefault() {
        prevented = true;
    }

    public boolean stopped() {
        return stopped;
    }

    public boolean prevented() {
        return prevented;
    }

    public void enter(Element current, int phase) {
        this.currentTarget = current;
        this.phase = phase;
    }

    public Object value() {
        return detail;
    }
}
