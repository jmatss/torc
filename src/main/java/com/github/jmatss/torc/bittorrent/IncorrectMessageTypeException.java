package com.github.jmatss.torc.bittorrent;

@SuppressWarnings("serial")
public class IncorrectMessageTypeException extends Exception {
    public IncorrectMessageTypeException(String msg) {
        super(msg);
    }
}
