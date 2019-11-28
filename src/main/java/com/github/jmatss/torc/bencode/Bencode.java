package com.github.jmatss.torc.bencode;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.CharBuffer;
import java.nio.file.Files;
import java.util.*;

public class Bencode {
    public static final String ENCODING = "utf-8";
    public static final char STRING_SEPARATOR = ':';
    private CharBuffer buffer;

    public Bencode(CharBuffer buffer) {
        this.buffer = buffer;
    }

    public Bencode(char[] fileContent) {
        this(CharBuffer.allocate(fileContent.length).put(fileContent));
        this.buffer.rewind();
    }

    public Bencode(File file) throws IOException {
        this(
                new String(
                        Files.readAllBytes(file.toPath()),
                        ENCODING
                ).toCharArray()
        );
    }

    public BencodeResult getNext() throws EOFException, BencodeException, UnsupportedEncodingException {
        BencodeType type = getNextType();
        BencodeResult<?> result;

        switch (type) {
            case NUMBER:
                result = new BencodeResult<>(type, getNumber());
                break;
            case LIST:
                result = new BencodeResult<>(type, getList());
                break;
            case DICTIONARY:
                result = new BencodeResult<>(type, getDictionary());
                break;
            case STRING:
                result = new BencodeResult<>(type, getString());
                break;
            default:
                throw new BencodeException("Parsed incorrect BencodeType.");
        }

        return result;
    }

    public BencodeType getNextType() throws EOFException, BencodeException {
        if (!this.buffer.hasRemaining())
            throw new EOFException();

        int oldPosition = this.buffer.position();
        char typeChar = this.buffer.get();
        BencodeType type = BencodeType.valueOf(typeChar);
        if (type == null)
            throw new BencodeException("Incorrect format of bencoded data. Expected BencodeType, got: " + typeChar);

        // Reset position so that the next "read" of the buffer starts at the correct position.
        this.buffer.position(oldPosition);
        return type;
    }

    public String getString() throws BencodeException {
        StringBuilder sb = new StringBuilder();

        char current;
        while ((current = buffer.get()) != STRING_SEPARATOR) {
            if (!Character.isDigit(current))
                throw new BencodeException("Received non digit character while parsing string length: " + current);
            sb.append(current);
        }

        // TODO: Make sure that the length isn't a weird value ex. extremely large.
        int stringLength = Integer.parseInt(sb.toString());
        char[] str = new char[stringLength];
        this.buffer.get(str);

        return new String(str);
    }

    public Long getNumber() {
        this.buffer.position(this.buffer.position() + 1);   // "Remove" 'i'.

        StringBuilder sb = new StringBuilder();
        char current;
        while (Character.isDigit(current = buffer.get())) {
            sb.append(current);
        }
        // The last iteration of the while loop "removes" the 'e'.

        return Long.parseLong(sb.toString());
    }

    public Map<String, BencodeResult> getDictionary() throws EOFException, BencodeException, UnsupportedEncodingException {
        this.buffer.position(this.buffer.position() + 1);   // "Remove" 'd'.
        Map<String, BencodeResult> map = new HashMap<>();

        BencodeType nextKeyType;
        while ((nextKeyType = getNextType()) != BencodeType.END) {
            if (nextKeyType != BencodeType.STRING)
                throw new BencodeException("Received a non String key for a dictionary.");
            map.put(getString(), getNext());
        }

        this.buffer.position(this.buffer.position() + 1);   // "Remove" 'e'.
        return map;
    }

    public List<BencodeResult> getList() throws EOFException, BencodeException, UnsupportedEncodingException {
        this.buffer.position(this.buffer.position() + 1);   // "Remove" 'l'.
        List<BencodeResult> list = new ArrayList<>();

        while (getNextType() != BencodeType.END) {
            list.add(getNext());
        }

        this.buffer.position(this.buffer.position() + 1);   // "Remove" 'e'.
        return list;
    }
}
