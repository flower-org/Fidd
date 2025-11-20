package com.fidd.base;

import com.fidd.core.NamedEntry;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.yaml.YamlFiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.fiddkey.yaml.YamlFiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.logicalfile.yaml.YamlLogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataSectionSerializer;
import com.fidd.core.metadata.yaml.YamlMetadataSectionSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.pki.sha256WithRsa.SHA256WithRSASignerChecker;
import com.fidd.core.pki.x509.X509PublicKeySerializer;
import com.fidd.core.random.RandomGeneratorType;
import com.fidd.core.random.plain.PlainRandomGeneratorType;
import com.fidd.core.random.secure.SecureRandomGeneratorType;

import java.util.List;

public class DefaultBaseRepositories implements BaseRepositories {
    <T extends NamedEntry> Repository<T> createEmpty() {
        return new MapRepository<>(null, List.of());
    }

    static Repository<EncryptionAlgorithm> ENCRYPTION_ALGORITHM_REPO;
    static Repository<FiddKeySerializer> FIDD_KEY_FORMAT_REPO;
    static Repository<MetadataSectionSerializer> METADATA_SECTION_FORMAT_REPO;
    static Repository<FiddFileMetadataSerializer> FIDD_FILE_METADATA_FORMAT_REPO;
    static Repository<LogicalFileMetadataSerializer> LOGICAL_FILE_METADATA_FORMAT_REPO;
    static Repository<PublicKeySerializer> PUBLIC_KEY_FORMAT_REPO;
    static Repository<SignerChecker> SIGNATURE_FORMAT_REPO;
    static Repository<RandomGeneratorType> RANDOM_GENERATORS_REPO;

    static {
        Aes256CbcEncryptionAlgorithm aes256Cbc = new Aes256CbcEncryptionAlgorithm();
        ENCRYPTION_ALGORITHM_REPO = new MapRepository<>(aes256Cbc.name(), List.of(aes256Cbc));

        YamlFiddKeySerializer yamlFiddKeySerializer = new YamlFiddKeySerializer();
        FIDD_KEY_FORMAT_REPO = new MapRepository<>(yamlFiddKeySerializer.name(), List.of(yamlFiddKeySerializer));

        YamlMetadataSectionSerializer yamlMetadataSectionSerializer = new YamlMetadataSectionSerializer();
        METADATA_SECTION_FORMAT_REPO = new MapRepository<>(yamlMetadataSectionSerializer.name(), List.of(yamlMetadataSectionSerializer));

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
    public Repository<MetadataSectionSerializer> metadataSectionFormatRepo() {
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
    public Repository<RandomGeneratorType> randomGeneratorsRepo() {
        return RANDOM_GENERATORS_REPO;
    }
}
