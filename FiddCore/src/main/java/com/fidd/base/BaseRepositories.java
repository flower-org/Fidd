package com.fidd.base;

import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;

public interface BaseRepositories {
    Repository<EncryptionAlgorithm> encryptionAlgorithmRepo();
    Repository<FiddKeySerializer> fiddKeyFormatRepo();
    Repository<FiddFileMetadataSerializer> fiddFileMetadataFormatRepo();
    Repository<LogicalFileMetadataSerializer> logicalFileMetadataFormatRepo();
}
