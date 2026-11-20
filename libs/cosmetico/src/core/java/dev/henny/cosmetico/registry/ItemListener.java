package dev.henny.cosmetico.registry;

public interface ItemListener {
    void replaced(Item old, Item now);

    void removed(Item old);
}
