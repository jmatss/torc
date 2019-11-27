package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.BencodeException;
import com.github.jmatss.torc.bencode.BencodeType;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.locks.Lock;

import static com.github.jmatss.torc.bencode.Bencode.getNextType;

/**
 * Represents a torrent. Created either with .torrent-file or a magnet link.
 */
public class Torrent {
    private final String announce;
    private final Tracker tracker;
    private final Lock mutex;
    private final Path name;
    private final List<FILE_TEMP> files;
    private final byte[][] pieces;
    private final long pieceLength;

    private class FILE_TEMP {
        private final long index;
        private final long length;
        private final Path path;    // TODO: List of strings instead(?)

        public long getIndex() {
            return this.index;
        }

        public long getLength() {
            return this.length;
        }

        public Path getPath() {
            return this.path;
        }
    }

    public Torrent(String filename) throws IOException {

    }
}
