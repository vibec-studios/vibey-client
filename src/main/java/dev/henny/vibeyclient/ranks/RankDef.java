package dev.henny.vibeyclient.ranks;

public record RankDef(String id, Label label, String icon, int color, int priority, String kind, boolean staff) {
    public boolean canBePrimary() {
        return !"badge".equals(kind);
    }
}
