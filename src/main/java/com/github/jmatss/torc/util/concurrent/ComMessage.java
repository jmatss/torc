package com.github.jmatss.torc.util.concurrent;

import com.github.jmatss.torc.bittorrent.InfoHash;

import java.util.*;

public class ComMessage {
    private final ComMessageType type;
    // Contains extra information/properties for this message.
    private final Map<String, Object> properties;

    // TODO: Add more properties (which will mean the constructor logic need to be changed).
    private ComMessage(ComMessageType type, InfoHash infoHash, String filename, Exception exception) {
        this.type = type;
        this.properties = new HashMap<>();
        if (infoHash != null) this.properties.put("infoHash", infoHash);
        if (filename != null) this.properties.put("filename", filename);
        if (exception != null) this.properties.put("exception", exception);
    }

    private ComMessage(ComMessageType type, InfoHash infoHash, String filename) {
        this(type, infoHash, filename, null);
    }

    private ComMessage(ComMessageType type, InfoHash infoHash, Exception exception) {
        this(type, infoHash, null, exception);
    }

    private ComMessage(ComMessageType type, InfoHash infoHash) {
        this(type, infoHash, null, null);
    }

    private ComMessage(ComMessageType type, String filename) {
        this(type, null, filename, null);
    }

    private ComMessage(ComMessageType type, Exception exception) {
        this(type, null, null, exception);
    }

    private ComMessage(ComMessageType type) {
        this(type, null, null, null);
    }

    public static ComMessage add(String filename) {
        return new ComMessage(ComMessageType.ADD, filename);
    }

    public static ComMessage remove(InfoHash infoHash) {
        return new ComMessage(ComMessageType.REMOVE, infoHash);
    }

    public static ComMessage start(InfoHash infoHash) {
        return new ComMessage(ComMessageType.START, infoHash);
    }

    public static ComMessage stop(InfoHash infoHash) {
        return new ComMessage(ComMessageType.STOP, infoHash);
    }

    // Sent from a parent to a child to indicate that the child should terminate.
    public static ComMessage shutdown() {
        return new ComMessage(ComMessageType.SHUTDOWN);
    }

    // Sent from a child to a parent to indicate that something have gone wrong.
    public static ComMessage error(Exception exception) {
        return new ComMessage(ComMessageType.ERROR, exception);
    }

    // Sent from a child to a parent to indicate that something have gone terrible wrong
    // and that the child will terminate itself after sending this message.
    public static ComMessage fatalError(InfoHash infoHash, Exception exception) {
        return new ComMessage(ComMessageType.FATAL_ERROR, infoHash, exception);
    }

    // Can be used to both move and rename the base path.
    // TODO: Make more "advanced" so that individual files can be renamed.
    //  Need to send more information for that to work.
    public static ComMessage rename(InfoHash infoHash, String filename) {
        return new ComMessage(ComMessageType.MOVE, infoHash, filename);
    }

    public ComMessageType getType() {
        return this.type;
    }

    public Map<String, Object> getProperties() {
        return this.properties;
    }
}
