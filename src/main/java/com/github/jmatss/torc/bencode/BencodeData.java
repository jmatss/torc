package com.github.jmatss.torc.bencode;

import java.util.List;
import java.util.Map;

public class BencodeData<T> {
    private final BencodeType type;
    private final T value;

    BencodeData(BencodeType bencodeType, T value) {
        this.type = bencodeType;
        this.value = value;
    }

    public BencodeType getType() {
        return this.type;
    }

    private T getValue() {
        return this.value;
    }

    /**
     * Validates that the data has the BencodeType `expectedType` before getting
     * the value.
     *
     * @param expectedType the expected type of this data.
     * @return the value.
     * @throws BencodeException if the `expectedType` and `this.type` doesn't match.
     */
    public T getValidatedValue(BencodeType expectedType) throws BencodeException {
        if (expectedType != this.type) {
            String msg = "Bad BencodeType. Expected: " + expectedType;
            msg += ", got: " + this.type;
            throw new BencodeException(msg);
        }
        return this.getValue();
    }

    public long getNumber() throws BencodeException {
        T bencodeNumber = this.getValidatedValue(BencodeType.NUMBER);
        return (long)bencodeNumber;
    }

    public BencodeString getBencodeString() throws BencodeException {
        return (BencodeString)this.getValidatedValue(BencodeType.STRING);
    }

    public String getString() throws BencodeException {
        T bencodeString = this.getValidatedValue(BencodeType.STRING);
        return ((BencodeString)bencodeString).getString();
    }

    public byte[] getBytes() throws BencodeException {
        T bencodeString = this.getValidatedValue(BencodeType.STRING);
        return ((BencodeString)bencodeString).getBytes();
    }

    @SuppressWarnings("unchecked")
    public List<BencodeData<Object>> getList() throws BencodeException {
        T bencodeList = this.getValidatedValue(BencodeType.LIST);
        return (List<BencodeData<Object>>)bencodeList;
    }

    @SuppressWarnings("unchecked")
    public Map<BencodeString, BencodeData<Object>> getDictionary() throws BencodeException {
        T bencodeDict = this.getValidatedValue(BencodeType.DICTIONARY);
        return (Map<BencodeString, BencodeData<Object>>)bencodeDict;
    }
}
