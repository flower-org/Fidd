package com.fidd.base;

import com.fidd.core.NamedEntry;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.fiddkey.yaml.YamlFiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignatureSerializer;
import com.fidd.core.random.RandomGeneratorNamedEntry;

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
        return createEmpty();
    }

    @Override
    public Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo() {
        return createEmpty();
    }

    @Override
    public Repository<PublicKeySerializer> publicKeyFormatRepo() {
        return createEmpty();
    }

    @Override
    public Repository<SignatureSerializer> signatureFormatRepo() {
        return createEmpty();
    }

    @Override
    public Repository<RandomGeneratorNamedEntry> randomGeneratorsRepo() {
        return createEmpty();
    }
}
