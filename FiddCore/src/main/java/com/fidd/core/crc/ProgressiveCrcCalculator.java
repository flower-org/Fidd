package com.fidd.core.crc;

import com.fidd.core.common.LimitedInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressiveCrcCalculator {
    public static byte[] calculateProgressiveCrc(InputStream dataStream, long chunkSize, CrcCalculator crcCalculator) throws IOException {
        if (chunkSize <= 0) { throw new IllegalArgumentException("chunkSize must be positive"); }

        ByteArrayOutputStream progressiveCrc = new ByteArrayOutputStream();

        while (true) {
            LimitedInputStream chunkStream = new LimitedInputStream(dataStream, chunkSize);

            // Try to calculate CRC directly; if no data left, break
            byte[] crc = crcCalculator.calculateCrc(chunkStream);
            if (chunkStream.getBytesRead() == 0) {
                break; // nothing read, EOF
            }

            progressiveCrc.write(crc);

            // If fewer than chunkSize bytes were consumed, underlying stream reached EOF
            if (chunkStream.getBytesRead() < chunkSize) {
                break;
            }
        }

        return progressiveCrc.toByteArray();
    }
}
