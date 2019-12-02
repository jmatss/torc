package com.github.jmatss.torc;

import com.github.jmatss.torc.bittorrent.InfoHash;
import com.github.jmatss.torc.bittorrent.Torrent;
import com.github.jmatss.torc.util.concurrent.ComChannel;
import com.github.jmatss.torc.util.concurrent.ComMessage;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Controller {
    // TODO: remove this temp download root path
    private static final String DOWNLOAD_ROOT_PATH = "";

    private final ExecutorService executor;

    private final String rootPath;
    private final byte[] peerId;

    // This ComChannel is used to send messages between this controller and the torrent handlers asynchronously.
    private final ComChannel torrentHandlersChannel;


    private final BlockingQueue<ComMessage> sendToView;

    // This channel will contain every message that is sent to this controller.
    // That includes message from both the View and the torrent handlers.
    private final BlockingQueue<ComMessage> receiver;

    // List of all "active" torrents in the client.
    private final Map<InfoHash, Torrent> torrents;

    Controller(BlockingQueue<ComMessage> sendToView, BlockingQueue<ComMessage> receiver) {
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);

        this.rootPath = DOWNLOAD_ROOT_PATH;
        this.peerId = newPeerId();

        this.torrentHandlersChannel = new ComChannel();
        this.sendToView = sendToView;
        this.receiver = receiver;

        this.torrents = getTorrentsFromDisk();
        this.executor.submit(this::run);
    }

    public List<Runnable> shutdown() {
        // TODO: maybe return exception instead of empty list.
        if (this.executor.isShutdown())
            return Collections.emptyList();
        return this.executor.shutdownNow();
    }

    public void run() {
        while (true) {

        }
    }

    public void addTorrent(String filename) {

    }

    public void removeTorrent(byte[] infoHash) {

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
        return Collections.emptyList();
    }

    // TODO: implement logic for fetching torrent from previous session.
    //  Currently only returns an empty HashMap.
    private Map<InfoHash, Torrent> getTorrentsFromDisk() {
        return new HashMap<>();
    }
}
