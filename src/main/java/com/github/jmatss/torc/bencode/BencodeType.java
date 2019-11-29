package com.github.jmatss.torc.bencode;

import java.util.HashMap;
import java.util.Map;

public enum BencodeType {
    NUMBER('i'),
    LIST('l'),
    DICTIONARY('d'),
    STRING('0', '1', '2', '3', '4', '5', '6', '7', '8', '9'),
    END('e');

    private final char[] c;
    private static Map<Character, BencodeType> lookup;

    static {
        lookup = new HashMap<>();
        for (BencodeType t : BencodeType.values()) {
            for (char c : t.c) {
                lookup.put(c, t);
            }
        }
    }

    private BencodeType(char... c) {
        this.c = c;
    }

    public static BencodeType valueOf(char c) {
        return lookup.get(c);
    }

    // FIXME: Make this work in a better way, or remove func completely.
    public char getChar() {
        return this.c[0];
    }
}
