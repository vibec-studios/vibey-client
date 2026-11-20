package dev.henny.companio.pack;

import java.io.IOException;
import java.util.List;

public interface PackFiles {
    String name();

    String location();

    List<String> list();

    boolean exists(String path);

    byte[] read(String path) throws IOException;

    long stamp();
}
