package com.github.jmatss.torc.bencode;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import static com.github.jmatss.torc.TMP_CONST.ENCODING;

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
        return this.getString().compareTo(other.getString());
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

    public static BencodeString toBenString(String s) throws UnsupportedEncodingException {
        return new BencodeString(s, ENCODING);
    }

    public static String fromBenString(BencodeResult bencodeResult) throws BencodeException {
        return ((BencodeString) bencodeResult.getValue()).getString();
    }

    public byte[] getBytes() {
        return this.bytes;
    }

    public String getString() {
        return this.string;
    }
}
