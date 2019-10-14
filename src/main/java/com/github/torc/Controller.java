package com.github.torc;

import java.util.Random;

public class Controller {
  // TODO: remove this temp download root path
  private static final String DOWNLOAD_ROOT_PATH = "";

  private final String peerId;
  private final String rootPath;

  Controller() {
    this.peerId = newPeerid();
    this.rootPath = DOWNLOAD_ROOT_PATH;
  }

  public void start() {
    while (true) {}
  }

  public String getPeerId() {
    return this.peerId;
  }

  // Format of peerid: -<client id(2 bytes)><version(4 bytes)>-<12 random ascii numbers>
  // Using client id "UT" (µTorrent)
  private String newPeerid() {
    char[] peerId = new char[20];
    peerId[0] = '-';
    peerId[1] = 'U';
    peerId[2] = 'T';
    peerId[7] = '-';

    Random rand = new Random();
    for (int i = 3; i < peerId.length; i++) {
      if (i == 7) continue;
      peerId[i] = (char) ((int) '0' + rand.nextInt(10));
    }

    return new String(peerId);
  }
}
