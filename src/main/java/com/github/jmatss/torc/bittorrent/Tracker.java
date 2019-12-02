package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.jmatss.torc.TMP_CONST.ENCODING;
import static com.github.jmatss.torc.bencode.BencodeString.fromBenString;
import static com.github.jmatss.torc.bencode.BencodeString.toBenString;

public class Tracker {
    public static long DEFAULT_INTERVAL = 10; // seconds

    private final Lock mutex;

    // TODO: private final InfoHash infoHash;
    private String trackerId;
    private final byte[] peerId;

    private long uploaded;
    private long downloaded;
    private long left;

    private boolean started;
    private boolean completed;

    private long interval;
    private long seeders;
    private long leechers;

    // The key used in the map is the IP address of the peer in String format.
    private final Map<String, Peer> peers;

    public Tracker(List<TorrentFile> files, byte[] peerId) {
        this.mutex = new ReentrantLock();

        this.peerId = peerId;

        this.uploaded = 0;
        this.downloaded = 0;
        this.left = 0;
        for (TorrentFile file : files)
            this.left += file.getLength();

        this.started = false;
        this.completed = false;

        this.interval = DEFAULT_INTERVAL;
        this.seeders = 0;
        this.leechers = 0;

        this.peers = new HashMap<>();
    }

    public Tracker lock() {
        this.mutex.lock();
        return this;
    }

    public Tracker unlock() {
        this.mutex.unlock();
        return this;
    }

    public void sendCompleted(URL announce) throws IOException, BencodeException {
        if (this.completed)
            throw new IllegalStateException("Sending COMPLETED to tracker while this tracker already is completed.");
        sendRequest(announce, Event.COMPLETED);
        this.completed = true;
    }

    public void sendStopped(URL announce) throws IOException, BencodeException {
        sendRequest(announce, Event.STOPPED);
    }

    public void sendRequest(URL announce) throws IOException, BencodeException {
        if (!this.started)
            sendRequest(announce, Event.STARTED);
        else
            sendRequest(announce, Event.NONE);
    }

    private void sendRequest(URL announce, Event event) throws IOException, BencodeException {
        // TODO: set timeouts.
        HttpURLConnection conn = (HttpURLConnection) announce.openConnection();
        try {
            conn.setRequestMethod("GET");
            // TODO: infoHash
            conn.setRequestProperty("info_hash", "TEMPORARY VALUE");
            conn.setRequestProperty("peer_id", URLEncode(this.peerId));
            conn.setRequestProperty("port", URLEncode(String.valueOf(Torrent.PORT)));
            conn.setRequestProperty("uploaded", URLEncode(this.uploaded));
            conn.setRequestProperty("downloaded", URLEncode(this.downloaded));
            conn.setRequestProperty("left", URLEncode(this.left));
            conn.setRequestProperty("compact", URLEncode(Torrent.ALLOW_COMPACT));
            if (event != Event.NONE) conn.setRequestProperty("event", URLEncode(event.getValue()));

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Received a non HTTP_OK (200) response code: " +
                        "" + responseCode + " (" + conn.getResponseMessage() + ")");
            }

            byte[] content = conn.getInputStream().readAllBytes();
            updateFromResponse(content);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private String URLEncode(String s) throws UnsupportedEncodingException {
        return URLEncoder.encode(s, ENCODING);
    }

    private String URLEncode(byte[] b) throws UnsupportedEncodingException {
        return URLEncode(new String(b, ENCODING));
    }

    private String URLEncode(long l) throws UnsupportedEncodingException {
        return URLEncode(String.valueOf(l));
    }

