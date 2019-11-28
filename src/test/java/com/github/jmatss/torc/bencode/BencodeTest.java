package com.github.jmatss.torc.bencode;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class BencodeTest {
    private <T> void runTest(char[] input, T[] expectedOutputs, BencodeType[] expectedTypes)
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
        char[] input = "i123e".toCharArray();
        Long[] expectedOutputs = {123L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeTwoNumbers() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = ("i123e" + "i45e").toCharArray();
        Long[] expectedOutputs = {123L, 45L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER, BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeString() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = "4:test".toCharArray();
        String[] expectedOutputs = {"test"};
        BencodeType[] expectedTypes = {BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeTwoStrings() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = ("4:test" + "10:0123456789").toCharArray();
        String[] expectedOutputs = {"test", "0123456789"};
        BencodeType[] expectedTypes = {BencodeType.STRING, BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeEmptyList() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = "le".toCharArray();
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
        char[] input = ("l" + "4:test" + "e").toCharArray();
        int expectedListSize = 1;
        String expectedOutputInner = "test";
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
        char[] input = ("de").toCharArray();
        // expectedOutput => empty map;
        BencodeType expectedType = BencodeType.DICTIONARY;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        Map<String, BencodeResult> actualResultMap = (Map<String, BencodeResult>) actualResult.getValue();
        assertTrue(actualResultMap.isEmpty());
    }


    @Test
    public void testDecodeDictionaryOfOneNumber() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = ("d" + "3:key" + "i123e" + "e").toCharArray();
        int expectedMapSize = 1;
        String expectedKey = "key";
        long expectedValue = 123L;
        BencodeType expectedType = BencodeType.DICTIONARY;
        BencodeType expectedTypeValue = BencodeType.NUMBER;

        Bencode bencode = new Bencode(input);
        BencodeResult actualResult = bencode.getNext();

        assertEquals(expectedType, actualResult.getType());
        Map<String, BencodeResult> actualResultMap = (Map<String, BencodeResult>) actualResult.getValue();
        assertEquals(expectedMapSize, actualResultMap.size());

        assertTrue(actualResultMap.containsKey(expectedKey));
        BencodeResult actualResultInner = actualResultMap.get(expectedKey);
        assertEquals(expectedTypeValue, actualResultInner.getType());
        assertEquals(expectedValue, actualResultInner.getValue());
    }
}
