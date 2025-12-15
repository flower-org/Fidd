package com.fidd.packer.pack;

import com.fidd.core.NamedEntry;
import com.fidd.core.common.FiddSignature;
import com.fidd.core.common.ProgressiveCrc;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.crc.ProgressiveCrcCalculator;
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
    public static final String DEFAULT_FIDD_SIGNATURE_EXT = ".sign";

    public static final String DEFAULT_FIDD_FILE_NAME_PREF = DEFAULT_FIDD_FILE_NAME + ".";
    public static final String DEFAULT_FIDD_KEY_FILE_NAME_PREF = DEFAULT_FIDD_KEY_FILE_NAME + ".";

    private static final int IO_BUFFER_SIZE = 8192; // 8 KB buffer size

    public static FiddFileMetadata createNewPostFiddFileMetadata(
            long messageNumber,
            String postId,
            @Nullable Pair<X509Certificate, PublicKeySerializer> publicKeySerializerAndKey,
            @Nullable List<String> fiddFileAndFiddKeySignatureFormats,
            boolean includeMessageCreationTime
    ) {
        // Build FiddFileMetadata
        ImmutableFiddFileMetadata.Builder fiddFileMetadataBuilder = ImmutableFiddFileMetadata.builder()
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

        if (fiddFileAndFiddKeySignatureFormats != null) {
            fiddFileMetadataBuilder
                    .authorsFiddFileSignatureFormats(fiddFileAndFiddKeySignatureFormats)
                    .authorsFiddKeyFileSignatureFormats(fiddFileAndFiddKeySignatureFormats);
        }

        if (includeMessageCreationTime) {
            long time = System.currentTimeMillis();
            fiddFileMetadataBuilder
                    .originalMessageCreationTime(time)
                    .messageCreationTime(time);
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

                                boolean includeMessageCreationTime,

                                boolean createFileAndKeySignatures,
                                boolean addFiddFileMetadataSignature,
                                boolean addLogicalFileSignatures,
                                boolean addLogicalFileMetadataSignatures,

                                boolean includePublicKey,
                                PublicKeySerializer publicKeySerializer,
                                List<SignerChecker> signerCheckers,

                                boolean addCrcsToFiddKey,
                                List<CrcCalculator> crcCalculators,

                                boolean addProgressiveCrcs,
                                long minProgressiveCrcFileSize,
                                long progressiveCrcChunkSize,
                                @Nullable List<CrcCalculator> progressiveCrcCalculators
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
        if (fiddFile.exists()) {
            throw new IllegalArgumentException("Cannot create file: Fidd File exists: " + fiddFile.getAbsolutePath());
        }
        if (fiddKeyFile.exists()) {
            throw new IllegalArgumentException("Cannot create file: Fidd Key File exists: " + fiddKeyFile.getAbsolutePath());
        }

        for (int i = 0; i < signerCheckers.size(); i++) {
            File fiddFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_FILE_NAME_PREF + i + DEFAULT_FIDD_SIGNATURE_EXT);
            File fiddKeyFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_KEY_FILE_NAME_PREF + i + DEFAULT_FIDD_SIGNATURE_EXT);
            if (createFileAndKeySignatures) {
                if (fiddFileSignature.exists()) {
                    throw new IllegalArgumentException("Cannot create file: Fidd File Signature exists: " + fiddFileSignature.getAbsolutePath());
                }
                if (fiddKeyFileSignature.exists()) {
                    throw new IllegalArgumentException("Cannot create file: Fidd Key File Signature exists: " + fiddKeyFileSignature.getAbsolutePath());
                }
            }
        }

        // 2. Create FiddFileMetadata for new post
        FiddFileMetadata fiddFileMetadata = createNewPostFiddFileMetadata(
                messageNumber,
                postId,
                includePublicKey ? Pair.of(authorsPublicKey, publicKeySerializer) : null,
                createFileAndKeySignatures ? signerCheckers.stream().map(NamedEntry::name).toList() : null,
                includeMessageCreationTime
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

                    LengthAndCrcs logicalFileSectionLengthAndCrc =
                            addLogicalFileWithMetadata(originalDirectory, file.file(),
                                    outputStream, encryptionAlgorithm, logicalFileSectionKey,
                                    metadataContainerSerializer, logicalFileMetadataSerializer,
                                    addLogicalFileSignatures, addLogicalFileMetadataSignatures,
                                    signerCheckers, authorsPrivateKey,
                                    addCrcsToFiddKey, crcCalculators,
                                    addProgressiveCrcs, minProgressiveCrcFileSize, progressiveCrcChunkSize, progressiveCrcCalculators);
                    position += logicalFileSectionLengthAndCrc.length();

                    // 3.2.2.2 Form corresponding Section descriptor for FiddKey
                    FiddKey.Section logicalFileSection = createFiddKeySection(logicalFileSectionOffset,
                            position - logicalFileSectionOffset, encryptionAlgorithm,
                            logicalFileSectionKey, addCrcsToFiddKey,
                            crcCalculators, logicalFileSectionLengthAndCrc.crcs());
                    logicalFilesSections.add(logicalFileSection);

                    // 3.2.1.2 Add gap after LogicalFile Section
                    position += appendGap(outputStream, minGapSize, maxGapSize, randomGenerator.generator());
                }

                // 3.2.2 If we're at the right position, also write FiddFile Metadata
                if ((i+1) == fiddFileMetadataSectionPosition) {
                    // 3.2.2.1 Encrypt and add FiddFileMetadata
                    long fiddFileMetadataSectionOffset = position;
                    byte[] fiddFileMetadataSectionKey = encryptionAlgorithm.generateNewKeyData(randomGenerator);

                    LengthAndCrcs fiddFileMetadataSectionLengthAndCrc =
                            addFiddFileMetadata(outputStream, fiddFileMetadata, fiddFileMetadataSerializer,
                                    metadataContainerSerializer,
                                    encryptionAlgorithm, fiddFileMetadataSectionKey, crcCalculators, addCrcsToFiddKey,
                                    addFiddFileMetadataSignature, authorsPrivateKey, signerCheckers);
                    position += fiddFileMetadataSectionLengthAndCrc.length();

                    // 3.2.2.2 Form corresponding Section descriptor for FiddKey
                    fiddFileMetadataSection = createFiddKeySection(fiddFileMetadataSectionOffset,
                            position - fiddFileMetadataSectionOffset, encryptionAlgorithm,
                            fiddFileMetadataSectionKey, addCrcsToFiddKey,
                            crcCalculators, fiddFileMetadataSectionLengthAndCrc.crcs());

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
            for (int i = 0; i < signerCheckers.size(); i++) {
                SignerChecker signerChecker = signerCheckers.get(i);

                File fiddFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_FILE_NAME + "." + i + DEFAULT_FIDD_SIGNATURE_EXT);
                File fiddKeyFileSignature = new File(packedContentDirectory, DEFAULT_FIDD_KEY_FILE_NAME + "." + i + DEFAULT_FIDD_SIGNATURE_EXT);

                try (FileChannel inputChannel = FileChannel.open(fiddFile.toPath(), StandardOpenOption.READ)) {
                    InputStream inputFileStream = Channels.newInputStream(inputChannel);
                    byte[] fiddFileSignatureBytes = signerChecker.signData(inputFileStream, authorsPrivateKey);
                    Files.write(fiddFileSignature.toPath(), fiddFileSignatureBytes);
                }
                try (FileChannel inputChannel = FileChannel.open(fiddKeyFile.toPath(), StandardOpenOption.READ)) {
                    InputStream inputFileStream = Channels.newInputStream(inputChannel);
                    byte[] fiddKeyFileSignatureBytes = signerChecker.signData(inputFileStream, authorsPrivateKey);
                    Files.write(fiddKeyFileSignature.toPath(), fiddKeyFileSignatureBytes);
                }
            }
        }
    }

    public static FiddKey.Section createFiddKeySection(long sectionOffset, long sectionLength,
                                                       EncryptionAlgorithm encryptionAlgorithm, byte[] sectionKey,
                                                       boolean addCrcsToFiddKey, List<CrcCalculator> crcCalculators,
                                                       @Nullable List<byte[]> crcs
                                                       ) {
        ImmutableSection.Builder sectionBuilder = ImmutableSection.builder()
                .sectionOffset(sectionOffset)
                .sectionLength(sectionLength);

        // If encryption algorithm is not specified it means the data is unencrypted
        if (!encryptionAlgorithm.name().equals(EncryptionAlgorithm.UNENCRYPTED)) {
            sectionBuilder
                    .encryptionAlgorithm(encryptionAlgorithm.name())
                    .encryptionKeyData(sectionKey);
        }

        if (addCrcsToFiddKey) {
            List<FiddSignature> crcList = new ArrayList<>();
            for (int i = 0; i < crcCalculators.size(); i++) {
                CrcCalculator crcCalculator = crcCalculators.get(i);
                byte[] crc = checkNotNull(crcs).get(i);
                crcList.add(FiddSignature.of(crcCalculator.name(), crc));
            }

            sectionBuilder.crcs(crcList);
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

    private static LengthAndCrcs addLogicalFileWithMetadata(File originalDirectory, File inputFile, OutputStream outputFileStream,
                                                            EncryptionAlgorithm encryptionAlgorithm, byte[] keyData,
                                                            MetadataContainerSerializer metadataContainerSerializer,
                                                            LogicalFileMetadataSerializer logicalFileMetadataSerializer,
                                                            boolean addLogicalFileSignatures,
                                                            boolean addLogicalFileMetadataSignatures,
                                                            List<SignerChecker> signerCheckers,
                                                            @Nullable PrivateKey authorsPrivateKey,
                                                            boolean addCrcsToFiddKey,
                                                            List<CrcCalculator> crcCalculators,

                                                            boolean addProgressiveCrcs,
                                                            long minProgressiveCrcFileSize,
                                                            long progressiveCrcChunkSize,
                                                            @Nullable List<CrcCalculator> progressiveCrcCalculators
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
                for (SignerChecker signerChecker : signerCheckers) {
                    List<FiddSignature> signatures = new ArrayList<>();
                    try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
                        InputStream inputFileStream = Channels.newInputStream(inputChannel);
                        byte[] inputFileSignature = signerChecker.signData(inputFileStream, checkNotNull(authorsPrivateKey));

                        signatures.add(FiddSignature.of(signerChecker.name(), inputFileSignature));
                    }

                    logicalFileMetadataBuilder.authorsFileSignatures(signatures);
                }
            }

            if (addProgressiveCrcs && inputFile.length() >= minProgressiveCrcFileSize) {
                List<ProgressiveCrc> progressiveCrcs = new ArrayList<>();
                for (CrcCalculator progressiveCrcCalculator : checkNotNull(progressiveCrcCalculators)) {
                    try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
                        InputStream inputFileStream = Channels.newInputStream(inputChannel);
                        byte[] progressiveCrc = ProgressiveCrcCalculator.calculateProgressiveCrc(inputFileStream, progressiveCrcChunkSize, progressiveCrcCalculator);

                        progressiveCrcs.add(ProgressiveCrc.of(progressiveCrcCalculator.name(), progressiveCrc, progressiveCrcChunkSize));
                    }

                    logicalFileMetadataBuilder.progressiveCrcs(progressiveCrcs);
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
                    .metadataFormat(logicalFileMetadataSerializer.name())
                    .metadata(metadataBytes);

            if (addLogicalFileMetadataSignatures) {
                List<FiddSignature> signatures = new ArrayList<>();
                for (SignerChecker signerChecker : signerCheckers) {
                    byte[] logicalFileMetadataSignature = signerChecker.signData(metadataBytes, checkNotNull(authorsPrivateKey));
                    signatures.add(FiddSignature.of(signerChecker.name(), logicalFileMetadataSignature));
                }

                metadataContainerBuilder.signatures(signatures);
            }

            logicalFileMetadataContainer = metadataContainerBuilder.build();
        }
        byte[] metadataContainerBytes = metadataContainerSerializer.serialize(logicalFileMetadataContainer);

        // 3. Append MetadataContainer and File to Fidd File (output file)
        List<EncryptionAlgorithm.CrcCallback> crcCallbacks = null;
        if (addCrcsToFiddKey) {
            crcCallbacks = new ArrayList<>();
            for (CrcCalculator crcCalculator : crcCalculators) {
                crcCallbacks.add(crcCalculator.newCrcCallback());
            }
        }

        long length;
        try (FileChannel inputChannel = FileChannel.open(inputFile.toPath(), StandardOpenOption.READ)) {
            InputStream inputFileStream = Channels.newInputStream(inputChannel);
            length = encryptionAlgorithm.encrypt(keyData,
                    List.of(new ByteArrayInputStream(metadataContainerBytes), inputFileStream), outputFileStream, crcCallbacks);
        }

        List<byte[]> crcs = null;
        if (addCrcsToFiddKey) {
            crcs = new ArrayList<>();
            for (EncryptionAlgorithm.CrcCallback crcCallback : checkNotNull(crcCallbacks)) {
                byte[] crc = crcCallback.getCrc();
                crcs.add(crc);
            }
        }

        return LengthAndCrcs.of(length, crcs);
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

    private static LengthAndCrcs addFiddFileMetadata(OutputStream outputStream, FiddFileMetadata fiddFileMetadata,
                                                     FiddFileMetadataSerializer fiddFileMetadataSerializer,
                                                     MetadataContainerSerializer metadataContainerSerializer,
                                                     EncryptionAlgorithm encryptionAlgorithm,
                                                     byte[] fiddFileMetadataSectionKey,
                                                     List<CrcCalculator> crcCalculators,
                                                     boolean addCrcsToFiddKey,

                                                     boolean addFiddFileMetadataSignature,
                                                     @Nullable PrivateKey privateKey,
                                                     List<SignerChecker> signerCheckers

                                                       ) throws IOException {
        // 1. Serialize metadata
        byte[] fiddFileMetadataBytes = fiddFileMetadataSerializer.serialize(fiddFileMetadata);

        // 2. Form Metadata Container
        ImmutableMetadataContainer.Builder metadataContainerBuilder = ImmutableMetadataContainer.builder();
        metadataContainerBuilder
                .metadataFormat(fiddFileMetadataSerializer.name())
                .metadata(fiddFileMetadataBytes);

        if (addFiddFileMetadataSignature) {
            List<FiddSignature> signatures = new ArrayList<>();
            for (SignerChecker signerChecker : signerCheckers) {
                signatures.add(FiddSignature.of(signerChecker.name(),
                        signerChecker.signData(fiddFileMetadataBytes, checkNotNull(privateKey))));
            }

            metadataContainerBuilder.signatures(signatures);
        }

        MetadataContainer metadataContainer = metadataContainerBuilder.build();

        byte[] fiddFileMetadataSectionBytes = metadataContainerSerializer.serialize(metadataContainer);

        // 3. Encrypt Metadata Section
        byte[] encryptedFiddFileMetadataSectionBytes = encryptionAlgorithm.encrypt(fiddFileMetadataSectionKey,
                fiddFileMetadataSectionBytes);

        // 4. Calculate Metadata Section CRC for FiddKey (if needed)
        List<byte[]> crcs = null;
        if (addCrcsToFiddKey) {
            crcs = new ArrayList<>();
            for (CrcCalculator crcCalculator : crcCalculators) {
                byte[] crc = crcCalculator.calculateCrc(encryptedFiddFileMetadataSectionBytes);
                crcs.add(crc);
            }
        }

        // 5. Write Metadata Section to Fidd File
        outputStream.write(encryptedFiddFileMetadataSectionBytes);

        return LengthAndCrcs.of(encryptedFiddFileMetadataSectionBytes.length, crcs);
    }
}
