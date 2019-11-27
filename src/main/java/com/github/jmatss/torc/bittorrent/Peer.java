package com.github.jmatss.torc.bittorrent;

import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.locks.Lock;

public class Peer {
    private final Lock mutex;

    private final boolean usingIp;
    private final InetAddress ip;
    private final String hostname;
    private final int port;
    private final String hostAndPort;

    private Socket connection;
    private byte[] remoteBitfield;

    private boolean amChoking;
    private boolean amInterested;
    private boolean peerChoking;
    private boolean peerInterested;


}
