package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.BencodeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static com.github.jmatss.torc.TMP_CONST.ENCODING;
import static org.junit.jupiter.api.Assertions.*;

public class TorrentTest {
    private static byte[] peerId;

    @BeforeAll
    public static void setUp() {
        peerId = new byte[20];
        Arrays.fill(peerId, (byte) 'A');
    }

    @Test
    public void testCreatingTorrentFromSingleFileTorrentCorrectly() throws IOException, BencodeException {
        String filename = "test1.torrent";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("test.data");
        long expectedPieceLength = 4;
        long expectedLength = 4;
        long expectedIndex = 0;
        Path expectedPath = expectedName;
        byte[] expectedPiece = toDigest("a94a8fe5ccb19ba61c4c0873d391e987982fbbd3");

        // ACTUAL
        URL actualAnnounce = torrent.getAnnounce();
        Path actualName = torrent.getName();
        long actualPieceLength = torrent.getPieceLength();
        long actualLength = torrent.getFiles().get(0).getLength();
        long actualIndex = torrent.getFiles().get(0).getIndex();
        Path actualPath = torrent.getFiles().get(0).getPath();
        byte[] actualPiece = torrent.getPieces()[0];

        // ASSERT
        assertEquals(expectedAnnounce, actualAnnounce);
        assertEquals(expectedName, actualName);
        assertEquals(1, torrent.getPieces().length);
        assertArrayEquals(expectedPiece, actualPiece);
        assertEquals(expectedPieceLength, actualPieceLength);
        assertEquals(expectedLength, actualLength);
        assertEquals(expectedIndex, actualIndex);
        assertEquals(expectedPath, actualPath);
    }

    @Test
    public void testCreatingTorrentFromSingleFileTorrentWithTwoPiecesCorrectly()
    throws IOException, BencodeException {
        String filename = "test2.torrent";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("test.data");
        long expectedPieceLength = 2;
        long expectedLength = 4;
        long expectedIndex = 0;
        Path expectedPath = expectedName;
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
        assertEquals(expectedPieces.length, actualPieces.length);
        for (int i = 0; i < expectedPieces.length; i++)
            assertArrayEquals(expectedPieces[i], actualPieces[i]);
    }

    @Test
    public void testCreatingTorrentFromMultiFileTorrentCorrectly() throws IOException, BencodeException {
        String filename = "test3.torrent";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        Torrent torrent = new Torrent(path, peerId);

        // EXPECTED
        URL expectedAnnounce = new URL("https://www.testURL.se");
        Path expectedName = Paths.get("");
        long expectedPieceLength = 7;
        int expectedAmountOfFiles = 2;
        byte[] expectedPiece = toDigest("8865096550cb3439aa1ae8ce209fd18a0be8d76f");
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
        List<TorrentFile> actualTorrentFiles = torrent.getFiles();

        // ASSERT
        assertEquals(expectedAnnounce, actualAnnounce);
        assertEquals(expectedName, actualName);
        assertEquals(1, torrent.getPieces().length);
        assertArrayEquals(expectedPiece, actualPiece);
        assertEquals(expectedPieceLength, actualPieceLength);
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
            res[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return res;
    }
}
