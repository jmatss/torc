package com.github.jmatss.torc.bencode;

import java.io.UnsupportedEncodingException;
import com.github.jmatss.torc.TMP_CONST;

public class BencodeUtil {
    public static BencodeString toBenString(String s) throws UnsupportedEncodingException {
        return new BencodeString(s, TMP_CONST.ENCODING);
    }

    public static String fromBenString(BencodeData<Object> bencodeResult)
    throws BencodeException {
        Object value = bencodeResult.getValidatedValue(BencodeType.STRING);
        if (!(value instanceof BencodeString)) {
            return null;
        }

        return ((BencodeString)value).getString();
    }
}