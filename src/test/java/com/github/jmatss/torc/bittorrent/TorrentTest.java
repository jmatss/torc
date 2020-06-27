package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.BencodeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.github.jmatss.torc.TMP_CONST.ENCODING;
import static org.junit.jupiter.api.Assertions.*;

public class TorrentTest {
    private static byte[] peerId;   // 20 'A's

    @BeforeAll
    public static void setUp() {
        peerId = new byte[20];
        Arrays.fill(peerId, (byte) 'A');
    }

    @Test
    public void testCreatingTorrentFromSingleFileTorrentCorrectly()
            throws IOException, BencodeException, NoSuchAlgorithmException {
        String filename = "test1.torrent";
        String path = getTestDataPath(filename);
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("test.data");
        long expectedPieceLength = 4;
        long expectedLength = 4;
        long expectedIndex = 0;
        Path expectedPath = expectedName;
        var expectedInfoHash = new InfoHash(toDigest("8744e7c94baaec2757f8cc52a98e966a9ad4cf78"), true);
        byte[] expectedPiece = toDigest("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");

        // ACTUAL
        URL actualAnnounce = torrent.getAnnounce();
        Path actualName = torrent.getName();
        long actualPieceLength = torrent.getPieceLength();
        long actualLength = torrent.getFiles().get(0).getLength();
        long actualIndex = torrent.getFiles().get(0).getIndex();
        Path actualPath = torrent.getFiles().get(0).getPath();
        var actualInfoHash = torrent.getInfoHash();
        byte[] actualPiece = torrent.getPieces()[0];

        // ASSERT
        assertEquals(expectedAnnounce, actualAnnounce);
        assertEquals(expectedName, actualName);
        assertEquals(1, torrent.getPieces().length);
        assertArrayEquals(expectedPiece, actualPiece);
        assertEquals(expectedPieceLength, actualPieceLength);
        assertEquals(expectedLength, actualLength);
        assertEquals(expectedIndex, actualIndex);
        assertArrayEquals(expectedInfoHash.getBytes(), actualInfoHash.getBytes());
        assertEquals(expectedPath, actualPath);
    }

    @Test
    public void testCreatingTorrentFromSingleFileTorrentWithTwoPiecesCorrectly()
            throws IOException, BencodeException, NoSuchAlgorithmException {
        String filename = "test2.torrent";
        String path = getTestDataPath(filename);
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("test.data");
        long expectedPieceLength = 2;
        long expectedLength = 4;
        long expectedIndex = 0;
        Path expectedPath = expectedName;
        var expectedInfoHash = new InfoHash(toDigest("fba9b668222968632d32c8abf571514b29b68b71"), true);
        byte[][] expectedPieces = {
                toDigest("33e9505d12942e8259a3c96fb6f88ed325b95797"),
                toDigest("9b02d9974c14e623c9ffbed7360beacbf0dcb95f")
        };

        // ACTUAL
        URL actualAnnounce = torrent.getAnnounce();
        Path actualName = torrent.getName();
        long actualPieceLength = torrent.getPieceLength();
        long actualLength = torrent.getFiles().get(0).getLength();
        long actualIndex = torrent.getFiles().get(0).getIndex();
        Path actualPath = torrent.getFiles().get(0).getPath();
        var actualInfoHash = torrent.getInfoHash();
        byte[][] actualPieces = new byte[torrent.getPieces().length][];
        for (int i = 0; i < torrent.getPieces().length; i++)
            actualPieces[i] = torrent.getPieces()[i];

        // ASSERT
        assertEquals(expectedAnnounce, actualAnnounce);
        assertEquals(expectedName, actualName);
        assertEquals(expectedPieceLength, actualPieceLength);
        assertEquals(expectedLength, actualLength);
        assertEquals(expectedIndex, actualIndex);
        assertEquals(expectedPath, actualPath);
        assertArrayEquals(expectedInfoHash.getBytes(), actualInfoHash.getBytes());
        assertEquals(expectedPieces.length, actualPieces.length);
        for (int i = 0; i < expectedPieces.length; i++)
            assertArrayEquals(expectedPieces[i], actualPieces[i]);
    }

    @Test
    public void testCreatingTorrentFromMultiFileTorrentCorrectly()
            throws IOException, BencodeException, NoSuchAlgorithmException {
        String filename = "test3.torrent";
        String path = getTestDataPath(filename);
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("");
        long expectedPieceLength = 7;
        int expectedAmountOfFiles = 2;
        byte[] expectedPiece = toDigest("8865096550cb3439aa1ae8ce209fd18a0be8d76f");
        var expectedInfoHash = new InfoHash(toDigest("5f67b42614fd18d3bb6e61a091dc53144fc3eda8"), true);
        long[] expectedLengths = {
                "test".getBytes(ENCODING).length,
                "åäö".getBytes(ENCODING).length
        };
        long[] expectedIndices = {0, 1};
        Path[] expectedPaths = {
                Paths.get("test.data"),
                Paths.get("åäö.data")
        };

        // ACTUAL
        URL actualAnnounce = torrent.getAnnounce();
        Path actualName = torrent.getName();
        long actualPieceLength = torrent.getPieceLength();
        byte[] actualPiece = torrent.getPieces()[0];
        var actualInfoHash = torrent.getInfoHash();
        List<TorrentFile> actualTorrentFiles = torrent.getFiles();

        // ASSERT
        assertEquals(expectedAnnounce, actualAnnounce);
        assertEquals(expectedName, actualName);
        assertEquals(1, torrent.getPieces().length);
        assertArrayEquals(expectedPiece, actualPiece);
        assertEquals(expectedPieceLength, actualPieceLength);
        assertArrayEquals(expectedInfoHash.getBytes(), actualInfoHash.getBytes());
        assertEquals(expectedAmountOfFiles, actualTorrentFiles.size());
        for (int i = 0; i < actualTorrentFiles.size(); i++) {
            var tf = actualTorrentFiles.get(i);
            assertEquals(expectedLengths[i], tf.getLength());
            assertEquals(expectedIndices[i], tf.getIndex());
            assertEquals(expectedPaths[i], tf.getPath());
        }
    }

    private byte[] toDigest(String s) {
        byte[] res = new byte[s.length() / 2];
        for (int i = 0; i < s.length(); i += 2) {
            int first = (Character.digit(s.charAt(i), 16) << 4);
            int second = Character.digit(s.charAt(i + 1), 16);
            res[i / 2] = (byte) (first + second);
        }
        return res;
    }

    private String getTestDataPath(String filename) {
        try {
            String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
            var urlDecodedPath = new URI(path);
            return urlDecodedPath.getPath();
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
}
