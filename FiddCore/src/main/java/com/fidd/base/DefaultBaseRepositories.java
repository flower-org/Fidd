package com.fidd.base;

import com.fidd.core.NamedEntry;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.encryption.aes256.Aes256CbcEncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
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
        Aes256CbcEncryptionAlgorithm encryptionAlgorithm = new Aes256CbcEncryptionAlgorithm();
        return new MapRepository<>(encryptionAlgorithm.name(), List.of(encryptionAlgorithm));
    }

    @Override
    public Repository<FiddKeySerializer> fiddKeyFormatRepo() {
        return createEmpty();
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
