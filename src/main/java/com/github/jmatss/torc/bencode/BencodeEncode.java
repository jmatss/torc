package com.github.jmatss.torc.bencode;

import com.github.jmatss.torc.util.DynamicByteBuffer;

import java.util.List;
import java.util.Map;

public class BencodeEncode extends Bencode {
    private final BencodeResult startBencodeResult;

    public BencodeEncode(BencodeResult bencodeResult) {
        this.startBencodeResult = bencodeResult;
    }

    public byte[] encode() throws BencodeException {
        return encode(this.startBencodeResult, new DynamicByteBuffer()).getBytes();
    }

    private DynamicByteBuffer encode(BencodeResult bencodeResult, DynamicByteBuffer accResult) throws BencodeException {
        switch (bencodeResult.getType()) {
            case NUMBER:
                var number = (long) bencodeResult.getValue();
                accResult
                        .put((byte) BencodeType.NUMBER.getChar())
                        .putLongBytes(number)
                        .put((byte) BencodeType.END.getChar());
                break;

            case STRING:
                var benString = (BencodeString) bencodeResult.getValue();
                accResult
                        .putIntBytes(benString.getBytes().length)
                        .put((byte) STRING_SEPARATOR)
                        .put(benString.getBytes());
                break;

            case LIST:
                @SuppressWarnings("unchecked")
                var list = (List<BencodeResult>) bencodeResult.getValue();
                accResult.put((byte) BencodeType.LIST.getChar());
                for (BencodeResult listBenResult : list) {
                    encode(listBenResult, accResult);
                }
                accResult.put((byte) BencodeType.END.getChar());
                break;

            case DICTIONARY:
                @SuppressWarnings("unchecked")
                var dictionary = (Map<BencodeString, BencodeResult>) bencodeResult.getValue();
                accResult.put((byte) BencodeType.DICTIONARY.getChar());
                for (var dictionaryBenResult : dictionary.entrySet()) {
                    var keyString = dictionaryBenResult.getKey();
                    var key = new BencodeResult<>(BencodeType.STRING, keyString);
                    var value = dictionaryBenResult.getValue();

                    encode(key, accResult);
                    encode(value, accResult);
                }
                accResult.put((byte) BencodeType.END.getChar());
                break;

            default:
                throw new BencodeException("Parsed incorrect BencodeType.");
        }

        return accResult;
    }
}
