package com.github.jmatss.torc.bencode;

/**
 * Class used to decode/encode between bencoding and Java types.
 * See: https://wiki.theory.org/index.php/BitTorrentSpecification#Bencoding
 *
 * Conversion between bencode types and Java types:
 * dictionary <=> SortedMap<BencodeString, BencodeResult>
 * list       <=> List<BencodeResult>
 * string     <=> BencodeString (contains both the original bytes and a utf-8 string)
 * integer    <=> long
 */
public abstract class Bencode {
    public static final String ENCODING = "utf-8";
    public static final char STRING_SEPARATOR = ':';

    // Prevent sign extension.
    char byteToChar(byte b) {
        return (char) (b & 0xff);
    }
}
