package com.github.jmatss.torc.handler;

import com.github.jmatss.torc.bittorrent.InfoHash;
import com.github.jmatss.torc.bittorrent.Torrent;
import com.github.jmatss.torc.util.com.ComMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.jmatss.torc.TMP_CONST.QUEUE_SIZE;

public class TorrentHandler {
    public static final Logger LOGGER = Logger.getLogger(TorrentHandler.class.getName());

    private final Torrent torrent;
    private final Map<InfoHash, PeerHandler> peers;
    private final BlockingQueue<ComMessage> messageBuffer;

    TorrentHandler(Torrent torrent, BlockingQueue<ComMessage> a) {
        this.torrent = torrent;
        this.peers = new HashMap<>();
        this.messageBuffer = new ArrayBlockingQueue<>(QUEUE_SIZE);
    }

    public void run() {
        try {
            this.torrent.sendTrackerRequest();
            this.torrent.getTracker().getPeers();

            while (true) {
                ComMessage message = this.messageBuffer.remove();
                switch (message.getType()) {
                    case REMOVE:
                        // TODO: Remove torrent from disk(meta-data, not file) before exiting.
                        return;
                    case START:
                        if (!this.torrent.isPaused()) {
                            // TODO: tell controller that this torrent isn't paused, so nothing to do.
                        } else {
                            this.torrent.setPaused(false);
                        }
                        break;
                    case STOP:
                        // The pause boolean probably doesn't need to be atomic(?)
                        if (this.torrent.isPaused()) {
                            // TODO: tell controller that this torrent is already paused.
                        } else {
                            this.torrent.setPaused(true);
                        }
                        break;
                    case SHUTDOWN:
                        // TODO: tell controller that this handler is shutting down.
                        return;
                    case MOVE:
                        // TODO: lock something and wait until the pieces that are being downloaded/uploaded
                        //  finishes before moving/renaming the file. When finished, unlock and resume as before.
                        //  Might be able to use the "paused"/"stopped" boolean to do this.
                        break;
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private void sendMessage(ComMessage message) {
        this.messageBuffer.add(message);
    }

    private void shutdown() {

    }
}
