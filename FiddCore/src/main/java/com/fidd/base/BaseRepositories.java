package com.fidd.base;

import com.fidd.connectors.FiddConnectorFactory;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.pki.StableTransformForAlgo;
import com.fidd.core.random.RandomGeneratorType;

public interface BaseRepositories {
    Repository<EncryptionAlgorithm> encryptionAlgorithmRepo();
    Repository<FiddKeySerializer> fiddKeyFormatRepo();
    Repository<MetadataContainerSerializer> metadataContainerFormatRepo();
    Repository<FiddFileMetadataSerializer> fiddFileMetadataFormatRepo();
    Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo();

    Repository<PublicKeySerializer> publicKeyFormatRepo();
    Repository<StableTransformForAlgo> stableTransformRepo();
    Repository<SignerChecker> signatureFormatRepo();
    Repository<CrcCalculator> crcCalculatorsRepo();

    Repository<RandomGeneratorType> randomGeneratorsRepo();
    Repository<FiddConnectorFactory> fiddConnectorFactoryRepo();
}
