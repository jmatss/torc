package com.github.jmatss.torc.bittorrent;

import java.util.Map;
import java.util.concurrent.locks.Lock;

public class Tracker {
    private final Lock mutex;

    private final byte[] infoHash;
    private long uploaded;
    private long downloaded;
    private long left;

    // Contains the bitfield of the pieces that this client have downloaded
    // and can be seeded to other clients.
    private byte[]  bitfieldHave;

    // Contains the bitfield of the pieces that this client have downloaded (= bitfieldHave)
    // and also the pieces that the peerHandlers are currently downloading.
    // This can be used to see which pieces that are free to start downloading.
    private byte[] bitfieldDownloading;

    private boolean started;
    private boolean completed;

    private long interval;
    private long seeders;
    private long leechers;
    private final Map<String, Peer> peers;

    public Tracker(byte[] content, Torrent torrent) {

    }

}
