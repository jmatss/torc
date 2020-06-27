package com.github.jmatss.torc.bittorrent;

public class Bitfield {
    private final byte[] bitfield;
    private final int amountOfPieces;

    public Bitfield(int amountOfPieces) {
        this.bitfield = new byte[((amountOfPieces - 1) / 8) + 1];
        this.amountOfPieces = amountOfPieces;
    }

    private void _set(int index) {
        int byteIndex = this.byteIndex(index);
        int shiftAmount = this.shiftAmount(index);
        this.bitfield[byteIndex] |= (1 << shiftAmount);
    }

    private void _unSet(int index) {
        int byteIndex = this.byteIndex(index);
        int shiftAmount = this.shiftAmount(index);
        this.bitfield[byteIndex] &= ~(1 << shiftAmount);
    }

    /**
     * Sets the piece with index `index` to 1. Returns true if the value
     * was set to 1 or false if the value already is set to 1.
     * 
     * @param index the index of the piece to set.
     */
    public synchronized boolean set(int index) {
        this.outOfBoundsGuard(index);
        if (!this.isSet(index)) {
            this._set(index);
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets the piece with index `index` to 0. Returns true if the value
     * was set to 0 or false if the value already is set to 0.
     * 
     * @param index the index of the piece to set.
     */
    public synchronized boolean unSet(int index) {
        this.outOfBoundsGuard(index);
        if (this.isSet(index)) {
            this._unSet(index);
            return true;
        } else {
            return false;
        }
    }

    // Returns "true" if piece "index" contains a 1. Returns "false" if it contains a 0.
    public synchronized boolean isSet(int index) {
        this.outOfBoundsGuard(index);
        int byteIndex = this.byteIndex(index);
        int shiftAmount = this.shiftAmount(index);
        return (this.bitfield[byteIndex] & (1 << shiftAmount)) > 0;
    }

    public int getAmountOfPieces() {
        return this.amountOfPieces;
    }

    /**
     * This function returns the byte index i.e. the position in the
     * `bitField` array that corresponds to the given `index`.
     * 
     * @param index
     * @return
     */
    private int byteIndex(int index) {
        return index / 8;
    }

    /**
     * This function returns the amount that a bit needs to be shifted to line
     * up inside a `bitField` byte corresponds to the given `index`.
     * 
     * @param index
     * @return
     */
    private int shiftAmount(int index) {
        return 7 - (index % 8);
    }

    private void outOfBoundsGuard(int index) {
        if (index >= this.amountOfPieces)
            throw new IndexOutOfBoundsException("index >= amountOfPieces (" + index + " >= " + amountOfPieces + ")");
        else if (index < 0)
            throw new IndexOutOfBoundsException("index < 0 (" + index + " < 0)");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.amountOfPieces; i++) {
            sb.append(this.isSet(i) ? "1" : "0");
            if (i > 0 && i % 8 == 0) {
                sb.append("_");
            }
        }
        return sb.toString();
    }
}
