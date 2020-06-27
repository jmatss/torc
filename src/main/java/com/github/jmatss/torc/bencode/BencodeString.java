package com.github.jmatss.torc.bencode;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

/**
 * This class is needed since strings in a bencoded file can be either
 * UTF-8 encoded strings or binary data.
 * 
 * The UTF-8 strings will be stored as a String in java and the binary data
 * will be stored as byes.
 */
public class BencodeString implements Comparable<BencodeString> {
    private final byte[] bytes;
    private final String string;

    public BencodeString(byte[] bytes, String encoding) throws UnsupportedEncodingException {
        this.bytes = bytes;
        this.string = new String(bytes, encoding);
    }

    public BencodeString(String string, String encoding) throws UnsupportedEncodingException {
        this.bytes = string.getBytes(encoding);
        this.string = string;
    }

    @Override
    public int compareTo(BencodeString other) {
        return Arrays.compare(this.bytes, other.bytes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (!(o instanceof BencodeString)) {
            return false;
        }

        BencodeString other = (BencodeString) o;
        return Arrays.equals(this.getBytes(), other.getBytes());
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public String getString() {
        return this.string;
    }
}
