package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.*;

import java.io.IOException;
import java.io.InputStream;
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

public class Tracker {
    public static long DEFAULT_INTERVAL = 10; // seconds

    private final Lock mutex;

    private final InfoHash infoHash;
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

    public Tracker(List<TorrentFile> files, InfoHash infoHash, byte[] peerId) {
        this.mutex = new ReentrantLock();

        this.infoHash = infoHash;
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
            conn.setRequestProperty("info_hash", URLEncode(this.infoHash.getBytes()));
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

            this.updateFromResponse(conn.getInputStream());

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

    public void updateFromResponse(InputStream inputStream)
    throws IOException, BencodeException {
        var responseDictionary = Bencode.decodeDictionary(inputStream);

        // FAILURE REASON
        // If (failure reason is empty): the request went as expected, else: something failed.
        var failureReasonResult = responseDictionary.get(BencodeUtil.toBenString("failure reason"));
        if (failureReasonResult == null)
            throw new BencodeException("\"failure reason\" field is null.");
        String failureReason = BencodeUtil.fromBenString(failureReasonResult);
        if (!failureReason.isEmpty())
            throw new IOException("Received failure from tracker: " + failureReason);

        this.mutex.lock();
        try {

            // INTERVAL
            var intervalResult = responseDictionary.get(BencodeUtil.toBenString("interval"));
            if (intervalResult == null)
                throw new BencodeException("\"interval\" field is null.");
            long interval = intervalResult.getNumber();

            // TRACKER ID
            var trackerIdResult = responseDictionary.get(BencodeUtil.toBenString("tracker id"));
            if (trackerIdResult == null)
                throw new BencodeException("\"tracker id\" field is null.");
            String trackerId = trackerIdResult.getString();

            // SEEDERS (complete)
            var seedersResult = responseDictionary.get(BencodeUtil.toBenString("complete"));
            if (seedersResult == null)
                throw new BencodeException("\"complete\"(seeders) field is null.");
            long seeders = seedersResult.getNumber();

            // LEECHERS (incomplete)
            var leecehersResult = responseDictionary.get(BencodeUtil.toBenString("incomplete"));
            if (leecehersResult == null)
                throw new BencodeException("\"incomplete\"(leechers) field is null.");
            long leechers = leecehersResult.getNumber();

            // PEERS
            var peersResult = responseDictionary.get(BencodeUtil.toBenString("peers"));
            if (peersResult == null)
                throw new BencodeException("\"peers\" field is null.");

            // If (LIST): The peer list is a "dictionary model".
            // Else if (STRING): The peer list is a "binary model".
            // Else: Something wrong.
            var newPeers = new ArrayList<Peer>();
            if (peersResult.getType() == BencodeType.LIST) {
                /*
                    DICTIONARY MODEL
                 */
                var peersList = peersResult.getList();
                for (BencodeData<Object> peerResult : peersList) {

                    if (peerResult.getType() != BencodeType.DICTIONARY)
                        throw new BencodeException("Incorrect \"peers\" field, expected dictionary model.");
                    var peerDictionary = peerResult.getDictionary();
                    // (PEER ID is ignored)

                    // IP
                    var ipResult = peerDictionary.get(BencodeUtil.toBenString("ip"));
                    if (ipResult == null) {
                        throw new BencodeException("\"ip\" field of a peer is null.");
                    }
                    String ip = ipResult.getString();

                    // PORT
                    var portResult = peerDictionary.get(BencodeUtil.toBenString("port"));
                    if (portResult == null) {
                        throw new BencodeException("\"port\" field of a peer is null.");
                    }
                    long port = portResult.getNumber();

                    // PEER
                    newPeers.add(new Peer(ip, (int)port));
                }
            } else if (peersResult.getType() == BencodeType.STRING) {
                /*
                    BINARY MODEL
                 */
                var peersBytes = peersResult.getBytes();
                if (peersBytes.length % 6 != 0) {
                    throw new BencodeException("Binary model peers list not divisible by 6 (4 byte ip + 2 byte port).");
                }

                ByteBuffer buffer = ByteBuffer.wrap(peersBytes);

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
                throw new BencodeException("Incorrect format of peers. Peers where neither List or String.");
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
