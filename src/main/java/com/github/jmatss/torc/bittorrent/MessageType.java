package com.github.jmatss.torc.bittorrent;

import java.util.HashMap;
import java.util.Map;

public enum MessageType {
    KEEP_ALIVE(-1),
    CHOKE(0),
    UNCHOKE(1),
    INTERESTED(2),
    NOT_INTERESTED(3),
    HAVE(4),
    BIT_FIELD(5),
    REQUEST(6),
    PIECE(7),
    CANCEL(8);

    public static Map<Integer, MessageType> lookup;
    private final int i;

    private MessageType(int i) {
        this.i = i;
    }

    static {
        MessageType.lookup = new HashMap<>();
        for (var messageType : MessageType.values())
            MessageType.lookup.put(messageType.i, messageType);
    }

    public int getValue() {
        return this.i;
    }

    public static MessageType valueOf(int key) throws IncorrectMessageTypeException {
        MessageType messageType;
        if ((messageType = MessageType.lookup.get(key)) == null)
            throw new IncorrectMessageTypeException("Received incorrect MessageType: " + key);
        return messageType;
    }
}
