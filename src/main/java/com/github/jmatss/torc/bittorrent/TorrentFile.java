package com.github.jmatss.torc.bittorrent;

import java.nio.file.Path;
import java.nio.file.Paths;

public class TorrentFile {
    private final long index;
    private final long length;
    private final Path path;    // TODO: List of strings instead(?)

    TorrentFile(long index, long length, Path path) {
        this.index = index;
        this.length = length;
        this.path = path;
    }

    TorrentFile(long index, long length, String path) {
        this.index = index;
        this.length = length;
        this.path = Paths.get(path);
    }

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
