package com.github.jmatss.torc;

import com.github.jmatss.torc.bittorrent.InfoHash;
import com.github.jmatss.torc.bittorrent.Torrent;
import com.github.jmatss.torc.util.com.ComChannel;
import com.github.jmatss.torc.util.com.ComMessage;
import com.github.jmatss.torc.util.com.ComPropertyType;

import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Controller {
    public static final Logger LOGGER = Logger.getLogger(Controller.class.getName());
    // TODO: remove this temp download root path
    private static final String DOWNLOAD_ROOT_PATH = "";

    private final ExecutorService executor;

    private final String rootPath;
    private final byte[] peerId;

    // This ComChannel is used to send messages between this controller and the torrent handlers asynchronously.
    // It will also receive messages from the View.
    private final ComChannel comChannel;

    private final BlockingQueue<ComMessage> sendToView;

    // List of all "active" torrents in the client.
    private final Map<InfoHash, Torrent> torrents;

    Controller(BlockingQueue<ComMessage> sendToView, BlockingQueue<ComMessage> receiver) {
        int processors = Runtime.getRuntime().availableProcessors();
        this.executor = Executors.newFixedThreadPool(processors);

        this.rootPath = DOWNLOAD_ROOT_PATH;
        this.peerId = newPeerId();

        this.comChannel = new ComChannel(receiver);
        this.sendToView = sendToView;

        this.torrents = getTorrentsFromDisk();
        this.executor.submit(this::run);
    }

    public List<Runnable> shutdown() {
        this.comChannel.sendChildren(ComMessage.shutdown());
        // TODO: maybe return exception instead of empty list.
        if (this.executor.isShutdown())
            return Collections.emptyList();
        return this.executor.shutdownNow();
    }

    public void run() {
        ComMessage message;
        InfoHash infoHash;
        String filename;
        while (true) {
            try {
                message = this.comChannel.recvParent();
                switch (message.getType()) {
                    case ADD:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        filename = (String) message.getProperty(ComPropertyType.FILENAME.toString());
                        var torrent = new Torrent(filename, getPeerId());
                        this.torrents.put(infoHash, torrent);
                        this.executor.submit(() -> TorrentHandler.run(torrent));
                        break;
                    case REMOVE:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        this.comChannel.sendChild(ComMessage.remove(infoHash));
                        break;
                    case START:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        this.comChannel.sendChild(ComMessage.start(infoHash));
                        break;
                    case STOP:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        this.comChannel.sendChild(ComMessage.stop(infoHash));
                        break;
                    case SHUTDOWN:
                        this.comChannel.sendChildren(ComMessage.shutdown());
                        return;
                    case ERROR:
                        this.sendToView.add(message);
                        break;
                    case FATAL_ERROR:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        this.torrents.remove(infoHash);
                        this.sendToView.add(message);
                        break;
                    case MOVE:
                        infoHash = (InfoHash) message.getProperty(ComPropertyType.INFO_HASH.toString());
                        filename = (String) message.getProperty(ComPropertyType.FILENAME.toString());
                        this.comChannel.sendChild(ComMessage.rename(infoHash, filename));
                        break;
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }
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
