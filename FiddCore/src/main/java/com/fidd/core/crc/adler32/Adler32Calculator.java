package com.fidd.core.crc.adler32;

import com.fidd.core.crc.CrcCalculator;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Adler32;

public class Adler32Calculator implements CrcCalculator {
    @Override
    public byte[] calculateCrc(byte[] data) {
        return calculateCrc(data, new Adler32());
    }

    @Override
    public byte[] calculateCrc(InputStream dataStream) throws IOException {
        return calculateCrc(dataStream, new Adler32());
    }

    @Override
    public String name() {
        return "Adler32";
    }
}
