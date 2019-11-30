package com.github.jmatss.torc.bittorrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Bitfield {
    private final Lock mutex;
    private final byte[] bitfield;

    public Bitfield(int amountOfPieces) {
        this.mutex = new ReentrantLock();
        this.bitfield = new byte[((amountOfPieces - 1) / 8) + 1];
    }

    public Bitfield(byte[] bitfield) {
        this.mutex = new ReentrantLock();
        this.bitfield = bitfield;
    }

    // Sets the piece with "index" to 1.
    public synchronized void set(int index) {
        this.bitfield[byteIndex(index)] |= (1 << shiftAmount(index));
    }

    // Tries to set "index" to 1. If "index" currently is 0, set it to 1 and return "true".
    // Else, return false.
    public synchronized boolean trySet(int index) {
        if (!isSet(index)) {
            set(index);
            return true;
        } else {
            return false;
        }
    }

    // Sets the piece with "index" to 0.
    public synchronized void clear(int index) {
        this.bitfield[byteIndex(index)] &= ~(1 << shiftAmount(index));
    }

    // Returns "true" if piece "index" contains a 1. Returns "false" if it contains a 0.
    public synchronized boolean isSet(int index) {
        return (this.bitfield[byteIndex(index)] & (1 << shiftAmount(index))) > 0;
    }

    // byteIndex is the index of the byte inside the "bitfield" array that contains the "real" index.
    private int byteIndex(int index) {
        return index / 8;
    }

    // shiftAmount represents how much the bit needs to be shifted left to get to the correct "bit index".
    private int shiftAmount(int index) {
        return 7 - (index % 8);
    }
}
