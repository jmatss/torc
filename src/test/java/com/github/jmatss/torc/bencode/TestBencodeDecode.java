package com.github.jmatss.torc.bencode;

import org.junit.jupiter.api.Test;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

public class TestBencodeDecode {
    @Test
    public void TestDecodeNumberCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream("i123e".getBytes());
        BencodeType expectedOutputType = BencodeType.NUMBER;
        long expectedOutput = 123;

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedOutput, actualOutput.getNumber());
    }

    @Test
    public void TestDecodeStringCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream("4:test".getBytes());
        BencodeType expectedOutputType = BencodeType.STRING;
        String expectedOutput = "test";

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedOutput, actualOutput.getString());
    }

    @Test
    public void TestDecodeEmptyListCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream("le".getBytes());
        BencodeType expectedOutputType = BencodeType.LIST;
        int expectedListSize = 0;

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedListSize, actualOutput.getList().size());
    }

    @Test
    public void TestDecodeListContainingOneItemCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream(("l" + "i456e" + "e").getBytes());
        BencodeType expectedOutputType = BencodeType.LIST;
        int expectedListSize = 1;

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedListSize, actualOutput.getList().size());
    }

    @Test
    public void TestDecodeEmptyMapCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream("de".getBytes());
        BencodeType expectedOutputType = BencodeType.DICTIONARY;
        int expectedMapSize = 0;

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedMapSize, actualOutput.getDictionary().size());
    }

    @Test
    public void TestDecodeMapContainingOneItemCorrectly() throws Exception {
        // ARRANGE
        InputStream inputStream = new ByteArrayInputStream(("d" + "3:key" + "i456e" + "e").getBytes());
        BencodeType expectedOutputType = BencodeType.DICTIONARY;
        int expectedMapSize = 1;

        // ACT
        BencodeData<Object> actualOutput = Bencode.decode(inputStream);

        // ASSERT
        assertEquals(expectedOutputType, actualOutput.getType());
        assertEquals(expectedMapSize, actualOutput.getDictionary().size());
    }
}
