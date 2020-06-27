package com.github.jmatss.torc.bencode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * Class to decode bencode-encoded data.
 */
class BencodeDecode extends Bencode {
    /**
     * Contains the bytes of the contents to decode.
     */
    private ByteBuffer buffer;

    protected BencodeDecode(ByteBuffer buffer) {
        this.buffer = buffer;
    }

    protected BencodeDecode(byte[] fileContent) {
        this(ByteBuffer.wrap(fileContent));
    }

    protected BencodeDecode(InputStream inputStream) throws IOException {
        this(inputStream.readAllBytes());
    }

    /**
     * Used to see what type the next "structure" is (dict, list, string or integer).
     *
     * @return the BencodeType of the next structure.
     * @throws EOFException     if this function is called when there are no more data to decode.
     * @throws BencodeException if the given bencoded data has incorrect format.
     */
    protected BencodeType getNextType() throws EOFException, BencodeException {
        if (!this.buffer.hasRemaining()) {
            throw new EOFException();
        }

        int oldPosition = this.buffer.position();

        byte nextTypeByte = this.buffer.get();
        char nextTypeChar = super.byteToChar(nextTypeByte);

        BencodeType nextType = BencodeType.valueOf(nextTypeChar);
        if (nextType == null) {
            String msg = "Incorrect format of bencoded data.";
            msg += " Expected BencodeType, got(int): " + (int)nextTypeChar;
            throw new BencodeException(msg);
        }

        // Reset position so that the next "read" of the buffer starts at the correct position.
        this.buffer.position(oldPosition);

        return nextType;
    }

    /**
     * Gets the next bencode "structure" (dict, list, string or integer).
     *
     * @return the next structure wrapped inside a BencodeResult
     * @throws EOFException                 if this function is called when there are no more data to decode.
     * @throws BencodeException             if the given bencoded data has incorrect format.
     * @throws UnsupportedEncodingException if utf-8 isn't supported.
     */
    protected BencodeData<Object> getNext()
    throws EOFException, BencodeException, UnsupportedEncodingException {
        Object value;
        BencodeType type = getNextType();

        switch (type) {
            case NUMBER:
                value = getNumber();
                break;

            case LIST:
                value = getList();
                break;

            case DICTIONARY:
                value = getDictionary();
                break;

            case STRING:
                value = getString();
                break;

            default:
                throw new BencodeException("Parsed incorrect BencodeType: \"" + type + "\".");
        }

        return new BencodeData<>(type, value);
    }

    /**
     * Returns the next bencode structure which is assumed to be a String.
     * 
     * @return the bencode string.
     * @throws BencodeException             if unable to parse string length.
     * @throws UnsupportedEncodingException if "utf-8" isn't supported on this platform.
     */
    private BencodeString getString() throws BencodeException, UnsupportedEncodingException {
        StringBuilder stringLengthBuf = new StringBuilder();

        // Iterate through all digits until a "string separator" is found
        // and add all digits into the `stringLengthBuf` buffer.
        byte currentByte = this.buffer.get();
        char currentChar = super.byteToChar(currentByte);
        while (currentChar != STRING_SEPARATOR) {
            if (!Character.isDigit(currentChar)) {
                String msg = "Received non digit character while parsing string length: ";
                throw new BencodeException(msg + currentChar);
            }

            stringLengthBuf.append(currentChar);

            currentByte = this.buffer.get();
            currentChar = super.byteToChar(currentByte);
        }

        if (stringLengthBuf.length() == 0) {
            String msg = "Received no digits while parsing string length.";
            throw new BencodeException(msg);
        }

        // Parse the `stringLength` string into a int a get that amount
        // of bytes from `this.buffer` which contains the string that
        // is to be parsed.
        int stringLength = Integer.parseInt(stringLengthBuf.toString());
        byte[] bytes = new byte[stringLength];
        this.buffer.get(bytes);

        return new BencodeString(bytes, ENCODING);
    }

    /**
     * Returns the next bencode structure which is assumed to be a Number.
     * 
     * @return the bencode number as a long.
     */
    private long getNumber() {
        StringBuilder digits = new StringBuilder();

        // "Remove" the first char which is assumes to be 'i'.
        this.buffer.get();

        // Iterate through all digits and add them to the `digits` variable 
        // that will be converted to long before returning it.
        byte currentByte = this.buffer.get();
        char currentChar = super.byteToChar(currentByte);
        while (Character.isDigit(currentChar)) {
            digits.append(currentChar);

            currentByte = this.buffer.get();
            currentChar = super.byteToChar(currentByte);
        }

        // The last iteration of the while loop would have "removed" the ending
        // 'e', so the position of `this.buffer` is correct.
        return Long.parseLong(digits.toString());
    }

    /**
     * Returns the next bencode structure which is assumed to be a SortedMap.
     * 
     * @return the bencoded dictionary as a Map.
     * @throws EOFException                 if EOF reached unexpectedly.
     * @throws BencodeException             if badly formatted bencoding is found.
     * @throws UnsupportedEncodingException if "utf-8" isn't supported on this platform.
     */
    protected SortedMap<BencodeString, BencodeData<Object>> getDictionary()
    throws EOFException, BencodeException, UnsupportedEncodingException {
        var resultMap = new TreeMap<BencodeString, BencodeData<Object>>();

        // "Remove" the first char which is assumes to be 'd'.
        this.buffer.get();

        // Iterate through all key-value pairs in the dictionary and add to
        // the `resultMap`. According to the bencode standard, the keys must
        // be valid bencode strings.
        BencodeType nextType = this.getNextType();
        while (nextType != BencodeType.END) {
            if (nextType != BencodeType.STRING) {
                String msg = "Received a non String key for a dictionary: ";
                throw new BencodeException(msg + nextType);
            }

            BencodeString key = this.getString();
            BencodeData<Object> value = this.getNext();
            resultMap.put(key, value);

            nextType = this.getNextType();
        }

        // "Remove" the ending 'e'.
        this.buffer.get();

        return resultMap;
    }

    /**
     * Returns the next bencode structure which is assumed to be a List.
     * 
     * @return the bencoded List.
     * @throws EOFException                 if EOF reached unexpectedly.
     * @throws BencodeException             if badly formatted bencoding is found.
     * @throws UnsupportedEncodingException if "utf-8" isn't supported on this platform.
     */
    private List<BencodeData<Object>> getList()
    throws EOFException, BencodeException, UnsupportedEncodingException {
        // "Remove" the first char which is assumes to be 'l'.
        this.buffer.get();

        var resultList = new ArrayList<BencodeData<Object>>();

        // Iterate through all values in the list which can be of any type.
        BencodeType nextType = this.getNextType();
        while (nextType != BencodeType.END) {
            BencodeData<Object> value = this.getNext();
            resultList.add(value);

            nextType = this.getNextType();
        }

        // "Remove" the ending 'e'.
        this.buffer.get();

        return resultList;
    }
}
