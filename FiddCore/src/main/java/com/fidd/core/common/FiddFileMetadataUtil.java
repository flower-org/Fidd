package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddFileMetadataUtil {
    public final static Logger LOGGER = LoggerFactory.getLogger(FiddFileMetadataUtil.class);

    public static Pair<FiddFileMetadata, MetadataContainer> loadFiddFileMetadata(BaseRepositories baseRepositories,
                                                                                 FiddConnector fiddConnector,
                                                                                 long messageNumber,
                                                                                 FiddKey.Section fiddFileMetadataSection,
                                                                                 String metadataContainerSerializerFormat
    ) throws IOException, NotEnoughBytesException {
        MetadataContainerSerializer metadataContainerSerializer =
                checkNotNull(baseRepositories.metadataContainerFormatRepo().get(metadataContainerSerializerFormat));

        try (InputStream metadataSectionStream = fiddConnector.getFiddMessageChunk(messageNumber,
                fiddFileMetadataSection.sectionOffset(), fiddFileMetadataSection.sectionLength())) {
            String encryptionAlgorithmName = fiddFileMetadataSection.encryptionAlgorithm();
            LOGGER.info("Loading and decrypting FiddFileMetadata Section: Encryption Algorithm " + encryptionAlgorithmName);
            byte[] sectionBytes = metadataSectionStream.readAllBytes();
            EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(encryptionAlgorithmName);
            if (encryptionAlgorithm == null) {
                throw new RuntimeException("Can't load FiddFileMetadata Encryption algorithm " + encryptionAlgorithmName +
                        " not supported. Can't Proceed!");
            }

            byte[] metadataContainerBytes;
            if (fiddFileMetadataSection.encryptionKeyData() != null) {
                metadataContainerBytes = checkNotNull(encryptionAlgorithm).decrypt(fiddFileMetadataSection.encryptionKeyData(), sectionBytes);
            } else {
                metadataContainerBytes = sectionBytes;
            }
            LOGGER.info("FiddFileMetadata Section decrypted successfully");

            LOGGER.info("Loading FiddFileMetadataContainer using format: " + metadataContainerSerializer.name());
            MetadataContainerSerializer.MetadataContainerAndLength metadataContainer =
                    metadataContainerSerializer.deserialize(metadataContainerBytes);

            String fiddFileMetadataFormat = metadataContainer.metadataContainer().metadataFormat();
            LOGGER.info("Loading FiddFileMetadata using format: " + fiddFileMetadataFormat);
            FiddFileMetadataSerializer fiddFileMetadataSerializer = baseRepositories.fiddFileMetadataFormatRepo().get(fiddFileMetadataFormat);
            if (fiddFileMetadataSerializer == null) {
                throw new RuntimeException("Can't load FiddFileMetadata Encryption algorithm " + encryptionAlgorithmName +
                        " not supported. Can't Proceed!");
            }
            FiddFileMetadata fiddFileMetadata =
                    checkNotNull(fiddFileMetadataSerializer).deserialize(metadataContainer.metadataContainer().metadata());

            LOGGER.info("FiddFileMetadata loaded");
            return Pair.of(fiddFileMetadata, metadataContainer.metadataContainer());
        }
    }
}
