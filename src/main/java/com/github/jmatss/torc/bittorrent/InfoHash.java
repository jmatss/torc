package com.github.jmatss.torc.bittorrent;

import java.util.Arrays;

public class InfoHash {
    private final byte[] infoHash;

    public InfoHash(byte[] infoHash) {
        this.infoHash = infoHash;
    }

    public byte[] getBytes() {
        return this.infoHash;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this)
            return true;
        else if (other instanceof InfoHash)
            return Arrays.equals(this.getBytes(), ((InfoHash) other).getBytes());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.infoHash);
    }
}
