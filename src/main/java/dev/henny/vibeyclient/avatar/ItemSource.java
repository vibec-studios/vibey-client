package dev.henny.vibeyclient.avatar;

/** Where an item comes from. Items with the same id are merged: DEV wins over CATALOG wins over BUILTIN. */
public enum ItemSource {
    /** Shipped inside the jar (assets/vibeyclient/builtin). Works offline. */
    BUILTIN,
    /** Backend catalog (GET /api/v1/cosmetics/catalog), cached on disk. */
    CATALOG,
    /** Pushed from Blockbench over the devlink. Local only, never sent anywhere, shown with a DEV badge. */
    DEV
}
