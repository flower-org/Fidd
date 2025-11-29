package com.fidd.base;

import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.crc.adler32.Adler32Calculator;
import com.fidd.core.crc.crc32.Crc32Calculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.encryption.unencrypted.NoEncryptionAlgorithm;
import com.fidd.core.encryption.xor.XorEncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.yaml.YamlFiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.fiddkey.yaml.YamlFiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.logicalfile.yaml.YamlLogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.blobs.BlobsMetadataContainerSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.pki.sha256WithRsa.SHA256WithRSASignerChecker;
import com.fidd.core.pki.x509.X509PublicKeySerializer;
import com.fidd.core.random.RandomGeneratorType;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import com.fidd.core.random.secure.SecureRandomGeneratorType;

import java.util.List;

public class DefaultBaseRepositories implements BaseRepositories {
    static final Repository<EncryptionAlgorithm> ENCRYPTION_ALGORITHM_REPO;
    static final Repository<FiddKeySerializer> FIDD_KEY_FORMAT_REPO;
    static final Repository<MetadataContainerSerializer> METADATA_SECTION_FORMAT_REPO;
    static final Repository<FiddFileMetadataSerializer> FIDD_FILE_METADATA_FORMAT_REPO;
    static final Repository<LogicalFileMetadataSerializer> LOGICAL_FILE_METADATA_FORMAT_REPO;
    static final Repository<PublicKeySerializer> PUBLIC_KEY_FORMAT_REPO;
    static final Repository<SignerChecker> SIGNATURE_FORMAT_REPO;
    static final Repository<RandomGeneratorType> RANDOM_GENERATORS_REPO;
    static final Repository<CrcCalculator> CRC_CALCULATOR_REPO;

    static {
        Aes256CbcEncryptionAlgorithm aes256Cbc = new Aes256CbcEncryptionAlgorithm();
        XorEncryptionAlgorithm xor = new XorEncryptionAlgorithm();
        NoEncryptionAlgorithm noEncryption = new NoEncryptionAlgorithm();
        ENCRYPTION_ALGORITHM_REPO = new MapRepository<>(aes256Cbc.name(), List.of(aes256Cbc, xor, noEncryption), noEncryption);

        YamlFiddKeySerializer yamlFiddKeySerializer = new YamlFiddKeySerializer();
        FIDD_KEY_FORMAT_REPO = new MapRepository<>(yamlFiddKeySerializer.name(), List.of(yamlFiddKeySerializer));

        BlobsMetadataContainerSerializer blobsMetadataContainerSerializer = new BlobsMetadataContainerSerializer();
        METADATA_SECTION_FORMAT_REPO = new MapRepository<>(blobsMetadataContainerSerializer.name(), List.of(blobsMetadataContainerSerializer));

        YamlFiddFileMetadataSerializer yamlFiddFileMetadataSerializer = new YamlFiddFileMetadataSerializer();
        FIDD_FILE_METADATA_FORMAT_REPO = new MapRepository<>(yamlFiddFileMetadataSerializer.name(), List.of(yamlFiddFileMetadataSerializer));

        YamlLogicalFileMetadataSerializer yamlLogicalFileMetadataSerializer = new YamlLogicalFileMetadataSerializer();
        LOGICAL_FILE_METADATA_FORMAT_REPO = new MapRepository<>(yamlLogicalFileMetadataSerializer.name(), List.of(yamlLogicalFileMetadataSerializer));

        X509PublicKeySerializer x509 = new X509PublicKeySerializer();
        PUBLIC_KEY_FORMAT_REPO = new MapRepository<>(x509.name(), List.of(x509));

        SHA256WithRSASignerChecker signerChecker = new SHA256WithRSASignerChecker();
        SIGNATURE_FORMAT_REPO = new MapRepository<>(signerChecker.name(), List.of(signerChecker));

        PlainRandomGeneratorType plainRandomGeneratorType = new PlainRandomGeneratorType();
        SecureRandomGeneratorType secureRandomGeneratorType = new SecureRandomGeneratorType();
        RANDOM_GENERATORS_REPO = new MapRepository<>(secureRandomGeneratorType.name(), List.of(secureRandomGeneratorType, plainRandomGeneratorType));

        Adler32Calculator adler32Calculator = new Adler32Calculator();
        Crc32Calculator crc32Calculator = new Crc32Calculator();
        CRC_CALCULATOR_REPO = new MapRepository<>(adler32Calculator.name(), List.of(adler32Calculator, crc32Calculator));
    }

    @Override
    public Repository<EncryptionAlgorithm> encryptionAlgorithmRepo() {
        return ENCRYPTION_ALGORITHM_REPO;
    }

    @Override
    public Repository<FiddKeySerializer> fiddKeyFormatRepo() {
        return FIDD_KEY_FORMAT_REPO;
    }

    @Override
    public Repository<MetadataContainerSerializer> metadataContainerFormatRepo() {
        return METADATA_SECTION_FORMAT_REPO;
    }

    @Override
    public Repository<FiddFileMetadataSerializer> fiddFileMetadataFormatRepo() {
        return FIDD_FILE_METADATA_FORMAT_REPO;
    }

    @Override
    public Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo() {
        return LOGICAL_FILE_METADATA_FORMAT_REPO;
    }

    @Override
    public Repository<PublicKeySerializer> publicKeyFormatRepo() {
        return PUBLIC_KEY_FORMAT_REPO;
    }

    @Override
    public Repository<SignerChecker> signatureFormatRepo() {
        return SIGNATURE_FORMAT_REPO;
    }

    @Override
    public Repository<CrcCalculator> crcCalculatorsRepo() {
        return CRC_CALCULATOR_REPO;
    }

    @Override
    public Repository<RandomGeneratorType> randomGeneratorsRepo() {
        return RANDOM_GENERATORS_REPO;
    }
}
