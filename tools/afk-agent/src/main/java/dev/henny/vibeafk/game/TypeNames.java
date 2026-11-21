package dev.henny.vibeafk.game;

final class TypeNames {
    private TypeNames() {}

    static String primitiveDesc(String t) {
        if (t.equals("int")) return "I";
        if (t.equals("long")) return "J";
        if (t.equals("float")) return "F";
        if (t.equals("double")) return "D";
        if (t.equals("boolean")) return "Z";
        if (t.equals("byte")) return "B";
        if (t.equals("char")) return "C";
        if (t.equals("short")) return "S";
        if (t.equals("void")) return "V";
        return null;
    }
}
