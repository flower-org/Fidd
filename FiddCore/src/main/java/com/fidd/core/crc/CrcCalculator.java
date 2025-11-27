package com.fidd.core.crc;

import com.fidd.core.NamedEntry;
import com.fidd.core.encryption.EncryptionAlgorithm;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.Checksum;

public interface CrcCalculator extends NamedEntry {
    byte[] calculateCrc(byte[] data);
    byte[] calculateCrc(InputStream dataStream) throws IOException;
    EncryptionAlgorithm.CrcCallback newCrcCallback();

    default byte[] calculateCrc(byte[] data, Checksum checksum) {
        checksum.update(data, 0, data.length);
        return toBytes(checksum.getValue());
    }

    default byte[] calculateCrc(InputStream dataStream, Checksum checksum) throws IOException {
        byte[] buffer = new byte[4096];
        int read;
        while ((read = dataStream.read(buffer)) != -1) {
            checksum.update(buffer, 0, read);
        }
        return toBytes(checksum.getValue());
    }

    static byte[] toBytes(long value) {
        // Always return 4-byte checksum (big-endian)
        return new byte[] {
                (byte) ((value >> 24) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) (value & 0xFF)
        };
    }
}
