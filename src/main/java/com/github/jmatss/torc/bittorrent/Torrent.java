package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.*;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.jmatss.torc.TMP_CONST.SHA1_HASH_LENGTH;


/**
 * Represents a torrent. Created either with .torrent-file or a magnet link.
 */
public class Torrent {
    public static final int PORT = 6881;    // TODO: Make list of ports 6881-6889 instead(?)
    public static final int MAX_REQUEST_LENGTH = 1 << 14;   // 2^14 most common.
    public static final int ALLOW_COMPACT = 1;  // 0 == disallow (se README)

    public static final long CONNECT_TIMEOUT = 5000;
    public static final long READ_TIMEOUT = 5000;

    // Mutex used when changing filename or moving the file.
    private final Lock mutex;

    private final byte[] peerId;

    // Contains data related to the active tracker.
    private Tracker tracker;

    // Contains the URL of the tracker.
    private final URL announce;

    private final InfoHash infoHash;

    // Contains the bitfield of the pieces that this client have downloaded
    // and can be seeded to other clients.
    private Bitfield bitfieldHave;

    // Contains the bitfield of the pieces that this client have downloaded (= bitfieldHave)
    // and also the pieces that the this client are currently downloading.
    // This can be used to see which pieces that are free to start downloading.
    private Bitfield bitfieldDownloading;

    // Contains the "root" directory if this torrent is a "multi-file torrent".
    // Contains the path (== files[0].path) if this torrent is "single-file torrent".
    private final Path name;

    // Contains info regarding the files of the torrent.
    private final List<TorrentFile> files;

    // SHA1 hashes of all pieces concatenated (pieces.length % 20 == 0).
    private final byte[][] pieces;

    // Length of a single piece in bytes.
    // All pieces have the same length expected the last one that will be less that pieceLength.
    private final long pieceLength;

    // Indicate of downloading/uploading of this torrent is paused.
    private boolean paused;

