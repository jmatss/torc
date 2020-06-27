package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.BencodeException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class TrackerTest {
    private static byte[] peerId;   // 20 'A's

    @BeforeAll
    public static void setUp() {
        peerId = new byte[20];
        Arrays.fill(peerId, (byte) 'A');
    }

    @Test
    public void testCreateNewTrackerObjectCorrectly() throws NoSuchAlgorithmException {
        String filename = "test.data";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        List<TorrentFile> files = new ArrayList<>(1);
        files.add(new TorrentFile(0, 4, path));

        var infoHash = new InfoHash("".getBytes());
        var tracker = new Tracker(files, infoHash, peerId);

        // EXPECTED
        byte[] expectedPeerId = peerId;
        long expectedUploaded = 0;
        long expectedDownloaded = 0;
        long expectedLeft = 4;
        long expectedInterval = Tracker.DEFAULT_INTERVAL;
        long expectedSeeders = 0;
        long expectedLeechers = 0;
        // Expect peers isEmpty

        // ACTUAL
        byte[] actualPeerId = tracker.getPeerId();
        long actualUploaded = tracker.getUploaded();
        long actualDownloaded = tracker.getDownloaded();
        long actualLeft = tracker.getLeft();
        long actualInterval = tracker.getInterval();
        long actualSeeders = tracker.getSeeders();
        long actualLeechers = tracker.getLeechers();
        Map<String, Peer> actualPeers = tracker.getPeers();

        // ASSERT
        assertArrayEquals(expectedPeerId, actualPeerId);
        assertEquals(expectedUploaded, actualUploaded);
        assertEquals(expectedDownloaded, actualDownloaded);
        assertEquals(expectedLeft, actualLeft);
        assertEquals(expectedInterval, actualInterval);
        assertEquals(expectedSeeders, actualSeeders);
        assertEquals(expectedLeechers, actualLeechers);
        assertTrue(actualPeers.isEmpty());
    }

    @Test
    public void testSendTrackerRequestCorrectly()
    throws IOException, BencodeException, InterruptedException, NoSuchAlgorithmException {
        int listenPort = 7301;
        String filename = "test.data";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        List<TorrentFile> files = new ArrayList<>(1);
        files.add(new TorrentFile(0, 4, path));

        // The serverThread will receive a GET request from the tracker object.
        // It will gather the headers in the headers map so that they can be checked.
        var infoHash = new InfoHash("".getBytes());
        var tracker = new Tracker(files, infoHash, peerId);
        var headers = new TreeMap<String, String>();
        Thread serverThread = new Thread(
                () -> {
                    try (
                            var serverSocket = new ServerSocket(listenPort);
                            var conn = serverSocket.accept();
                            var in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    ) {
                        conn.setSoTimeout(1000);
                        String line;
                        while (!(line = in.readLine()).isEmpty()) {
                            String[] keyAndValue = line.split(":", 2);
                            if (keyAndValue.length == 2)
                                headers.put(keyAndValue[0].trim(), keyAndValue[1].trim());
                        }
                    } catch (Exception e) {
                        fail(e);
                    }
                }
        );

        serverThread.start();
        try {
            tracker.sendRequest(new URL("http://127.0.0.1:" + listenPort));
        } catch (ConnectException e) {
            // The serverThread will cancel the socket connection after it receives the
            // request from the tracker object. So a ConnectionException will be thrown.
        }
        serverThread.join();

        // EXPECTED
        // TODO: String expectedInfoHash = "...";
        String expectedPeerId = new String(peerId);
        // expectedPort between 0 and 2^16
        long expectedUploaded = 0;
        long expectedDownloaded = 0;
        long expectedLeft = 4;
        long expectedCompact = 1;
        String expectedEvent = Event.STARTED.getValue();

        // ACTUAL
        // TODO: String actualInfoHash = getStringFromMapAndAssertNotNull(headers, "info_hash")
        String actualPeerId = getStringFromMapAndAssertNotNull(headers, "peer_id");
        int actualPort = (int) getLongFromMapAndAssertNotNull(headers, "port");
        long actualUploaded = getLongFromMapAndAssertNotNull(headers, "uploaded");
        long actualDownloaded = getLongFromMapAndAssertNotNull(headers, "downloaded");
        long actualLeft = getLongFromMapAndAssertNotNull(headers, "left");
        long actualCompact = getLongFromMapAndAssertNotNull(headers, "compact");
        String actualEvent = getStringFromMapAndAssertNotNull(headers, "event");

        // ASSERT
        // TODO: assertEquals(expectedInfoHash, actualInfoHash);
        assertEquals(expectedPeerId, actualPeerId);
        assertTrue(actualPort > 0 && actualPort < (1 << 16));
        assertEquals(expectedUploaded, actualUploaded);
        assertEquals(expectedDownloaded, actualDownloaded);
        assertEquals(expectedLeft, actualLeft);
        assertEquals(expectedCompact, actualCompact);
        assertEquals(expectedEvent, actualEvent);
    }

    private String getStringFromMapAndAssertNotNull(Map<String, String> headers, String key) {
        String value = headers.get(key);
        assertNotNull(value);
        return value;
    }

    private long getLongFromMapAndAssertNotNull(Map<String, String> headers, String key) {
        return Long.parseLong(getStringFromMapAndAssertNotNull(headers, key));
    }

    @Test
    public void testReceivesTrackerBinaryModelResponseCorrectly()
    throws IOException, BencodeException, InterruptedException, NoSuchAlgorithmException {
        int listenPort = 7302;
        String filename = "trackerResponseBinary.data";
        String path = Objects.requireNonNull(getClass().getClassLoader().getResource(filename)).getFile();
        List<TorrentFile> files = new ArrayList<>(0);

        byte[][] trackerResponse = new byte[][]{
                "HTTP/1.1 200\r\n".getBytes(),
                "Content-Type: text/plain\r\n".getBytes(),
                "Connection: close\r\n".getBytes(),
                "\r\n".getBytes(),
                Files.readAllBytes(new File(path).toPath())
        };

        // The serverThread will ignore everyhting received in the GET request.
        // This test will only test if the Tracker object receives the response correctly.
        var infoHash = new InfoHash("".getBytes());
        var tracker = new Tracker(files, infoHash, peerId);
        Thread serverThread = new Thread(
                () -> {
                    try (
                            var serverSocket = new ServerSocket(listenPort);
                            var conn = serverSocket.accept();
                            var in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                            var out = conn.getOutputStream();
                    ) {
                        conn.setSoTimeout(1000);
                        while (!in.readLine().isEmpty()) {
                            // Ignore everything read, only want to test what is sent.
                        }

                        for (byte[] data : trackerResponse)
                            out.write(data);
                        out.flush();

                    } catch (Exception e) {
                        fail(e);
                    }
                }
        );

        serverThread.start();
        tracker.sendRequest(new URL("http://127.0.0.1:" + listenPort));
        serverThread.join();

        // EXPECTED
        long expectedInterval = 123;
        String expectedTrackerId = "id";
        long expectedAmountOfSeeders = 1;
        long expectedAmountOfLeechers = 2;
        Peer[] expectedPeers = {
                new Peer("65.66.67.68", 8257),  // IP:PORT => ABCD:(space)A
                new Peer("97.98.99.100", 8289)  // IP:PORT => abcd:(space)a
        };

        // ACTUAL
        long actualInterval = tracker.getInterval();
        String actualTrackerId = tracker.getTrackerId();
        long actualAmountOfSeeders = tracker.getSeeders();
        long actualAmountOfLeechers = tracker.getLeechers();
        Map<String, Peer> actualPeers = tracker.getPeers();

        // ASSERT
        assertEquals(expectedInterval, actualInterval);
        assertEquals(expectedTrackerId, actualTrackerId);
        assertEquals(expectedAmountOfSeeders, actualAmountOfSeeders);
        assertEquals(expectedAmountOfLeechers, actualAmountOfLeechers);
        assertEquals(expectedPeers.length, actualPeers.size());
        for (Peer expectedPeer : expectedPeers) {
            Peer actualPeer = actualPeers.get(expectedPeer.getIp().getHostAddress());
            assertNotNull(actualPeer);
            assertEquals(expectedPeer.getIp(), actualPeer.getIp());
            assertEquals(expectedPeer.getPort(), actualPeer.getPort());
        }
    }

    // TODO: test dictionary model
}
