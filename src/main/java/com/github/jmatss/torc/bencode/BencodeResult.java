package com.github.jmatss.torc.bencode;

public class BencodeResult<T> {
    private final BencodeType type;
    private final T value;

    BencodeResult(BencodeType bencodeType, T value) {
        this.type = bencodeType;
        this.value = value;
    }

    public BencodeType getType() {
        return this.type;
    }

    public T getValue() {
        return this.value;
    }
}
