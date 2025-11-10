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

    @Override
    public Repository<EncryptionAlgorithm> encryptionAlgorithmRepo() {
        Aes256CbcEncryptionAlgorithm aes256Cbc = new Aes256CbcEncryptionAlgorithm();
        return new MapRepository<>(aes256Cbc.name(), List.of(aes256Cbc));
    }

    @Override
    public Repository<FiddKeySerializer> fiddKeyFormatRepo() {
        YamlFiddKeySerializer yamlFiddKeySerializer = new YamlFiddKeySerializer();
        return new MapRepository<>(yamlFiddKeySerializer.name(), List.of(yamlFiddKeySerializer));
    }

    @Override
    public Repository<FiddFileMetadataSerializer> fiddFileMetadataFormatRepo() {
        YamlFiddFileMetadataSerializer yamlFiddFileMetadataSerializer = new YamlFiddFileMetadataSerializer();
        return new MapRepository<>(yamlFiddFileMetadataSerializer.name(), List.of(yamlFiddFileMetadataSerializer));
    }

    @Override
    public Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo() {
        YamlLogicalFileMetadataSerializer yamlLogicalFileMetadataSerializer = new YamlLogicalFileMetadataSerializer();
        return new MapRepository<>(yamlLogicalFileMetadataSerializer.name(), List.of(yamlLogicalFileMetadataSerializer));
    }

    @Override
    public Repository<PublicKeySerializer> publicKeyFormatRepo() {
        X509PublicKeySerializer x509 = new X509PublicKeySerializer();
        return new MapRepository<>(x509.name(), List.of(x509));
    }

    @Override
    public Repository<SignerChecker> signatureFormatRepo() {
        SHA256WithRSASignerChecker signerChecker = new SHA256WithRSASignerChecker();
        return new MapRepository<>(signerChecker.name(), List.of(signerChecker));
    }

    @Override
    public Repository<RandomGeneratorType> randomGeneratorsRepo() {
        PlainRandomGeneratorType plainRandomGeneratorType = new PlainRandomGeneratorType();
        SecureRandomGeneratorType secureRandomGeneratorType = new SecureRandomGeneratorType();
        return new MapRepository<>(secureRandomGeneratorType.name(), List.of(secureRandomGeneratorType, plainRandomGeneratorType));
    }
}
