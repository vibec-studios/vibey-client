package dev.henny.cosmetico.anim;

public interface TimeSource {
    long nanos();

    TimeSource SYSTEM = System::nanoTime;
}
