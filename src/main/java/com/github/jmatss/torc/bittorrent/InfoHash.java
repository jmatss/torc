package com.github.jmatss.torc.bittorrent;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

public class InfoHash {
    public static final String HASH_ALGORITHM = "SHA-1";
    private final byte[] infoHash;

    public InfoHash(InfoHash infoHash) {
        this.infoHash = infoHash.getBytes();
    }

    public InfoHash(byte[] infoDictionary) throws NoSuchAlgorithmException {
        var md = MessageDigest.getInstance(HASH_ALGORITHM);
        this.infoHash = md.digest(infoDictionary);
    }

    public InfoHash(byte[] infoHash, boolean rawInfoHash) {
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
            return Arrays.equals(this.getBytes(), ((InfoHash)other).getBytes());
        else
            return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(this.infoHash);
    }

    @Override
    public String toString() {
        var sb = new StringBuilder(this.infoHash.length * 2);
        for (byte b : this.infoHash) {
            sb.append(Character.forDigit((b >> 0xf) & 0xf, 16));
            sb.append(Character.forDigit(b & 0xf, 16));
        }
        return sb.toString();
    }
}
