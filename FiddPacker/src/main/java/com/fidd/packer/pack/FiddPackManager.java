package com.fidd.packer.pack;

import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.ImmutableFiddFileMetadata;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.MetadataSectionSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.random.RandomGeneratorType;

import javax.annotation.Nullable;
import java.io.File;
import java.security.cert.X509Certificate;

public class FiddPackManager {
    public static void fiddPackNewPost(File originalDirectory,
                                File packedContentDirectory,
                                @Nullable X509Certificate authorsPublicKey,
                                long messageNumber,
                                String postId,

                                FiddKeySerializer fiddKeySerializer,
                                MetadataSectionSerializer metadataSectionSerializer,
                                FiddFileMetadataSerializer fiddFileMetadataSerializer,
                                LogicalFileMetadataSerializer logicalFileMetadataSerializer,
                                EncryptionAlgorithm encryptionAlgorithm,
                                RandomGeneratorType randomGenerator,
                                long minGapSize,
                                long maxGapSize,

                                boolean createFileAndKeySignatures,
                                boolean addFiddFileMetadataSignature,
                                boolean addLogicalFileSignatures,
                                boolean addLogicalFileMetadataSignatures,
                                PublicKeySerializer publicKeySerializer,
                                SignerChecker signerChecker,

                                boolean addCrcsToFiddKey,
                                CrcCalculator crcCalculator
    ) {
        // Build FiddFileMetadata
        ImmutableFiddFileMetadata.Builder fiddFileMetadataBuilder = ImmutableFiddFileMetadata.builder()
                .logicalFileMetadataFormatVersion(logicalFileMetadataSerializer.name())
                .messageNumber(messageNumber)
                .originalMessageNumber(messageNumber)
                .postId(postId)
                .versionNumber(1)
                .isNewOrSquash(true)
                .isDelete(false);

        if (createFileAndKeySignatures || addFiddFileMetadataSignature || addLogicalFileSignatures || addLogicalFileMetadataSignatures) {
            fiddFileMetadataBuilder.authorsPublicKeyFormat(publicKeySerializer.name())
                .authorsPublicKey(publicKeySerializer.serialize(authorsPublicKey));
        }

        if (addFiddFileMetadataSignature) {
            fiddFileMetadataBuilder.authorsFiddFileMetadataSignatureFormat(fiddFileMetadataSerializer.name());
        }

        FiddFileMetadata fiddFileMetadata = fiddFileMetadataBuilder.build();

        // Call pack with FiddFileMetadata
        fiddPack(originalDirectory,
                packedContentDirectory,
                fiddFileMetadata,

                fiddKeySerializer,
                metadataSectionSerializer,
                fiddFileMetadataSerializer,
                logicalFileMetadataSerializer,
                encryptionAlgorithm,
                randomGenerator,
                minGapSize,
                maxGapSize,

                createFileAndKeySignatures,
                addFiddFileMetadataSignature,
                addLogicalFileSignatures,
                addLogicalFileMetadataSignatures,
                publicKeySerializer,
                signerChecker,

                addCrcsToFiddKey,
                crcCalculator
            );
    }

    public static void fiddPack(File originalDirectory,
                                File packedContentDirectory,
                                FiddFileMetadata fiddFileMetadata,

                                FiddKeySerializer fiddKeySerializer,
                                MetadataSectionSerializer metadataSectionSerializer,
                                FiddFileMetadataSerializer fiddFileMetadataSerializer,
                                LogicalFileMetadataSerializer logicalFileMetadataSerializer,
                                EncryptionAlgorithm encryptionAlgorithm,
                                RandomGeneratorType randomGenerator,
                                long minGapSize,
                                long maxGapSize,

                                boolean createFileAndKeySignatures,
                                boolean addFiddFileMetadataSignature,
                                boolean addLogicalFileSignatures,
                                boolean addLogicalFileMetadataSignatures,
                                PublicKeySerializer publicKeySerializer,
                                SignerChecker signerChecker,

                                boolean addCrcsToFiddKey,
                                CrcCalculator crcCalculator
    ) {
        // TODO: implement

        // TODO: check compatibility with metadata section

        System.out.println("fiddPack " + fiddFileMetadata);
    }
}
