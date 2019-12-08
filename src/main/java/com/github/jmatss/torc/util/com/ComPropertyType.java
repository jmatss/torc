package com.github.jmatss.torc.util.com;

public enum ComPropertyType {
    INFO_HASH("infoHash"), FILENAME("filename"), EXCEPTION("exception");
    private final String s;

    private ComPropertyType(String s) {
        this.s = s;
    }

    @Override
    public String toString() {
        return this.s;
    }
}
