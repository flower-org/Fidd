package com.fidd.base;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.FiddFileMetadataSignatureSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignatureSerializer;

import java.util.random.RandomGenerator;

public interface BaseRepositories {
    Repository<EncryptionAlgorithm> encryptionAlgorithmRepo();
    Repository<FiddKeySerializer> fiddKeyFormatRepo();
    Repository<FiddFileMetadataSerializer> fiddFileMetadataFormatRepo();
    Repository<FiddFileMetadataSignatureSerializer> fiddFileMetadataSignatureFormatRepo();
    Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo();

    Repository<PublicKeySerializer> publicKeyFormatRepo();
    Repository<SignatureSerializer> signatureFormatRepo();

    Repository<RandomGenerator> randomGeneratorsRepo();
}
