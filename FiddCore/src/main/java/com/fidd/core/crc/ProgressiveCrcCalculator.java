package com.fidd.core.crc;

import java.io.IOException;
import java.io.InputStream;

public class ProgressiveCrcCalculator {
    public static byte[] calculateProgressiveCrc(InputStream dataStream, long chunkSize, CrcCalculator crcCalculator) throws IOException {
        // TODO: calculate per mb
        return crcCalculator.calculateCrc(dataStream);
    }
}
