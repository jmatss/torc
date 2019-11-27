package com.github.jmatss.torc;

import com.github.jmatss.torc.bittorrent.Torrent;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    // TODO: remove this temp download root path
    private static final String DOWNLOAD_ROOT_PATH = "";

    private final ExecutorService executor;
    private final byte[] peerId;
    private final String rootPath;

    Controller() {
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);
        this.peerId = newPeerId();
        this.rootPath = DOWNLOAD_ROOT_PATH;
    }

    public List<Runnable> shutdown() {
        // TODO: maybe return exception instead of empty list.
        if (this.executor.isShutdown())
            return Collections.emptyList();
        return this.executor.shutdownNow();
    }

    public void start() {
        while (true) {
        }
    }

    public byte[] getPeerId() {
        return this.peerId;
    }

    // Format of peer id: -<client id(2 bytes)><version(4 bytes)>-<12 random ascii numbers>
    // Using client id "UT" (µTorrent) version 3.5.0 for anonymity.
    private byte[] newPeerId() {
        byte[] client = {'-', 'U', 'T', '3', '5', '0', '0', '-'};
        ByteBuffer peerId = ByteBuffer.allocate(20).put(client);

        Random rand = new Random();
        for (int i = 0; i < peerId.capacity() - client.length; i++)
            peerId.put((byte) ('0' + rand.nextInt(10)));

        return peerId.array();
    }

    private List<Torrent> fetchTorrents() {
        return;
    }
}
