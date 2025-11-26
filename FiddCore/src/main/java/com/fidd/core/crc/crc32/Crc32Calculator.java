package com.fidd.core.crc.crc32;

import com.fidd.core.crc.CrcCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

public class Crc32Calculator implements CrcCalculator {
    @Override
    public byte[] calculateCrc(byte[] data) {
        return calculateCrc(data, new CRC32());
    }

    @Override
    public byte[] calculateCrc(InputStream dataStream) throws IOException {
        return calculateCrc(dataStream, new CRC32());
    }

    @Override
    public String name() {
        return "CRC32";
    }
}