    public void updateFromResponse(byte[] content)
    throws IOException, BencodeException {
        Bencode bencode = new Bencode(content);
        Map<BencodeString, BencodeResult> responseDictionary = bencode.getDictionary();

        // FAILURE REASON
        // If (failure reason is empty): the request went as expected, else: something failed.
        BencodeResult failureReasonResult = responseDictionary.get(toBenString("failure reason"));
        if (failureReasonResult == null || failureReasonResult.getType() != BencodeType.STRING) {
            throw new BencodeException("Incorrect \"failure reason\" field.");
        }
        String failureReason = fromBenString(failureReasonResult);
        if (!failureReason.isEmpty())
            throw new IOException("Received failure from tracker: " + failureReason);

        this.mutex.lock();
        try {

            // INTERVAL
            BencodeResult intervalResult = responseDictionary.get(toBenString("interval"));
            if (intervalResult == null || intervalResult.getType() != BencodeType.NUMBER) {
                throw new BencodeException("Incorrect \"interval\" field.");
            }
            long interval = (long) intervalResult.getValue();

            // TRACKER ID
            BencodeResult trackerIdResult = responseDictionary.get(toBenString("tracker id"));
            if (trackerIdResult == null || trackerIdResult.getType() != BencodeType.STRING) {
                throw new BencodeException("Incorrect \"tracker id\" field.");
            }
            String trackerId = fromBenString(trackerIdResult);

            // SEEDERS (complete)
            BencodeResult seedersResult = responseDictionary.get(toBenString("complete"));
            if (seedersResult == null || seedersResult.getType() != BencodeType.NUMBER) {
                throw new BencodeException("Incorrect \"complete\"(seeders) field.");
            }
            long seeders = (long) seedersResult.getValue();

            // LEECHERS (incomplete)
            BencodeResult leecehersResult = responseDictionary.get(toBenString("incomplete"));
            if (leecehersResult == null || leecehersResult.getType() != BencodeType.NUMBER) {
                throw new BencodeException("Incorrect \"incomplete\"(leechers) field.");
            }
            long leechers = (long) leecehersResult.getValue();

            // PEERS
            BencodeResult peersResult = responseDictionary.get(toBenString("peers"));
            if (peersResult == null) {
                throw new BencodeException("Incorrect \"peers\" field.");
            }

            // If (LIST): The peer list is a "dictionary model".
            // Else if (STRING): The peer list is a "binary model".
            // Else: Something wrong.
            var newPeers = new ArrayList<Peer>();
            if (peersResult.getType() == BencodeType.LIST) {
                /*
                    DICTIONARY MODEL
                 */
                @SuppressWarnings("unchecked")
                var peersList = (List<BencodeResult>) peersResult.getValue();
                for (BencodeResult peerResult : peersList) {

                    if (peerResult.getType() != BencodeType.DICTIONARY) {
                        throw new BencodeException("Incorrect \"peers\" field, expected dictionary model.");
                    }
                    @SuppressWarnings("unchecked")
                    var peerDictionary = (Map<BencodeString, BencodeResult>) peerResult.getValue();
                    // (PEER ID is ignored)
                    // IP
                    BencodeResult ipResult = peerDictionary.get(toBenString("ip"));
                    if (ipResult == null || ipResult.getType() != BencodeType.STRING) {
                        throw new BencodeException("Incorrect \"ip\" field of a peer.");
                    }
                    String ip = fromBenString(ipResult);

                    // PORT
                    BencodeResult portResult = peerDictionary.get(toBenString("port"));
                    if (portResult == null || portResult.getType() != BencodeType.NUMBER) {
                        throw new BencodeException("Incorrect \"port\" field of a peer.");
                    }
                    long port = (long) portResult.getValue();

                    // PEER
                    newPeers.add(new Peer(ip, (int) port));
                }
            } else if (peersResult.getType() == BencodeType.STRING) {
                /*
                    BINARY MODEL
                 */
                BencodeString peersString = (BencodeString) peersResult.getValue();
                if (peersString.getBytes().length % 6 != 0) {
                    throw new BencodeException("Binary model peers list not divisible by 6 (4 byte ip + 2 byte port).");
                }

                ByteBuffer buffer = ByteBuffer
                        .allocate(peersString.getBytes().length)
                        .put(peersString.getBytes());
                buffer.rewind();

                byte[] ipBuf = new byte[4];
                byte[] portBuf = new byte[2];
                while (buffer.hasRemaining()) {
                    buffer.get(ipBuf);
                    buffer.get(portBuf);

                    InetAddress ip = InetAddress.getByAddress(ipBuf);
                    int port = portBuf[0] << 8 | portBuf[1];

                    // PEER
                    newPeers.add(new Peer(ip, port));
                }
            } else {
                throw new BencodeException("Incorrect format of peers.");
            }

            this.trackerId = trackerId;
            this.interval = interval;
            this.seeders = seeders;
            this.leechers = leechers;

            // TODO: Will need some sort of mechanism to remove old peers that might have exited or
            //  just doesn't have the pieces that this client needs.
            // Add the newPeers to the old peers in this.peers.
            // Uses the IP address as key.
            for (Peer peer : newPeers)
                this.peers.putIfAbsent(peer.getIp().getHostAddress(), peer);

        } finally {
            this.mutex.unlock();
        }
    }

    public String getTrackerId() {
        return this.trackerId;
    }

    public byte[] getPeerId() {
        return this.peerId;
    }

    public long getUploaded() {
        return this.uploaded;
    }

    public long getDownloaded() {
        return this.downloaded;
    }

    public long getLeft() {
        return this.left;
    }

    public long getInterval() {
        return this.interval;
    }

    public long getSeeders() {
        return this.seeders;
    }

    public long getLeechers() {
        return this.leechers;
    }

    public Map<String, Peer> getPeers() {
        return this.peers;
    }
}
