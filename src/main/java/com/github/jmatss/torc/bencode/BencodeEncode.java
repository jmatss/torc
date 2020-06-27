package com.github.jmatss.torc.bencode;

import java.util.TreeMap;
import com.github.jmatss.torc.util.DynamicByteBuffer;

/**
 * Class to encode data into bencode.
 */
class BencodeEncode extends Bencode {
    /**
     * Encodes the given BencodeData and returns the encoded data as an array
     * of bytes.
     * 
     * @param data the data to be encoded.
     * @return the encoded data as an array of bytes.
     * @throws BencodeException if a invalid BencodeType is found.
     */
    protected static byte[] encodeRecursive(BencodeData<Object> data)
    throws BencodeException {
        DynamicByteBuffer accumulatedResult = new DynamicByteBuffer();
        return encodeRecursive(data, accumulatedResult);
    }

    /**
     * Encodes the given BencodeData recursively and returns the encoded data
     * as an array of bytes.
     * 
     * @param data the data to be encoded.
     * @param accumulatedResult accumulates encoded data during recursion.
     * @return the encoded data as an array of bytes.
     * @throws BencodeException if a invalid BencodeType is found.
     */
    private static byte[] encodeRecursive(BencodeData<Object> data, DynamicByteBuffer accumulatedResult)
    throws BencodeException {
        switch (data.getType()) {
            case NUMBER:
                var number = data.getNumber();

                accumulatedResult
                    .put((byte)BencodeType.NUMBER.getChar())
                    .putLongBytes(number)
                    .put((byte)BencodeType.END.getChar());
                break;

            case STRING:
                var benString = data.getBencodeString();

                accumulatedResult
                    .putIntBytes(benString.getBytes().length)
                    .put((byte)STRING_SEPARATOR)
                    .put(benString.getBytes());
                break;

            case LIST:
                var list = data.getList();

                accumulatedResult.put((byte)BencodeType.LIST.getChar());
                for (BencodeData<Object> item : list) {
                    encodeRecursive(item, accumulatedResult);
                }
                accumulatedResult.put((byte)BencodeType.END.getChar());
                break;

            case DICTIONARY:
                var dictionary = data.getDictionary();

                accumulatedResult.put((byte)BencodeType.DICTIONARY.getChar());

                // Create a new TreeMap just to make sure that it is sorted
                // correctly according to the keys in the map.
                for (var dictionaryEntry : new TreeMap<>(dictionary).entrySet()) {
                    // Need to convert the BencodeString `keyString` to
                    // BencodeData before calling this function again
                    // recursively since it expects BencodeData.
                    BencodeString keyString = dictionaryEntry.getKey();
                    BencodeData<Object> key = new BencodeData<>(BencodeType.STRING, keyString);
                    BencodeData<Object> value = dictionaryEntry.getValue();

                    encodeRecursive(key, accumulatedResult);
                    encodeRecursive(value, accumulatedResult);
                }

                accumulatedResult.put((byte) BencodeType.END.getChar());
                break;

            default:
                throw new BencodeException("Parsed incorrect BencodeType.");
        }

        return accumulatedResult.getBytes();
    }
}
