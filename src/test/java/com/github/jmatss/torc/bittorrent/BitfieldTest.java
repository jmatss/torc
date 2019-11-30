package com.github.jmatss.torc.bittorrent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BitfieldTest {
    private static final int amountOfPieces = 10;
    private static final int index = 3;
    private static Bitfield bitfield;

    @BeforeEach
    public static void setUp() {
        BitfieldTest.bitfield = new Bitfield(amountOfPieces);
    }

    @Test
    public void testBitfieldCreatedWithCorrectAmountOfPiecesAndEveryBitSetToZero() {
        assertEquals(amountOfPieces, bitfield.getAmountOfPieces());
        for (int i = 0; i < amountOfPieces; i++)
            assertFalse(bitfield.isSet(i));
    }

    @Test
    public void testAllBitfieldFunctionsThrowsIndexOutOfBounds() {
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.set(amountOfPieces));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.trySet(amountOfPieces));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.clear(amountOfPieces));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.isSet(amountOfPieces));

        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.set(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.trySet(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.clear(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> bitfield.isSet(-1));
    }

    @Test
    public void testAllBitsAreSettableAndClearable() {
        for (int i = 0; i < amountOfPieces; i++) {
            assertFalse(bitfield.isSet(i));
            bitfield.set(i);
            assertTrue(bitfield.isSet(i));
            bitfield.clear(i);
            assertFalse(bitfield.isSet(i));
        }
    }

    @Test
    public void testTrySet() {
        assertFalse(bitfield.isSet(index));
        assertTrue(bitfield.trySet(index));
        assertTrue(bitfield.isSet(index));
    }

    @Test
    public void testTrySetReturnsFalseWhenIndexAlreadyIsSet() {
        assertFalse(bitfield.isSet(index));
        assertTrue(bitfield.trySet(index));
        assertTrue(bitfield.isSet(index));
        assertFalse(bitfield.trySet(index));
    }
}
