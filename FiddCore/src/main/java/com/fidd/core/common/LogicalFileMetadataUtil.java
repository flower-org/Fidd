package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class LogicalFileMetadataUtil {
    public final static Logger LOGGER = LoggerFactory.getLogger(LogicalFileMetadataUtil.class);

    // TODO: hardcoding this to "BLOBS" for now
    final static String METADATA_CONTAINER_SERIALIZER_FORMAT = "BLOBS";

    public static @Nullable Pair<LogicalFileMetadata, MetadataContainerSerializer.MetadataContainerAndLength>
    getLogicalFileMetadata(BaseRepositories baseRepositories, FiddConnector fiddConnector, long messageNumber,
                           FiddKey.Section logicalFileSection) throws IOException {
        String encryptionAlgorithmName = logicalFileSection.encryptionAlgorithm();
        EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(encryptionAlgorithmName);
        if (encryptionAlgorithm == null) {
            throw new RuntimeException("EncryptionAlgorithm " + encryptionAlgorithmName + " not supported - can't process Logical file");
        }

        MetadataContainerSerializer metadataContainerSerializer =
                checkNotNull(baseRepositories.metadataContainerFormatRepo().get(METADATA_CONTAINER_SERIALIZER_FORMAT));

        return getLogicalFileMetadata(baseRepositories, encryptionAlgorithm, fiddConnector,
                messageNumber, logicalFileSection, metadataContainerSerializer, true);
    }

    public static @Nullable Pair<LogicalFileMetadata, MetadataContainerSerializer.MetadataContainerAndLength>
    getLogicalFileMetadata(BaseRepositories baseRepositories,
                           EncryptionAlgorithm encryptionAlgorithm, FiddConnector fiddConnector,
                           long messageNumber, FiddKey.Section logicalFileSection,
                           MetadataContainerSerializer metadataContainerSerializer,
                           boolean throwOnValidationFailure) throws IOException {
        MetadataContainerSerializer.MetadataContainerAndLength metadataContainerAndLength = null;
        try (InputStream sectionInputStream = fiddConnector.getFiddMessageChunk(messageNumber,
                logicalFileSection.sectionOffset(), logicalFileSection.sectionLength())) {
            byte[] cumul = new byte[0];
            int bufferSize = (int)Math.min(4096L, logicalFileSection.sectionLength());
            byte[] buffer = new byte[bufferSize];
            int totalRead = 0;
            while (totalRead < logicalFileSection.sectionLength()) {
                int bytesRead = sectionInputStream.read(buffer);
                if (bytesRead == -1) {
                    warnAndMaybeThrow("Failed to read metadata", throwOnValidationFailure);
                    return null;
                }
                totalRead += bytesRead;
                cumul = concat(cumul, buffer, bytesRead);

                try {
                    InputStream ciphertextStream = new ByteArrayInputStream(cumul);
                    ByteArrayOutputStream plaintextStream = new ByteArrayOutputStream();

                    if (logicalFileSection.encryptionKeyData() != null) {
                        encryptionAlgorithm.decrypt(logicalFileSection.encryptionKeyData(), ciphertextStream, plaintextStream, true);
                        LOGGER.info("LogicalFileMetadata Section decrypted successfully");
                    } else {
                        LOGGER.info("Key not found for LogicalFileMetadata Section");
                        encryptionAlgorithm.decrypt(new byte[]{}, ciphertextStream, plaintextStream, true);
                        LOGGER.info("DUMMY KEY LoicalFileMetadata Section \"decrypted\" with empty key");
                    }

                    byte[] metadataContainerBytes = plaintextStream.toByteArray();
                    metadataContainerAndLength = metadataContainerSerializer.deserialize(metadataContainerBytes);
                    break;
                } catch (NotEnoughBytesException ne) {
                    // Load more bytes then
                }
            }
        }

        String logicalFileMetadataFormat = checkNotNull(metadataContainerAndLength).metadataContainer().metadataFormat();
        LogicalFileMetadataSerializer logicalFileMetadataSerializer =
                baseRepositories.logicalFileMetadataFormatRepo().get(logicalFileMetadataFormat);
        if (logicalFileMetadataSerializer == null) {
            warnAndMaybeThrow("LogicalFileMetadata format " + logicalFileMetadataFormat + " is not supported.",
                    throwOnValidationFailure);
            return null;
        } else {
            LogicalFileMetadata logicalFileMetadata =
                    logicalFileMetadataSerializer.deserialize(metadataContainerAndLength.metadataContainer().metadata());
            return Pair.of(logicalFileMetadata, metadataContainerAndLength);
        }
    }

    static void warnAndMaybeThrow(String failedToReadMetadata, boolean throwOnValidationFailure) {
        LOGGER.warn(failedToReadMetadata);
        if (throwOnValidationFailure) {
            throw new RuntimeException(failedToReadMetadata);
        }
    }

    public static byte[] concat(byte[] buffer1, byte[] buffer2, int buffer2Len) {
        byte[] result = new byte[buffer1.length + buffer2Len];
        System.arraycopy(buffer1, 0, result, 0, buffer1.length);
        System.arraycopy(buffer2, 0, result, buffer1.length, buffer2Len);
        return result;
    }
}
