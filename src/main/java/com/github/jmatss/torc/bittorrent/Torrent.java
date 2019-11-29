package com.github.jmatss.torc.bittorrent;

import com.github.jmatss.torc.bencode.Bencode;
import com.github.jmatss.torc.bencode.BencodeException;
import com.github.jmatss.torc.bencode.BencodeResult;
import com.github.jmatss.torc.bencode.BencodeType;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.github.jmatss.torc.TMP_CONST.SHA1_HASH_LENGTH;


/**
 * Represents a torrent. Created either with .torrent-file or a magnet link.
 */
public class Torrent {
    public static final int PORT = 6881;    // TODO: Make list of 6881-6889 instead(?)
    public static final int MAX_REQUEST_LENGTH = 1 << 14;   // 2^14 most common.

    // Mutex used when changing filename or moving the file.
    private final Lock mutex;

    // Contains data related to the active tracker.
    private Tracker tracker;

    // Contains the URL of the tracker.
    private final String announce;

    // Contains the bitfield of the pieces that this client have downloaded
    // and can be seeded to other clients.
    private byte[] bitfieldHave;

    // Contains the bitfield of the pieces that this client have downloaded (= bitfieldHave)
    // and also the pieces that the peerHandlers are currently downloading.
    // This can be used to see which pieces that are free to start downloading.
    private byte[] bitfieldDownloading;

    // Contains the "root" directory if this torrent is a "multi-file torrent".
    // Contains the path (== files[0].path) if this torrent is "single-file torrent".
    private final Path name;

    // Contains info regarding the files of the torrent.
    private final List<FILE_TEMP> files;

    // SHA1 hashes of all pieces concatenated (pieces.length % 20 == 0).
    private final byte[][] pieces;

    // Length of a single piece in bytes.
    // All pieces have the same length expected the last one that will be less that pieceLength.
    private final long pieceLength;

    private class FILE_TEMP {
        private final long index;
        private final long length;
        private final Path path;    // TODO: List of strings instead(?)

        FILE_TEMP(long index, long length, Path path) {
            this.index = index;
            this.length = length;
            this.path = path;
        }

        FILE_TEMP(long index, long length, String path) {
            this.index = index;
            this.length = length;
            this.path = Paths.get(path);
        }

        public long getIndex() {
            return this.index;
        }

        public long getLength() {
            return this.length;
        }

        public Path getPath() {
            return this.path;
        }
    }

    // FIXME: See .torrent structure in README.md.
    public Torrent(String filename) throws IOException, BencodeException {
        File f = new File(filename);
        if (!f.exists())
            throw new IOException("File \"" + filename + "\" doesn't exist.");
        else if (!f.isFile())
            throw new IOException("File \"" + filename + "\" isn't a valid file.");

        Bencode bencode = new Bencode(f);
        Map<String, BencodeResult> torrentDictionary = bencode.getDictionary();

        this.mutex = new ReentrantLock();

        // ANNOUNCE
        BencodeResult announce = torrentDictionary.get("announce");
        if (announce == null || announce.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"announce\" field.");
        this.announce = (String) announce.getValue();

        // INFO
        BencodeResult infoResult = torrentDictionary.get("info");
        if (infoResult == null || infoResult.getType() != BencodeType.DICTIONARY)
            throw new BencodeException("Incorrect \"info\" field.");
        @SuppressWarnings("unchecked")
        Map<String, BencodeResult> info = (Map<String, BencodeResult>) infoResult.getValue();

        // NAME
        BencodeResult name = info.get("name");
        if (name == null || name.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"name\" field.");
        this.name = Paths.get((String) name.getValue());

        // PIECE LENGTH
        BencodeResult pieceLength = info.get("piece length");
        if (pieceLength == null || pieceLength.getType() != BencodeType.NUMBER)
            throw new BencodeException("Incorrect \"piece length\" field.");
        this.pieceLength = (long) pieceLength.getValue();

        // PIECES
        BencodeResult pieces = info.get("pieces");
        if (pieces == null || pieces.getType() != BencodeType.STRING)
            throw new BencodeException("Incorrect \"pieces\" field.");
        String piecesString = (String) pieces.getValue();
        if (piecesString.length() % SHA1_HASH_LENGTH != 0)
            throw new BencodeException("Field \"pieces\" isn't divisible by sha1 length.");

        ByteBuffer buffer = ByteBuffer
                .allocate(piecesString.length())
                .put(piecesString.getBytes());
        buffer.rewind();
        this.pieces = new byte[piecesString.length() / SHA1_HASH_LENGTH][];
        for (int i = 0; i < this.pieces.length; i++) {
            byte[] currentDigest = new byte[SHA1_HASH_LENGTH];
            buffer.get(currentDigest);
            this.pieces[i++] = currentDigest;
        }

        // If true: this is a single-file torrent.
        // Else: this is a multi-file torrent.
        if (!info.containsKey("files")) {
            // LENGTH
            BencodeResult length = info.get("length");
            if (length == null || length.getType() != BencodeType.NUMBER)
                throw new BencodeException("Incorrect \"length\" field.");
            this.files = List.of(new FILE_TEMP(0, (long) length.getValue(), this.name));
        } else {
            // FILES
            BencodeResult filesResult = torrentDictionary.get("files");
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
                Map<String, BencodeResult> file = (Map<String, BencodeResult>) fileResult.getValue();

                // LENGTH
                BencodeResult lengthResult = file.get("length");
                if (lengthResult == null || lengthResult.getType() != BencodeType.NUMBER)
                    throw new BencodeException("Incorrect length of file inside \"files\" field.");
                long length = (long) lengthResult.getValue();

                // PATH
                BencodeResult pathResult = file.get("path");
                if (pathResult == null || pathResult.getType() != BencodeType.LIST)
                    throw new BencodeException("Incorrect path of file inside \"files\" field.");
                @SuppressWarnings("unchecked")
                List<BencodeResult> path = (List<BencodeResult>) pathResult.getValue();
                List<String> pathStrings = new ArrayList<>(path.size());

                for (BencodeResult p : path) {
                    if (p == null || p.getType() != BencodeType.STRING)
                        throw new BencodeException("Incorrect path of file inside \"files\" field.");
                    pathStrings.add((String) p.getValue());
                }

                this.files.add(new FILE_TEMP(i, length, String.join(File.separator, pathStrings)));
                i++;
            }
        }
    }
}
