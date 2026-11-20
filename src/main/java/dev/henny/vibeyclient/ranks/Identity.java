package dev.henny.vibeyclient.ranks;

import java.util.List;

public record Identity(String rank, List<String> tags, String style) {
    public static final Identity EMPTY = new Identity(null, List.of(), null);

    public boolean isEmpty() {
        return rank == null && tags.isEmpty() && style == null;
    }

    public Identity withoutRanks() {
        return rank == null && tags.isEmpty() ? this : new Identity(null, List.of(), style);
    }
}
