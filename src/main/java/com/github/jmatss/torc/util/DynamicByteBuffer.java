package com.github.jmatss.torc.util;

import java.nio.*;

/**
 * A wrapper around a ByteBuffer with dynamic size.
 */
public class DynamicByteBuffer {
    public static final int DEFAULT_CAPACITY = 10;
    private ByteBuffer buffer;

    public DynamicByteBuffer() {
        this.buffer = ByteBuffer.allocate(DEFAULT_CAPACITY);
    }

    public DynamicByteBuffer put(byte b) {
        if (!this.buffer.hasRemaining()) {
            var oldBuffer = this.buffer;
            this.buffer = ByteBuffer.allocate(this.buffer.capacity() + (this.buffer.capacity() / 2));
            this.buffer.put(oldBuffer.array(), 0, oldBuffer.position());
        }
        this.buffer.put(b);
        return this;
    }

    // TODO: more effective puts
    public DynamicByteBuffer put(byte[] bytes) {
        for (byte b : bytes) {
            put(b);
        }
        return this;
    }

    public DynamicByteBuffer putLongBytes(long number) {
        put(String.valueOf(number).getBytes());
        return this;
    }

    public DynamicByteBuffer putIntBytes(int number) {
        return putLongBytes(number);
    }

    // Return the buffer with the excess byte removed.
    public byte[] getBytes() {
        int newCapacity = this.buffer.position();
        var newBuffer = ByteBuffer.allocate(newCapacity)
                .put(this.buffer.array(), 0, newCapacity);
        return newBuffer.array();
    }
}
