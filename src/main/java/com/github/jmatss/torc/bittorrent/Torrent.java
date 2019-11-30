package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.*;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.jmatss.torc.TMP_CONST.ENCODING;
import static com.github.jmatss.torc.TMP_CONST.SHA1_HASH_LENGTH;


/**
 * Represents a torrent. Created either with .torrent-file or a magnet link.
 */
public class Torrent {
    public static final int PORT = 6881;    // TODO: Make list of ports 6881-6889 instead(?)
    public static final int MAX_REQUEST_LENGTH = 1 << 14;   // 2^14 most common.

    // Mutex used when changing filename or moving the file.
    private final Lock mutex;

    // Contains data related to the active tracker.
    private Tracker tracker;

    // Contains the URL of the tracker.
    private final String announce;

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

    public Torrent(File f) throws BencodeException, IOException {
        if (!f.exists())
            throw new IOException("File \"" + f.getAbsolutePath() + "\" doesn't exist.");
        else if (!f.isFile())
            throw new IOException("File \"" + f.getAbsolutePath() + "\" isn't a valid file.");

        Bencode bencode = new Bencode(f);
        Map<BencodeString, BencodeResult> torrentDictionary = bencode.getDictionary();

        this.mutex = new ReentrantLock();

        // ANNOUNCE
        BencodeResult announce = torrentDictionary.get(toBenString("announce"));
        if (announce == null || announce.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"announce\" field.");
        this.announce = fromBenString(announce);

        // INFO
        BencodeResult infoResult = torrentDictionary.get(toBenString("info"));
        if (infoResult == null || infoResult.getType() != BencodeType.DICTIONARY)
            throw new BencodeException("Incorrect \"info\" field.");
        @SuppressWarnings("unchecked")
        var info = (Map<BencodeString, BencodeResult>) infoResult.getValue();

        // NAME
        BencodeResult name = info.get(toBenString("name"));
        if (name == null || name.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"name\" field.");
        this.name = Paths.get(fromBenString(name));

        // PIECE LENGTH
        BencodeResult pieceLength = info.get(toBenString("piece length"));
        if (pieceLength == null || pieceLength.getType() != BencodeType.NUMBER)
            throw new BencodeException("Incorrect \"piece length\" field.");
        this.pieceLength = (long) pieceLength.getValue();

        // PIECES
        BencodeResult pieces = info.get(toBenString("pieces"));
        if (pieces == null || pieces.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"pieces\" field.");
        BencodeString piecesBenString = (BencodeString) pieces.getValue();
        if (piecesBenString.getBytes().length % SHA1_HASH_LENGTH != 0)
            throw new BencodeException("Field \"pieces\" isn't divisible by sha1 length.");

        ByteBuffer buffer = ByteBuffer
                .allocate(piecesBenString.getBytes().length)
                .put(piecesBenString.getBytes());
        buffer.rewind();
        this.pieces = new byte[piecesBenString.getBytes().length / SHA1_HASH_LENGTH][];
        for (int i = 0; i < this.pieces.length; i++) {
            byte[] currentDigest = new byte[SHA1_HASH_LENGTH];
            buffer.get(currentDigest);
            this.pieces[i] = currentDigest;
        }

        // If true: this is a single-file torrent.
        // Else: this is a multi-file torrent.
        if (!info.containsKey(toBenString("files"))) {
            // LENGTH
            BencodeResult length = info.get(toBenString("length"));
            if (length == null || length.getType() != BencodeType.NUMBER)
                throw new BencodeException("Incorrect \"length\" field.");
            this.files = List.of(new TorrentFile(0, (long) length.getValue(), this.name));
        } else {
            // FILES
            BencodeResult filesResult = info.get(toBenString("files"));
            if (filesResult == null || filesResult.getType() != BencodeType.LIST)
                throw new BencodeException("Incorrect \"files\" field.");
            @SuppressWarnings("unchecked")
            List<BencodeResult> files = (List<BencodeResult>) filesResult.getValue();

            this.files = new ArrayList<>(files.size());

            int i = 0;
            for (BencodeResult fileResult : files) {
                // FILE
                if (fileResult == null || fileResult.getType() != BencodeType.DICTIONARY)
                    throw new BencodeException("Incorrect file inside \"files\" field.");
                @SuppressWarnings("unchecked")
                Map<BencodeString, BencodeResult> file = (Map<BencodeString, BencodeResult>) fileResult.getValue();

                // LENGTH
                BencodeResult lengthResult = file.get(toBenString("length"));
                if (lengthResult == null || lengthResult.getType() != BencodeType.NUMBER)
                    throw new BencodeException("Incorrect length of file inside \"files\" field.");
                long length = (long) lengthResult.getValue();

                // PATH
                BencodeResult pathResult = file.get(toBenString("path"));
                if (pathResult == null || pathResult.getType() != BencodeType.LIST)
                    throw new BencodeException("Incorrect path of file inside \"files\" field.");
                @SuppressWarnings("unchecked")
                List<BencodeResult> path = (List<BencodeResult>) pathResult.getValue();
                List<String> pathStrings = new ArrayList<>(path.size());

                for (BencodeResult pathPiece : path) {
                    if (pathPiece == null || pathPiece.getType() != BencodeType.STRING)
                        throw new BencodeException("Incorrect path of file inside \"files\" field.");
                    pathStrings.add(fromBenString(pathPiece));
                }

                this.files.add(new TorrentFile(i, length, String.join(File.separator, pathStrings)));
                i++;
            }
        }
    }

    // FIXME: See .torrent structure in README.md.
    public Torrent(String filename) throws IOException, BencodeException {
        this(new File(filename));
    }

    public Tracker getTracker() {
        return this.tracker;
    }

    public String getAnnounce() {
        return this.announce;
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

    private static BencodeString toBenString(String s) throws UnsupportedEncodingException {
        return new BencodeString(s, ENCODING);
    }

    private static String fromBenString(BencodeResult bencodeResult) throws BencodeException {
        return ((BencodeString) bencodeResult.getValue()).getString();
    }
}
