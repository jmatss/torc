package com.github.jmatss.torc.bencode;

import org.junit.jupiter.api.Test;
import java.util.*;
import com.github.jmatss.torc.TMP_CONST;
import static org.junit.jupiter.api.Assertions.*;

public class TestBencodeEncode {
    @Test
    public void TestEncodeNumberCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = "i123e".getBytes();
        var data = new BencodeData<Object>(BencodeType.NUMBER, 123L);

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeStringCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = "4:test".getBytes();
        var bencodeString = new BencodeString("test", TMP_CONST.ENCODING);
        var data = new BencodeData<Object>(BencodeType.STRING, bencodeString);

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeEmptyListCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = "le".getBytes();
        var data = new BencodeData<Object>(BencodeType.LIST, new ArrayList<>());

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeListContainingOneItemCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = ("l" + "i456e" + "e").getBytes();
        var bencodeNumber = new BencodeData<>(BencodeType.NUMBER, 456L);
        var data = new BencodeData<Object>(BencodeType.LIST, List.of(bencodeNumber));

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeEmptyMapCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = ("de").getBytes();
        var data = new BencodeData<Object>(BencodeType.DICTIONARY, new TreeMap<>());

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeMapContainingOneItemCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = ("d" + "3:key" + "i456e" + "e").getBytes();
        var bencodeString = new BencodeString("key", TMP_CONST.ENCODING);
        var bencodeNumber = new BencodeData<>(BencodeType.NUMBER, 456L);
        var data = new BencodeData<Object>(BencodeType.DICTIONARY, Map.of(bencodeString, bencodeNumber));

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }

    @Test
    public void TestEncodeMapSortsKeysCorrectly() throws Exception {
        // ARRANGE
        byte[] expectedOutput = ("d" + "1:A" + "i123e" + "1:Z" + "i456e" + "e").getBytes();
        var unsortedMap = new HashMap<>();
        unsortedMap.put(new BencodeString("Z", TMP_CONST.ENCODING), new BencodeData<>(BencodeType.NUMBER, 456L));
        unsortedMap.put(new BencodeString("A", TMP_CONST.ENCODING), new BencodeData<>(BencodeType.NUMBER, 123L));
        var data = new BencodeData<Object>(BencodeType.DICTIONARY, unsortedMap);

        // ACT
        byte[] actualOutput = Bencode.encode(data);

        // ASSERT
        assertArrayEquals(expectedOutput, actualOutput);
    }
}
