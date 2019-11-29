package com.github.jmatss.torc.bencode;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static com.github.jmatss.torc.bencode.Bencode.ENCODING;
import static org.junit.jupiter.api.Assertions.*;

// TODO: Use get"Structure"() instead of getNext() during tests(?)
public class BencodeTest {
    private <T> void runTest(byte[] input, T[] expectedOutputs, BencodeType[] expectedTypes)
    throws EOFException, BencodeException, UnsupportedEncodingException {
        if (expectedOutputs.length != expectedTypes.length)
            throw new IllegalArgumentException("expectedOutputs.length != expectedTypes.length");

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult;
        for (int i = 0; i < expectedOutputs.length; i++) {
            actualResult = bencode.getNext();

            assertEquals(expectedTypes[i], actualResult.getType(), "(index = " + i + ")");
            assertEquals(expectedOutputs[i], actualResult.getValue(), "(index = " + i + ")");
        }
    }

    @Test
    public void testDecodeNumber() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = "i123e".getBytes();
        Long[] expectedOutputs = {123L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeTwoNumbers() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = ("i123e" + "i45e").getBytes();
        Long[] expectedOutputs = {123L, 45L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER, BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeString() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = "4:test".getBytes();
        BencodeString[] expectedOutputs = {new BencodeString("test", ENCODING)};
        BencodeType[] expectedTypes = {BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeTwoStrings() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = ("4:test" + "10:0123456789").getBytes();
        BencodeString[] expectedOutputs = {
                new BencodeString("test", ENCODING),
                new BencodeString("0123456789", ENCODING)
        };
        BencodeType[] expectedTypes = {BencodeType.STRING, BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeEmptyList() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = "le".getBytes();
        // expectedOutput => empty list;
        BencodeType expectedType = BencodeType.LIST;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        List<BencodeType> actualResultList = (List<BencodeType>) actualResult.getValue();
        assertTrue(actualResultList.isEmpty());
    }

    @Test
    public void testDecodeListOfOneString() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = ("l" + "4:test" + "e").getBytes();
        int expectedListSize = 1;
        BencodeString expectedOutputInner = new BencodeString("test", ENCODING);
        BencodeType expectedType = BencodeType.LIST;
        BencodeType expectedTypeInner = BencodeType.STRING;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        List<BencodeResult> actualResultList = (List<BencodeResult>) actualResult.getValue();
        assertEquals(expectedListSize, actualResultList.size());

        BencodeResult actualResultInner = actualResultList.get(0);
        assertEquals(expectedTypeInner, actualResultInner.getType());
        assertEquals(expectedOutputInner, actualResultInner.getValue());
    }

    @Test
    public void testDecodeEmptyDictionary() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = ("de").getBytes();
        // expectedOutput => empty map;
        BencodeType expectedType = BencodeType.DICTIONARY;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        Map<BencodeString, BencodeResult> actualResultMap = (Map<BencodeString, BencodeResult>) actualResult.getValue();
        assertTrue(actualResultMap.isEmpty());
    }


    @Test
    public void testDecodeDictionaryOfOneNumber() throws EOFException, BencodeException, UnsupportedEncodingException {
        byte[] input = ("d" + "3:key" + "i123e" + "e").getBytes();
        int expectedMapSize = 1;
        BencodeString expectedKey = new BencodeString("key", ENCODING);
        long expectedValue = 123L;
        BencodeType expectedType = BencodeType.DICTIONARY;
        BencodeType expectedTypeValue = BencodeType.NUMBER;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        Map<BencodeString, BencodeResult> actualResultMap = (Map<BencodeString, BencodeResult>) actualResult.getValue();
        assertEquals(expectedMapSize, actualResultMap.size());

        assertTrue(actualResultMap.containsKey(expectedKey));
        BencodeResult actualResultInner = actualResultMap.get(expectedKey);
        assertEquals(expectedTypeValue, actualResultInner.getType());
        assertEquals(expectedValue, actualResultInner.getValue());
    }
}
