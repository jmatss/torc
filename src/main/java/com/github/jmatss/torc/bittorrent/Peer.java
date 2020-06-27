package com.github.jmatss.torc.bittorrent;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class Peer {
    /*
    private final Lock mutex;
*/
    private final InetAddress ip;
    private final int port; /*
    private final String hostAndPort;

    private Socket connection;
    private byte[] remoteBitfield;

    private boolean amChoking;
    private boolean amInterested;
    private boolean peerChoking;
    private boolean peerInterested;

     */

    Peer(InetAddress host, int port) throws UnknownHostException {
        if (port >= (1 << 16) || port <= 0)
            throw new IllegalArgumentException("Received a invalid port number: " + port);

        this.ip = host;
        this.port = port;   /*

        this.amChoking = true;
        this.amInterested = false;
        this.peerChoking = true;
        this.peerInterested = false;
         */
    }

    Peer(String host, int port) throws UnknownHostException {
        this(InetAddress.getByName(host), port);
    }

    public InetAddress getIp() {
        return this.ip;
    }

    public int getPort() {
        return this.port;
    }
}
