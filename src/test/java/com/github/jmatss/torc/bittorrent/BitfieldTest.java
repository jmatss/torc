package com.github.jmatss.torc.bittorrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitfieldTest {
    private static final int AMOUNT_OF_PIECES = 10;
    private Bitfield bitfield;

    @BeforeEach
    public void setUp() {
        this.bitfield = new Bitfield(AMOUNT_OF_PIECES);
    }

    @Test
    public void testBitfieldCreatedWithCorrectAmountOfPiecesAndEveryBitSetToZero() {
        assertEquals(AMOUNT_OF_PIECES, bitfield.getAmountOfPieces());
        for (int i = 0; i < AMOUNT_OF_PIECES; i++)
            assertFalse(bitfield.isSet(i));
    }

    @Test
    public void testAllBitfieldFunctionsThrowsIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.set(AMOUNT_OF_PIECES));        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.unSet(AMOUNT_OF_PIECES));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.unSet(AMOUNT_OF_PIECES));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.isSet(AMOUNT_OF_PIECES));

        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.set(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.unSet(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.isSet(-1));
    }

    @Test
    public void testAllBitsAreSettableAndClearable() {
        for (int i = 0; i < AMOUNT_OF_PIECES; i++) {
            assertFalse(bitfield.isSet(i));
            bitfield.set(i);
            assertTrue(bitfield.isSet(i));
            bitfield.unSet(i);
            assertFalse(bitfield.isSet(i));
        }
    }

    @Test
    public void testSetReturnsTrueWhenIndexIsSetCurrectly() {
        int index = 8;
        assertTrue(bitfield.set(index));
    }

    @Test
    public void testSetReturnsFalseWhenIndexAlreadyIsSet() {
        int index = 8;
        bitfield.set(index);
        assertFalse(bitfield.set(index));
    }

    @Test
    public void testUnSetReturnsTrueWhenIndexIsUnSetCorrectly() {
        int index = 8;
        bitfield.set(index);
        assertTrue(bitfield.unSet(index));
    }

    @Test
    public void testUnSetReturnsFalseWhenIndexIsAlreadyUnSet() {
        int index = 8;
        assertFalse(bitfield.unSet(index));
    }
}
