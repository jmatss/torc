package com.github.jmatss.torc;

import com.github.jmatss.torc.bittorrent.Torrent;

import java.util.logging.Level;
import java.util.logging.Logger;

public class TorrentHandler {
    public static final Logger LOGGER = Logger.getLogger(TorrentHandler.class.getName());

    public static void run(Torrent torrent) {
        try {
            torrent.sendTrackerRequest();
            torrent.getTracker().getPeers();
            getNext();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage());
        }
    }

    private static void getNext() {

    }
}
