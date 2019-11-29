package com.github.jmatss.torc.bencode;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

public class BencodeString {
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof BencodeString) {
            BencodeString other = (BencodeString) o;
            return Arrays.equals(this.getBytes(), other.getBytes()) && this.string.equals(other.string);
        }

        return false;
    }

    @Override
    public int hashCode() {
        return 31 * this.string.hashCode() + Arrays.hashCode(this.bytes);
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public String getString() {
        return this.string;
    }
}
