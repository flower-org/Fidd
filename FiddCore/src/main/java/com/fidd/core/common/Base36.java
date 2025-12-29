package com.fidd.core.common;

import java.math.BigInteger;

public class Base36 {
    public static String toBase36(byte[] bytes) {
        // BigInteger interprets the byte[] as a SIGNED two's‑complement number.
        // If the highest bit of the first byte is 1, BigInteger will treat it as NEGATIVE.
        // Prepending a 0x00 byte forces the value to be interpreted as UNSIGNED.
        byte[] unsigned = new byte[bytes.length + 1];
        System.arraycopy(bytes, 0, unsigned, 1, bytes.length);

        return new BigInteger(unsigned).toString(36).toUpperCase();
    }

    public static byte[] fromBase36(String s) {
        BigInteger value = new BigInteger(s, 36);
        byte[] raw = value.toByteArray();
        return (raw.length > 1 && raw[0] == 0)
                ? java.util.Arrays.copyOfRange(raw, 1, raw.length)
                : raw;
    }
}
