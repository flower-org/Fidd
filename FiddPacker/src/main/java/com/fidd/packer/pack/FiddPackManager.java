package com.fidd.packer.pack;

import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddfile.ImmutableFiddFileMetadata;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.fiddkey.ImmutableFiddKey;
import com.fidd.core.fiddkey.ImmutableSection;
import com.fidd.core.logicalfile.ImmutableLogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadataSerializer;
import com.fidd.core.metadata.ImmutableMetadataContainer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
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
import java.util.ArrayList;
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
            @Nullable String fiddFileAndFiddKeySignatureFormat
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
            fiddFileMetadataBuilder
                    .authorsPublicKeyFormat(checkNotNull(publicKeySerializer).name())
                    .authorsPublicKey(publicKeySerializer.serialize(authorsPublicKey));
        }

        if (fiddFileAndFiddKeySignatureFormat != null) {
            fiddFileMetadataBuilder
                    .authorsFiddFileSignatureFormat(fiddFileAndFiddKeySignatureFormat)
                    .authorsFiddKeyFileSignatureFormat(fiddFileAndFiddKeySignatureFormat);
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
                                MetadataContainerSerializer metadataContainerSerializer,
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
        // Fidd Key parts will be populated as we go
        FiddKey.Section fiddFileMetadataSection = null;
        List<FiddKey.Section> logicalFilesSections = new ArrayList<>();

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
                createFileAndKeySignatures ? signerChecker.name() : null
        );

        // 3. Form FiddFile
        try (FileChannel outputChannel = FileChannel.open(fiddFile.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)) {
            OutputStream outputStream = Channels.newOutputStream(outputChannel);
            long position = 0;

            // 3.1. Add first gap
            position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());

            // 3.2. Process all content directory files
            List<FilePathTuple> files = getDirectoryContents(originalDirectory);
            shuffle(files, randomGenerator.generator());

            // Random position in the file for FiddFile Metadata Section
            int fiddFileMetadataSectionPosition = randomGenerator.generator().nextInt(files.size()+1);

            for (int i = -1; i < files.toArray().length; i++) {
                // 3.2.1 Write Logical file
                if (i != -1) {
                    // 3.2.1.1 Encrypt and add LogicalFile
                    FilePathTuple file = files.get(i);
                    long logicalFileSectionOffset = position;
                    byte[] logicalFileSectionKey = encryptionAlgorithm.generateNewKeyData(randomGenerator);

                    LengthAndCrc logicalFileSectionLengthAndCrc =
                            addLogicalFileWithMetadata(originalDirectory, file.file(),
                                    outputStream, encryptionAlgorithm, logicalFileSectionKey,
                                    metadataContainerSerializer, logicalFileMetadataSerializer,
                                    addLogicalFileSignatures, addLogicalFileMetadataSignatures,
                                    signerChecker, authorsPrivateKey,
                                    addCrcsToFiddKey, crcCalculator);
                    position += logicalFileSectionLengthAndCrc.length();

                    // 3.2.2.2 Form corresponding Section descriptor for FiddKey
                    FiddKey.Section logicalFileSection = createFiddKeySection(logicalFileSectionOffset,
                            position - logicalFileSectionOffset, encryptionAlgorithm,
                            logicalFileSectionKey, addCrcsToFiddKey,
                            crcCalculator, logicalFileSectionLengthAndCrc.crc());
                    logicalFilesSections.add(logicalFileSection);

                    // 3.2.1.2 Add gap after LogicalFile Section
                    position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());
                }

                // 3.2.2 If we're at the right position, also write FiddFile Metadata
                if ((i+1) == fiddFileMetadataSectionPosition) {
                    // 3.2.2.1 Encrypt and add FiddFileMetadata
                    long fiddFileMetadataSectionOffset = position;
                    byte[] fiddFileMetadataSectionKey = encryptionAlgorithm.generateNewKeyData(randomGenerator);

                    LengthAndCrc fiddFileMetadataSectionLengthAndCrc =
                            addFiddFileMetadata(outputStream, fiddFileMetadata, fiddFileMetadataSerializer,
                                    metadataContainerSerializer,
                                    encryptionAlgorithm, fiddFileMetadataSectionKey, crcCalculator, addCrcsToFiddKey,
                                    addFiddFileMetadataSignature, authorsPrivateKey, signerChecker);
                    position += fiddFileMetadataSectionLengthAndCrc.length();

                    // 3.2.2.2 Form corresponding Section descriptor for FiddKey
                    fiddFileMetadataSection = createFiddKeySection(fiddFileMetadataSectionOffset,
                            position - fiddFileMetadataSectionOffset, encryptionAlgorithm,
                            fiddFileMetadataSectionKey, addCrcsToFiddKey,
                            crcCalculator, fiddFileMetadataSectionLengthAndCrc.crc());

                    // 3.2.2.3 Add gap after FiddFile Metadata Section
                    position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());
                }
            }
        }

        // 4. Form FiddKey file
        ImmutableFiddKey.Builder fiddKeyBuilder = ImmutableFiddKey.builder();
        shuffle(logicalFilesSections, randomGenerator.generator());
        fiddKeyBuilder.fiddFileMetadata(checkNotNull(fiddFileMetadataSection))
                .logicalFiles(logicalFilesSections);

        FiddKey fiddKey = fiddKeyBuilder.build();

        byte[] fiddKeyBytes = fiddKeySerializer.serialize(fiddKey);
        Files.write(fiddKeyFile.toPath(), fiddKeyBytes);

        // 5. File signatures
        if (createFileAndKeySignatures) {
            // 5.1 Form Fidd file signature
            try (FileChannel inputChannel = FileChannel.open(fiddFile.toPath(), StandardOpenOption.READ)) {
                InputStream inputFileStream = Channels.newInputStream(inputChannel);
                byte[] fiddFileSignatureBytes = signerChecker.signData(inputFileStream, authorsPrivateKey);
                Files.write(fiddFileSignature.toPath(), fiddFileSignatureBytes);
            }

            // 5.2 Form FiddKey file signature
            try (FileChannel inputChannel = FileChannel.open(fiddKeyFile.toPath(), StandardOpenOption.READ)) {
                InputStream inputFileStream = Channels.newInputStream(inputChannel);
                byte[] fiddKeyFileSignatureBytes = signerChecker.signData(inputFileStream, authorsPrivateKey);
                Files.write(fiddKeyFileSignature.toPath(), fiddKeyFileSignatureBytes);
            }
        }
    }

    public static FiddKey.Section createFiddKeySection(long sectionOffset, long sectionLength,
                                                       EncryptionAlgorithm encryptionAlgorithm, byte[] sectionKey,
                                                       boolean addCrcsToFiddKey, CrcCalculator crcCalculator,
                                                       @Nullable byte[] crc
                                                       ) {
        ImmutableSection.Builder sectionBuilder = ImmutableSection.builder()
                .sectionOffset(sectionOffset)
                .sectionLength(sectionLength);

        //TODO: What if there is no encryption - don't add those values? they are @Nullable
        //TODO: special name - UNENCRYPTED?
        if (encryptionAlgorithm != null) {
            sectionBuilder
                    .encryptionAlgorithm(encryptionAlgorithm.name())
                    .encryptionKeyData(sectionKey);
        }

        if (addCrcsToFiddKey) {
            sectionBuilder
                    .crcAlgorithm(crcCalculator.name())
                    .crc(checkNotNull(crc));
        }

        return sectionBuilder.build();
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

    private static LengthAndCrc addLogicalFileWithMetadata(File originalDirectory, File inputFile, OutputStream outputFileStream,
                                                       EncryptionAlgorithm encryptionAlgorithm, byte[] keyData,
                                                       MetadataContainerSerializer metadataContainerSerializer,
                                                       LogicalFileMetadataSerializer logicalFileMetadataSerializer,
                                                       boolean addLogicalFileSignatures,
                                                       boolean addLogicalFileMetadataSignatures,
                                                       SignerChecker signerChecker,
                                                       @Nullable PrivateKey authorsPrivateKey,
                                                       boolean addCrcsToFiddKey,
                                                       CrcCalculator crcCalculator
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

        // 2. Form Metadata Container
        MetadataContainer logicalFileMetadataContainer;
        {
            ImmutableMetadataContainer.Builder metadataContainerBuilder = ImmutableMetadataContainer.builder();
            metadataContainerBuilder
                    .metadataFormat(metadataContainerSerializer.name())
                    .metadata(metadataBytes);

            if (addLogicalFileMetadataSignatures) {
                byte[] logicalFileMetadataSignature = signerChecker.signData(metadataBytes, checkNotNull(authorsPrivateKey));

                metadataContainerBuilder
                        .signatureFormat(signerChecker.name())
                        .signature(logicalFileMetadataSignature);
            }

            logicalFileMetadataContainer = metadataContainerBuilder.build();
        }
        byte[] metadataContainerBytes = metadataContainerSerializer.serialize(logicalFileMetadataContainer);

        // 3. Append MetadataContainer and File to Fidd File (output file)
        EncryptionAlgorithm.CrcCallback crcCallback = null;
        if (addCrcsToFiddKey) {
            crcCallback = crcCalculator.newCrcCallback();
        }

        long length;
        try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
            InputStream inputFileStream = Channels.newInputStream(inputChannel);
            length = encryptionAlgorithm.encrypt(keyData,
                    List.of(new ByteArrayInputStream(metadataContainerBytes), inputFileStream), outputFileStream, crcCallback);
        }

        byte[] crc = null;
        if (addCrcsToFiddKey) {
            crc = checkNotNull(crcCallback).getCrc();
        }

        return LengthAndCrc.of(length, crc);
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

    private static LengthAndCrc addFiddFileMetadata(OutputStream outputStream, FiddFileMetadata fiddFileMetadata,
                                                       FiddFileMetadataSerializer fiddFileMetadataSerializer,
                                                       MetadataContainerSerializer metadataContainerSerializer,
                                                       EncryptionAlgorithm encryptionAlgorithm,
                                                       byte[] fiddFileMetadataSectionKey,
                                                       CrcCalculator crcCalculator,
                                                       boolean addCrcsToFiddKey,

                                                       boolean addFiddFileMetadataSignature,
                                                       @Nullable PrivateKey privateKey,
                                                       SignerChecker signerChecker

                                                       ) throws IOException {
        // 1. Serialize metadata
        byte[] fiddFileMetadataBytes = fiddFileMetadataSerializer.serialize(fiddFileMetadata);

        // 2. Form Metadata Container
        ImmutableMetadataContainer.Builder metadataContainerBuilder = ImmutableMetadataContainer.builder();
        metadataContainerBuilder
                .metadataFormat(fiddFileMetadataSerializer.name())
                .metadata(fiddFileMetadataBytes);

        if (addFiddFileMetadataSignature) {
            metadataContainerBuilder
                .signatureFormat(signerChecker.name())
                .signature(signerChecker.signData(fiddFileMetadataBytes, checkNotNull(privateKey)));
        }

        MetadataContainer metadataContainer = metadataContainerBuilder.build();

        byte[] fiddFileMetadataSectionBytes = metadataContainerSerializer.serialize(metadataContainer);

        // 3. Calculate Metadata Section CRC for FiddKey (if needed)
        byte[] crc = null;
        if (addCrcsToFiddKey) {
            crc = crcCalculator.calculateCrc(fiddFileMetadataSectionBytes);
        }

        // 4. Encrypt Metadata Section
        byte[] encryptedFiddFileMetadataSectionBytes = encryptionAlgorithm.encrypt(fiddFileMetadataSectionKey,
                fiddFileMetadataSectionBytes);

        // 5. Write Metadata Section to Fidd File
        outputStream.write(encryptedFiddFileMetadataSectionBytes);

        return LengthAndCrc.of(encryptedFiddFileMetadataSectionBytes.length, crc);
    }
}
