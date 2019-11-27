package com.github.jmatss.torc.bencode;

import org.junit.jupiter.api.Test;

import java.io.EOFException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

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
        char[] input = new char[]{'i', '1', '2', '3', 'e'};
        Long[] expectedOutputs = {123L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public <T> void testDecodeTwoNumbers() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = new char[]{
                'i', '1', '2', '3', 'e',
                'i', '4', '5', 'e'
        };
        Long[] expectedOutputs = {123L, 45L};
        BencodeType[] expectedTypes = {BencodeType.NUMBER, BencodeType.NUMBER};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeString() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = new char[]{'4', ':', 't', 'e', 's', 't'};
        String[] expectedOutputs = {"test"};
        BencodeType[] expectedTypes = {BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }

    @Test
    public void testDecodeTwoStrings() throws EOFException, BencodeException, UnsupportedEncodingException {
        char[] input = new char[]{
                '4', ':', 't', 'e', 's', 't',
                '1', '0', ':', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };
        String[] expectedOutputs = {"test", "0123456789"};
        BencodeType[] expectedTypes = {BencodeType.STRING, BencodeType.STRING};

        runTest(input, expectedOutputs, expectedTypes);
    }
}