    public Torrent(InputStream inputStream, byte[] peerId)
    throws BencodeException, IOException, NoSuchAlgorithmException {
        if (inputStream == null)
            throw new IllegalArgumentException("InputStream is null.");

        var torrentDictionary = Bencode.decodeDictionary(inputStream);

        this.mutex = new ReentrantLock();
        this.peerId = peerId;
        this.tracker = null;
        this.paused = false;

        // ANNOUNCE
        var announce = torrentDictionary.get(BencodeUtil.toBenString("announce"));
        if (announce == null)
            throw new BencodeException("\"announce\" field is null.");
        this.announce = new URL(announce.getString());

        // INFO
        var infoResult = torrentDictionary.get(BencodeUtil.toBenString("info"));
        if (infoResult == null)
            throw new BencodeException("\"info\" field is null.");
        var info = infoResult.getDictionary();

        // INFO_HASH
        var content = Bencode.encode(infoResult);
        this.infoHash = new InfoHash(content);

        // NAME
        var name = info.get(BencodeUtil.toBenString("name"));
        if (name == null)
            throw new BencodeException("\"name\" field is null.");
        this.name = Paths.get(name.getString());

        // PIECE LENGTH
        var pieceLength = info.get(BencodeUtil.toBenString("piece length"));
        if (pieceLength == null)
            throw new BencodeException("\"piece length\" field is null.");
        this.pieceLength = pieceLength.getNumber();

        // PIECES
        var pieces = info.get(BencodeUtil.toBenString("pieces"));
        if (pieces == null)
            throw new BencodeException("\"pieces\" field is null.");
        var piecesBytes = pieces.getBytes();
        if (piecesBytes.length % SHA1_HASH_LENGTH != 0)
            throw new BencodeException("Field \"pieces\" isn't divisible by sha1 length.");

        ByteBuffer buffer = ByteBuffer.wrap(piecesBytes);
        this.pieces = new byte[piecesBytes.length / SHA1_HASH_LENGTH][];
        for (int i = 0; i < this.pieces.length; i++) {
            byte[] currentDigest = new byte[SHA1_HASH_LENGTH];
            buffer.get(currentDigest);
            this.pieces[i] = currentDigest;
        }

        // BITFIELDS
        this.bitfieldHave = new Bitfield(this.pieces.length);
        this.bitfieldDownloading = new Bitfield(this.pieces.length);

        // If true: this is a single-file torrent.
        // Else: this is a multi-file torrent.
        if (!info.containsKey(BencodeUtil.toBenString("files"))) {
            // LENGTH
            var length = info.get(BencodeUtil.toBenString("length"));
            if (length == null)
                throw new BencodeException("\"length\" field is null.");
            this.files = List.of(new TorrentFile(0, length.getNumber(), this.name));
        } else {
            // FILES
            var filesResult = info.get(BencodeUtil.toBenString("files"));
            if (filesResult == null)
                throw new BencodeException("\"files\" field is null.");
            var files = filesResult.getList();

            this.files = new ArrayList<>(files.size());

            int index = 0;
            for (BencodeData<Object> fileResult : files) {
                // FILE
                if (fileResult == null)
                    throw new BencodeException("File " + index + " inside \"files\" field is null.");
                var file = fileResult.getDictionary();

                // LENGTH
                var lengthResult = file.get(BencodeUtil.toBenString("length"));
                if (lengthResult == null)
                    throw new BencodeException("null length in file " + index + " inside \"files\" field.");
                long length = lengthResult.getNumber();

                // PATH
                var pathResult = file.get(BencodeUtil.toBenString("path"));
                if (pathResult == null)
                    throw new BencodeException("Incorrect path of file " + index + " inside \"files\" field.");
                var path = pathResult.getList();
                List<String> pathStrings = new ArrayList<>(path.size());

                for (BencodeData<Object> pathPiece : path) {
                    if (pathPiece == null)
                        throw new BencodeException("null path of file " + index + " inside \"files\" field.");
                    pathStrings.add(BencodeUtil.fromBenString(pathPiece));
                }

                this.files.add(new TorrentFile(index, length, String.join(File.separator, pathStrings)));
                index++;
            }
        }
    }

    public Torrent(String filename, byte[] peerId) throws IOException, BencodeException, NoSuchAlgorithmException {
        this(new FileInputStream(filename), peerId);
    }

    public Torrent lock() {
        this.mutex.lock();
        return this;
    }

    public void unlock() {
        this.mutex.unlock();
    }

    public Tracker getTracker() {
        return this.tracker;
    }

    public void sendTrackerRequest() throws IOException, BencodeException {
        if (this.tracker == null)
            this.tracker = new Tracker(this.files, this.infoHash, this.peerId);
        this.tracker.sendRequest(this.announce);
    }

    public void sendTrackerCompleted() throws IOException, BencodeException {
        if (this.tracker == null)
            throw new IllegalStateException("Trying to send Completed to a tracker that is null.");
        this.tracker.sendCompleted(this.announce);
    }

    public void sendTrackerStopped() throws IOException, BencodeException {
        if (this.tracker == null)
            throw new IllegalStateException("Trying to send Stopped to a tracker that is null.");
        this.tracker.sendStopped(this.announce);
    }

    public URL getAnnounce() {
        return this.announce;
    }

    public InfoHash getInfoHash() {
        return this.infoHash;
    }

    public Bitfield getBitfieldHave() {
        return this.bitfieldHave;
    }

    public Bitfield getBitfieldDownloading() {
        return this.bitfieldDownloading;
    }

    public Path getName() {
        return this.name;
    }

    public List<TorrentFile> getFiles() {
        return this.files;
    }

    public byte[][] getPieces() {
        return this.pieces;
    }

    public long getPieceLength() {
        return this.pieceLength;
    }

    public boolean isPaused() {
        return this.paused;
    }

    public Torrent setPaused(boolean value) {
        this.paused = value;
        return this;
    }
}
