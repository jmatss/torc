package com.github.jmatss.torc.bittorrent;

public enum Event {
    STARTED("started"), STOPPED("stopped"), COMPLETED("completed"), NONE("NO SPECIAL EVENT");
    private final String value;

    private Event(String value) {
        this.value = value;
    }

    public String getValue() {
        return this.value;
    }
}
