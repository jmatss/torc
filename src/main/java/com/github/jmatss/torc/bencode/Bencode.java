package com.github.jmatss.torc.bencode;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

/**
 * Class used to decode/encode between bencoding and Java types. See:
 * https://wiki.theory.org/index.php/BitTorrentSpecification#Bencoding
 *
 * Conversion between bencode types and Java types: 
 * dictionary <=> SortedMap<BencodeString, BencodeData<Object>>
 * list       <=> List<BencodeDataObject>
 * string     <=> BencodeString (contains both the original bytes and a utf-8 string)
 * number     <=> long
 */
public class Bencode {
    public static final String ENCODING = "utf-8";
    public static final char STRING_SEPARATOR = ':';

    /**
     * Encodes the given BencodeData and returns the encoded data as an array
     * of bytes.
     * 
     * @param data the data to be encoded.
     * @return the encoded data as an array of bytes.
     * @throws BencodeException if a invalid BencodeType is found.
     */
    public static byte[] encode(BencodeData<Object> data) throws BencodeException {
        return BencodeEncode.encodeRecursive(data);
    }

    /**
     * Decodes the data from the given InputStream.
     * 
     * @param inputStream the data to be decoded.
     * @return the decoded data as a BencodeData object.
     * @throws BencodeException if a invalid BencodeType is found.
     * @throws IOException      if unable to read `inputStream`.
     * @throws EOFException     if EOF reached unexpectedly.
     */
    public static BencodeData<Object> decode(InputStream inputStream)
    throws BencodeException, IOException, EOFException {
        return new BencodeDecode(inputStream).getNext();
    }

    /**
     * Decodes the data from the given InputStream as a map/dictionary.
     * 
     * @param inputStream the data to be decoded.
     * @return the decoded data as a map.
     * @throws BencodeException if a invalid BencodeType is found.
     * @throws IOException      if unable to read `inputStream`.
     * @throws EOFException     if EOF reached unexpectedly.
     */
    public static Map<BencodeString, BencodeData<Object>> decodeDictionary(InputStream inputStream)
    throws BencodeException, IOException, EOFException {
        return new BencodeDecode(inputStream).getDictionary();
    }

    /**
     * Helper function to convert a byte to a character and at the same time prevent
     * sign extension.
     * 
     * @param b byte to convert
     * @return the character.
     */
    protected char byteToChar(byte b) {
        return (char) (b & 0xff);
    }
}
