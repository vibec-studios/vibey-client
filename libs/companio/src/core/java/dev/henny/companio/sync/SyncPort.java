package dev.henny.companio.sync;

public interface SyncPort {
    void send(String payload);

    SyncPort NONE = payload -> {};
}
