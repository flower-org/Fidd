package com.fidd.packer.pack;

import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.ImmutableFiddFileMetadata;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.logicalfile.ImmutableLogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.ImmutableMetadataSection;
import com.fidd.core.metadata.MetadataSection;
import com.fidd.core.metadata.MetadataSectionSerializer;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import com.fidd.core.random.RandomGeneratorType;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.List;
import java.util.random.RandomGenerator;

import static com.fidd.packer.pack.DirectoryReader.getDirectoryContents;
import static com.google.common.base.Preconditions.checkNotNull;

public class FiddPackManager {
    public static final String DEFAULT_FIDD_FILE_NAME = "message.fidd";
    public static final String DEFAULT_FIDD_KEY_FILE_NAME = "fidd.key";
    public static final String DEFAULT_FIDD_FILE_SIGNATURE_FILE_NAME = DEFAULT_FIDD_FILE_NAME + ".sign";
    public static final String DEFAULT_FIDD_KEY_FILE_SIGNATURE_FILE_NAME = DEFAULT_FIDD_KEY_FILE_NAME + ".sign";

    private static final int IO_BUFFER_SIZE = 8192; // 8 KB buffer size

    public static FiddFileMetadata createNewPostFiddFileMetadata(
            long messageNumber,
            String postId,
            String logicalFileMetadataFormat,
            @Nullable Pair<X509Certificate, PublicKeySerializer> publicKeySerializerAndKey,
            @Nullable String fiddFileMetadataSignatureFormat
    ) {
        // Build FiddFileMetadata
        ImmutableFiddFileMetadata.Builder fiddFileMetadataBuilder = ImmutableFiddFileMetadata.builder()
                .logicalFileMetadataFormatVersion(logicalFileMetadataFormat)
                .messageNumber(messageNumber)
                .originalMessageNumber(messageNumber)
                .postId(postId)
                .versionNumber(1)
                .isNewOrSquash(true)
                .isDelete(false);

        if (publicKeySerializerAndKey != null) {
            X509Certificate authorsPublicKey = publicKeySerializerAndKey.getKey();
            PublicKeySerializer publicKeySerializer = publicKeySerializerAndKey.getValue();
            fiddFileMetadataBuilder.authorsPublicKeyFormat(checkNotNull(publicKeySerializer).name())
                    .authorsPublicKey(publicKeySerializer.serialize(authorsPublicKey));
        }

        return fiddFileMetadataBuilder.build();
    }

