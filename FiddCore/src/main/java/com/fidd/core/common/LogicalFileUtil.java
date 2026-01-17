package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddkey.FiddKey;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class LogicalFileUtil {
    public static InputStream getLogicalFileInputStream(BaseRepositories baseRepositories, FiddConnector fiddConnector,
                                                 long messageNumber, FiddKey.Section section, long fileOffset) throws IOException {
        EncryptionAlgorithm encryptionAlgorithm =
                baseRepositories.encryptionAlgorithmRepo().get(section.encryptionAlgorithm());

        byte[] keyData = section.encryptionKeyData() == null ? new byte[0] : section.encryptionKeyData();
        InputStream logicalFileStream =
                checkNotNull(encryptionAlgorithm).getDecryptedStream(keyData,
                             fiddConnector.getFiddMessageChunk(messageNumber, section.sectionOffset(),
                                     section.sectionLength()));
        skipAll(logicalFileStream, fileOffset);

        return logicalFileStream;
    }

    public static void skipAll(InputStream stream, long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = stream.skip(remaining);
            if (skipped <= 0) {
                // If skip() returns 0, try reading and discarding one byte
                if (stream.read() == -1) {
                    throw new EOFException("Reached end of stream before skipping " + n + " bytes");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }
}
