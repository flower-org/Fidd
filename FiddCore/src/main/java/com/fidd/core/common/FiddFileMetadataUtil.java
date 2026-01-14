package com.fidd.core.common;

import com.fidd.base.BaseRepositories;
import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
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

    public final static BaseRepositories BASE_REPOSITORIES;
    public final static Repository<EncryptionAlgorithm> ENCRYPTION_ALGORITHM_REPO;
    public final static Repository<FiddFileMetadataSerializer> FIDD_FILE_METADATA_FORMAT_REPO;

    static {
        BASE_REPOSITORIES = new DefaultBaseRepositories();
        ENCRYPTION_ALGORITHM_REPO = BASE_REPOSITORIES.encryptionAlgorithmRepo();
        FIDD_FILE_METADATA_FORMAT_REPO = BASE_REPOSITORIES.fiddFileMetadataFormatRepo();
    }

    public static Pair<FiddFileMetadata, MetadataContainer> loadFiddFileMetadata(FiddConnector fiddConnector,
                                                                                 long messageNumber,
                                                                                 FiddKey.Section fiddFileMetadataSection,
                                                                                 String metadataContainerSerializerFormat
    ) throws IOException, NotEnoughBytesException {
        MetadataContainerSerializer metadataContainerSerializer =
                checkNotNull(BASE_REPOSITORIES.metadataContainerFormatRepo().get(metadataContainerSerializerFormat));

        try (InputStream metadataSectionStream = fiddConnector.getFiddMessageChunk(messageNumber,
                fiddFileMetadataSection.sectionOffset(), fiddFileMetadataSection.sectionLength())) {
            String encryptionAlgorithmName = fiddFileMetadataSection.encryptionAlgorithm();
            LOGGER.info("Loading and decrypting FiddFileMetadata Section: Encryption Algorithm " + encryptionAlgorithmName);
            byte[] sectionBytes = metadataSectionStream.readAllBytes();
            EncryptionAlgorithm encryptionAlgorithm = ENCRYPTION_ALGORITHM_REPO.get(encryptionAlgorithmName);
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
            FiddFileMetadataSerializer fiddFileMetadataSerializer = FIDD_FILE_METADATA_FORMAT_REPO.get(fiddFileMetadataFormat);
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