    public static void fiddPackNewPost(File originalDirectory,
                                File packedContentDirectory,
                                @Nullable X509Certificate authorsPublicKey,
                                @Nullable PrivateKey authorsPrivateKey,
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
    ) throws IOException {
        // 0. Sanity checks
        if (minGapSize > maxGapSize) {
            throw new IllegalArgumentException("`minGapSize` can't be larger than `maxGapSize`: " + minGapSize + " > " + maxGapSize);
        }

        // 1. Check output directory
        File fiddFile = new File(packedContentDirectory, DEFAULT_FIDD_FILE_NAME);
        File fiddKeyFile = new File(packedContentDirectory, DEFAULT_FIDD_KEY_FILE_NAME);
        File fiddFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_FILE_SIGNATURE_FILE_NAME);
        File fiddKeyFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_KEY_FILE_SIGNATURE_FILE_NAME);
        if (fiddFile.exists()) {
            throw new IllegalArgumentException("Cannot create file: Fidd File exists: " + fiddFile.getAbsolutePath());
        }
        if (fiddKeyFile.exists()) {
            throw new IllegalArgumentException("Cannot create file: Fidd Key File exists: " + fiddKeyFile.getAbsolutePath());
        }
        if (createFileAndKeySignatures) {
            if (fiddFileSignature.exists()) {
                throw new IllegalArgumentException("Cannot create file: Fidd File Signature exists: " + fiddFileSignature.getAbsolutePath());
            }
            if (fiddKeyFileSignature.exists()) {
                throw new IllegalArgumentException("Cannot create file: Fidd Key File Signature exists: " + fiddKeyFileSignature.getAbsolutePath());
            }
        }

        // 2. Create FiddFileMetadata for new post
        FiddFileMetadata fiddFileMetadata = createNewPostFiddFileMetadata(
                messageNumber,
                postId,
                logicalFileMetadataSerializer.name(),
                (createFileAndKeySignatures || addFiddFileMetadataSignature || addLogicalFileSignatures || addLogicalFileMetadataSignatures)
                        ? Pair.of(authorsPublicKey, publicKeySerializer) : null,
                addFiddFileMetadataSignature ? signerChecker.name() : null
        );

        // 3. Form FiddFile
        try (FileChannel outputChannel = FileChannel.open(fiddFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            OutputStream outputStream = Channels.newOutputStream(outputChannel);
            long position = 0;

            // 3.1. Add initial gap
            position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());

            // 3.2. Add FiddFileMetadata
            //position += appendFiddFileMetadata(...);

            // 3.3. Add gap after FiddFileMetadata
            position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());

            // 3.4. Process all content directory files
            List<FilePathTuple> files = getDirectoryContents(originalDirectory);
            shuffle(files, randomGenerator.generator());

            for (FilePathTuple file : files) {
                byte[] sectionKey = encryptionAlgorithm.generateNewKeyData();

                // 3.4.1. Encrypt and add LogicalFile
                position += addLogicalFileWithMetadata(originalDirectory, file.file(),
                        outputStream, encryptionAlgorithm, sectionKey,
                        metadataSectionSerializer, logicalFileMetadataSerializer,
                        addLogicalFileSignatures, addLogicalFileMetadataSignatures,
                        signerChecker, authorsPrivateKey);

                // 3.4.2. Add gap after LogicalFile
                position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());
            }
        }

        // 4. Form FiddKey file
        // TODO: implement

        // 5. Form Fidd file signature
        // TODO: implement

        // 6. Form FiddKey file signature
        // TODO: implement
    }

    public static <T> void shuffle(List<T> list, RandomGenerator randomGenerator) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = randomGenerator.nextInt(i + 1);
            Collections.swap(list, i, j);
        }
    }

    public static long randomLongBetween(long minGapSize, long maxGapSize, RandomGenerator randomGenerator) {
        return minGapSize + randomGenerator.nextLong(maxGapSize - minGapSize + 1);
    }

    public static long appendGap(OutputStream outputChannelStream, long minGapSize, long maxGapSize, RandomGenerator randomGenerator) throws IOException {
        if (maxGapSize == 0) { return 0; }
        final long gapSize = randomLongBetween(minGapSize, maxGapSize, randomGenerator);
        final int bufferSize = IO_BUFFER_SIZE;

        ByteBuffer buffer = ByteBuffer.allocate(bufferSize);

        long remaining = gapSize;
        while (remaining > 0) {
            buffer.clear();

            // Fill buffer with random bytes
            int bytesToWrite = (int) Math.min(bufferSize, remaining);
            byte[] randomBytes = new byte[bytesToWrite];
            randomGenerator.nextBytes(randomBytes);

            buffer.put(randomBytes);
            buffer.flip();

            outputChannelStream.write(randomBytes);

            remaining -= bytesToWrite;
        }

        return gapSize;
    }

    private static long addLogicalFileWithMetadata(File originalDirectory, File inputFile, OutputStream outputFileStream,
                                                       EncryptionAlgorithm encryptionAlgorithm, byte[] keyData,
                                                       MetadataSectionSerializer metadataSectionSerializer,
                                                       LogicalFileMetadataSerializer logicalFileMetadataSerializer,
                                                       boolean addLogicalFileSignatures,
                                                       boolean addLogicalFileMetadataSignatures,
                                                       SignerChecker signerChecker,
                                                       @Nullable PrivateKey authorsPrivateKey
                                                   ) throws IOException {
        // 1. Form Logical file metadata
        LogicalFileMetadata logicalFileMetadata;
        {
            ImmutableLogicalFileMetadata.Builder logicalFileMetadataBuilder = ImmutableLogicalFileMetadata.builder();

            Path relativePath = originalDirectory.toPath().relativize(inputFile.toPath());
            logicalFileMetadataBuilder
                    .updateType(LogicalFileMetadata.FiddUpdateType.CREATE_OVERRIDE)
                    .filePath(relativePath.toString());//TODO: test relative path correctness

            Long createdAt = createdAt(inputFile);
            Long updatedAt = updatedAt(inputFile);
            if (createdAt != null) {
                logicalFileMetadataBuilder.createdAt(createdAt);
            }
            if (updatedAt != null) {
                logicalFileMetadataBuilder.updatedAt(updatedAt);
            }

            if (addLogicalFileSignatures) {
                try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
                    InputStream inputFileStream = Channels.newInputStream(inputChannel);
                    byte[] inputFileSignature = signerChecker.signData(inputFileStream, checkNotNull(authorsPrivateKey));

                    logicalFileMetadataBuilder
                        .authorsFileSignatureFormat(signerChecker.name())
                        .authorsFileSignature(inputFileSignature);
                }
            }
            logicalFileMetadata = logicalFileMetadataBuilder.build();
        }
        byte[] metadataBytes = logicalFileMetadataSerializer.serialize(logicalFileMetadata);

        // 2. Form Metadata Section
        MetadataSection logicalFileMetadataSection;
        {
            ImmutableMetadataSection.Builder metadataSectionBuilder = ImmutableMetadataSection.builder();
            metadataSectionBuilder
                    .metadataFormat(metadataSectionSerializer.name())
                    .metadata();

            if (addLogicalFileMetadataSignatures) {
                byte[] logicalFileMetadataSignature = signerChecker.signData(metadataBytes, checkNotNull(authorsPrivateKey));

                metadataSectionBuilder
                        .signatureFormat(signerChecker.name())
                        .signature(logicalFileMetadataSignature);
            }

            logicalFileMetadataSection = metadataSectionBuilder.build();
        }
        byte[] metadataSectionBytes = metadataSectionSerializer.serialize(logicalFileMetadataSection);

        // 3. Append MetadataSection and File to Fidd File (output file)
        try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
            InputStream inputFileStream = Channels.newInputStream(inputChannel);
            return encryptionAlgorithm.encrypt(keyData,
                    List.of(new ByteArrayInputStream(metadataSectionBytes), inputFileStream), outputFileStream);
        }
    }

    @Nullable
    public static Long createdAt(File file) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
            return attrs.creationTime().toMillis();
        } catch (IOException e) {
            return null; // could not read attributes
        }
    }

    @Nullable
    public static Long updatedAt(File file) {
        // File.lastModified() returns 0 if file does not exist
        long lastModified = file.lastModified();
        return lastModified == 0 ? null : lastModified;
    }
}
