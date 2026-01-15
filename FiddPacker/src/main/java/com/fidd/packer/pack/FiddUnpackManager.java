package com.fidd.packer.pack;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.FiddKeyUtil;
import com.fidd.core.common.FiddSignature;
import com.fidd.core.common.LogicalFileMetadataUtil;
import com.fidd.core.common.ProgressiveCrc;
import com.fidd.core.common.SubFileInputStream;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.crc.ProgressiveCrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static com.fidd.core.common.FiddFileMetadataUtil.loadFiddFileMetadata;
import static com.google.common.base.Preconditions.checkNotNull;

public class FiddUnpackManager {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddUnpackManager.class);

    // TODO: hardcoding this to "BLOBS" for now
    final static String METADATA_CONTAINER_SERIALIZER_FORMAT = "BLOBS";

    public interface ProgressCallback {
        void log(String log);
        void warn(String log);
    }

    public enum PublicKeySource {
        MESSAGE_EMBEDDED,
        SUPPLIED_PARAMETER,
        MESSAGE_FALL_BACK_TO_PARAMETER
    }

    public static void fiddUnpackPost(BaseRepositories baseRepositories,

                                      FiddConnector fiddConnector,
                                      long messageNumber,

                                      File fiddFile,
                                      String fiddKeyFileName,
                                      byte[] fiddKeyBytes,
                                      List<File> fiddFileSignatures,
                                      List<File> fiddKeyFileSignatures,

                                      File contentFolder,

                                      boolean throwOnValidationFailure,
                                      boolean validateFiddFileAndFiddKey,
                                      boolean validateCrcsInFiddKey,
                                      boolean validateFiddFileMetadata,
                                      boolean validateLogicalFileMetadatas,
                                      boolean validateLogicalFiles,

                                      PublicKeySource publicKeySource,
                                      @Nullable X509Certificate currentCert,

                                      ProgressCallback progressCallback) throws IOException {
        progressCallback.log("Unpacking " + fiddFile.getAbsolutePath());

        // 1. Load Fidd.Key file (format detection)
        progressCallback.log("1. Loading FiddKey " + fiddKeyFileName);

        FiddKey fiddKey = FiddKeyUtil.loadFiddKeyFromBytes(baseRepositories, fiddKeyBytes);
        if (fiddKey == null) {
            warnAndMaybeThrow("Failed to deserialize FiddKey - can't proceed.", progressCallback, true);
        } else {
            progressCallback.log("Successfully deserialized FiddKey.");
        }

        // 2. Check CRC for Sections
        if (!validateCrcsInFiddKey) {
            progressCallback.warn("2. Section CRC validation (from FiddKey) not requested, omitting");
        } else {
            progressCallback.log("2. Validating Section CRC (from FiddKey)");
            boolean hasCrcs = hasCrcs(checkNotNull(fiddKey).fiddFileMetadata());
            for (FiddKey.Section logicalFileSection : fiddKey.logicalFiles()) {
                hasCrcs = hasCrcs || hasCrcs(logicalFileSection);
            }

            if (!hasCrcs) {
                progressCallback.warn("FiddKey doesn't contain CRC information");
            } else {
                validateCrcs(fiddFile, baseRepositories, 0, fiddKey.fiddFileMetadata(),
                    progressCallback, throwOnValidationFailure);
                for (int i = 0; i < fiddKey.logicalFiles().size(); i++) {
                    FiddKey.Section logicalFileSection = fiddKey.logicalFiles().get(i);
                    validateCrcs(fiddFile, baseRepositories, i+1, logicalFileSection,
                            progressCallback, throwOnValidationFailure);
                }
            }
        }

        // 3. Load FiddFileMetadata
        progressCallback.log("3. Loading FiddFileMetadata");

        Pair<FiddFileMetadata, MetadataContainer> fiddFileMetadataAndContainer = null;
        try {
            fiddFileMetadataAndContainer = loadFiddFileMetadata(baseRepositories, fiddConnector, messageNumber,
                    checkNotNull(fiddKey).fiddFileMetadata(), METADATA_CONTAINER_SERIALIZER_FORMAT);
        } catch (NotEnoughBytesException e) {
            warnAndMaybeThrow(StringUtils.defaultIfBlank(e.getMessage(), e.toString()), progressCallback, true);
        }

        FiddFileMetadata fiddFileMetadata = checkNotNull(fiddFileMetadataAndContainer).getLeft();
        MetadataContainer fiddFileMetadataContainer = fiddFileMetadataAndContainer.getRight();

        // 4. Try to get Public Key from FiddFileMetadata
        progressCallback.log("4. Looking for Public Key in FiddFileMetadata");
        X509Certificate messagePublicKey = null;
        if (fiddFileMetadata.authorsPublicKey() == null) {
            progressCallback.warn("FiddFileMetadata doesn't contain author's PublicKey");
        } else {
            String publicKeyFormat = fiddFileMetadata.authorsPublicKeyFormat();
            progressCallback.log("FiddFileMetadata contains author's PublicKey in format: " + publicKeyFormat);

            PublicKeySerializer publicKeySerializer = baseRepositories.publicKeyFormatRepo().get(publicKeyFormat);
            if (publicKeySerializer != null) {
                try {
                    messagePublicKey = publicKeySerializer.deserialize(fiddFileMetadata.authorsPublicKey());
                    progressCallback.log("PublicKey from FiddFileMetadata successfully deserialized.");
                } catch (Exception e) {
                    String errorText = "Failed to deserialize PublicKey: format " + publicKeyFormat;
                    progressCallback.warn(errorText);
                    LOGGER.warn(errorText, e);
                }
            } else {
                progressCallback.warn("Can't deserialize PublicKeyPublicKey format is not supported: " + publicKeyFormat);
            }
        }

        if (publicKeySource == PublicKeySource.SUPPLIED_PARAMETER) {
            String messageText = "UI Certificate will be used for signature validation due to PublicKeySource: `" + publicKeySource + "`";
            if (messagePublicKey != null) {
                progressCallback.warn("However, " + messageText);
            } else {
                progressCallback.log(messageText);
            }
        } else if (publicKeySource == PublicKeySource.MESSAGE_EMBEDDED) {
            currentCert = messagePublicKey;
            if (messagePublicKey != null) {
                progressCallback.log("PublicKey from FiddFileMetadata will be used for signature validation due to PublicKeySource: `" + publicKeySource + "`");
            } else {
                progressCallback.warn("PublicKey from FiddFileMetadata was not loaded. Signature validation will not be possible due to PublicKeySource: `" + publicKeySource + "`");
            }
        } else if (publicKeySource == PublicKeySource.MESSAGE_FALL_BACK_TO_PARAMETER) {
            if (messagePublicKey != null) {
                currentCert = messagePublicKey;
                progressCallback.log("PublicKey from FiddFileMetadata will be used for signature validation due to PublicKeySource: `" + publicKeySource + "`");
            } else {
                progressCallback.warn("PublicKey from FiddFileMetadata was not loaded. " +
                        "Falling back to UI Certificate for signature validation due to PublicKeySource: `" + publicKeySource + "`");
            }
        }

        // 5. Validate FiddFileMetadata Signatures
        if (!validateFiddFileMetadata) {
            progressCallback.warn("5. Validating FiddFileMetadata signatures not requested, omitting");
        } else {
            progressCallback.log("5. Validating FiddFileMetadata signatures");
            validateMetadataContainer(baseRepositories, fiddFileMetadataContainer, progressCallback, currentCert, throwOnValidationFailure);
        }

        // Create output subfolder
        File outputFolder = new File(contentFolder, fiddFileMetadata.postId());
        if (!outputFolder.exists()) {
            boolean created = outputFolder.mkdirs();
            if (!created) {
                warnAndMaybeThrow("Failed to create subfolder: " + outputFolder.getAbsolutePath(),
                        progressCallback, true);
            }
        } else {
            warnAndMaybeThrow("Output subfolder already exists: " + outputFolder.getAbsolutePath(),
                    progressCallback, true);
        }


        if (!validateFiddFileAndFiddKey) {
            progressCallback.warn("6. Validating Fidd file signatures not requested, omitting");
            progressCallback.warn("7. Validating Fidd.Key file signatures not requested, omitting");
        } else {
            // 6. Validate Fidd file signature
            progressCallback.log("6. Validating Fidd file signatures");
            validateFile(baseRepositories, fiddFile, fiddFileSignatures, fiddFileMetadata.authorsFiddFileSignatureFormats(),
                    progressCallback, currentCert, throwOnValidationFailure);

            // 7. Validate Fidd.Key signature
            progressCallback.log("7. Validating Fidd.Key file signatures");
            validateFile(baseRepositories, fiddKeyFileName, fiddKeyBytes, fiddKeyFileSignatures, fiddFileMetadata.authorsFiddKeyFileSignatureFormats(),
                    progressCallback, currentCert, throwOnValidationFailure);
        }

        // 8. Load LogicalFile Sections
        progressCallback.log("8. Loading and Materializing Logical Files");

        for (int i = 0; i < fiddKey.logicalFiles().size(); i++) {
            FiddKey.Section logicalFileSection = fiddKey.logicalFiles().get(i);
            validateAndMaterializeLogicalFile(i, baseRepositories, fiddConnector, messageNumber, logicalFileSection,
                    METADATA_CONTAINER_SERIALIZER_FORMAT, outputFolder, progressCallback, currentCert, throwOnValidationFailure,
                    validateLogicalFileMetadatas, validateLogicalFiles, true);
        }
    }

    private static void validateAndMaterializeLogicalFile(int logicalFileIndex, BaseRepositories baseRepositories,
                                                          FiddConnector fiddConnector,
                                                          long messageNumber, FiddKey.Section logicalFileSection,
                                                          String metadataContainerSerializerFormat,
                                                          File outputFolder, ProgressCallback progressCallback,
                                                          @Nullable X509Certificate publicKey,
                                                          boolean throwOnValidationFailure,
                                                          boolean validateLogicalFileMetadatas, boolean validateLogicalFiles, boolean materializeLogicalFiles) throws IOException {
        MetadataContainerSerializer metadataContainerSerializer =
                checkNotNull(baseRepositories.metadataContainerFormatRepo().get(metadataContainerSerializerFormat));

        progressCallback.log("8.1 Processing Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");

        String encryptionAlgorithmName = logicalFileSection.encryptionAlgorithm();
        progressCallback.log("Section Encryption Algorithm: " + encryptionAlgorithmName);
        EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(encryptionAlgorithmName);
        if (encryptionAlgorithm == null) {
            progressCallback.log("EncryptionAlgorithm " + encryptionAlgorithmName + " not supported - can't process Section #" +
                    (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");
        } else {
            progressCallback.log("8.2 Loading LogicalFileMetadata for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");

            LOGGER.info("Getting LogicalFileMetadata for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");
            Pair<LogicalFileMetadata, MetadataContainerSerializer.MetadataContainerAndLength> pair =
                LogicalFileMetadataUtil.getLogicalFileMetadata(baseRepositories, encryptionAlgorithm, fiddConnector, messageNumber,
                        logicalFileSection, metadataContainerSerializer, throwOnValidationFailure);

            if (pair != null) {
                if (!validateLogicalFileMetadatas) {
                    progressCallback.warn("8.3 Validating LogicalFileMetadata signatures not requested, omitting");
                } else {
                    progressCallback.log("8.3 Validating LogicalFileMetadata for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");
                    validateMetadataContainer(baseRepositories, pair.getRight().metadataContainer(), progressCallback,
                            publicKey, throwOnValidationFailure);
                }

                LogicalFileMetadata logicalFileMetadata = pair.getLeft();
                String logicalFileName = logicalFileMetadata.filePath();

                long logicalFileMetadataLengthBytes = pair.getRight().lengthBytes();
                if (!validateLogicalFiles) {
                    progressCallback.warn("8.4 Validating LogicalFiles not requested, omitting");
                } else {
                    progressCallback.log("8.4 Validating LogicalFile \"" + logicalFileMetadata.filePath() +
                            "\" for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");
                    //LogicalFile Validation
                    if (logicalFileMetadata.authorsFileSignatures() == null) {
                        progressCallback.log("LogicalFileMetadata for \"" + logicalFileMetadata.filePath() + "\" has no signatures");
                    } else {
                        for (int i = 0; i < logicalFileMetadata.authorsFileSignatures().size(); i++) {
                            FiddSignature authorsFileSignature = logicalFileMetadata.authorsFileSignatures().get(i);

                            try (InputStream logicalFileStream =
                                        encryptionAlgorithm.getDecryptedStream(logicalFileSection.encryptionKeyData(),
                                            fiddConnector.getFiddMessageChunk(messageNumber, logicalFileSection.sectionOffset(),
                                                 logicalFileSection.sectionLength()))) {
                                skipAll(logicalFileStream, logicalFileMetadataLengthBytes);

                                try (InputStream signatureStream = new ByteArrayInputStream(authorsFileSignature.bytes())) {
                                    validateFileSignature(i, logicalFileName, null,
                                            logicalFileStream, signatureStream,
                                            baseRepositories, authorsFileSignature.format(),
                                            publicKey,
                                            progressCallback, throwOnValidationFailure);
                                }
                            }
                        }
                    }

                    if (logicalFileMetadata.progressiveCrcs() == null) {
                        progressCallback.log("LogicalFileMetadata for \"" + logicalFileMetadata.filePath() + "\" has no progressiveCrcs");
                    } else {
                        for (int i = 0; i < logicalFileMetadata.progressiveCrcs().size(); i++) {
                            ProgressiveCrc progressiveCrc = logicalFileMetadata.progressiveCrcs().get(i);

                            try (InputStream logicalFileStream =
                                         encryptionAlgorithm.getDecryptedStream(logicalFileSection.encryptionKeyData(),
                                                 fiddConnector.getFiddMessageChunk(messageNumber, logicalFileSection.sectionOffset(),
                                                         logicalFileSection.sectionLength()))) {
                                skipAll(logicalFileStream, logicalFileMetadataLengthBytes);

                                try (InputStream progressiveCrcStream = new ByteArrayInputStream(progressiveCrc.bytes())) {
                                    validateFileProgressiveCrc(i, logicalFileName,
                                            logicalFileStream, progressiveCrcStream, progressiveCrc.progressiveCrcChunkSize(),
                                            baseRepositories, progressiveCrc.format(),
                                            progressCallback, throwOnValidationFailure);
                                }
                            }
                        }
                    }
                }

                if (!materializeLogicalFiles) {
                    progressCallback.warn("8.5 Materializing LogicalFiles not requested, omitting");
                } else {
                    progressCallback.log("8.5 Materializing LogicalFile \"" + logicalFileMetadata.filePath() +
                            "\" for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");

                    try (InputStream logicalFileStream =
                                 encryptionAlgorithm.getDecryptedStream(logicalFileSection.encryptionKeyData(),
                                         fiddConnector.getFiddMessageChunk(messageNumber, logicalFileSection.sectionOffset(),
                                                 logicalFileSection.sectionLength()))) {
                        skipAll(logicalFileStream, logicalFileMetadataLengthBytes);

                        File outputFile = new File(outputFolder, logicalFileMetadata.filePath());
                        // Create containing directories if needed
                        outputFile.getParentFile().mkdirs();
                        saveToFile(logicalFileStream, outputFile);

                        progressCallback.log("LogicalFile materialized \"" + outputFile.getAbsolutePath() + "\"");
                    }
                }
            }
        }
    }

    public static void saveToFile(InputStream logicalFileStream, File targetFile) throws IOException {
        try (OutputStream out = new FileOutputStream(targetFile)) {
            byte[] buffer = new byte[8192]; // 8 KB buffer
            int bytesRead;
            while ((bytesRead = logicalFileStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }
    }

    public static void skipAll(InputStream stream, long n) throws IOException {
        long remaining = n;
        while (remaining > 0) {
            long skipped = stream.skip(remaining);
            if (skipped <= 0) {
                // If skip() returns 0, try reading and discarding one byte
                if (stream.read() == -1) {
                    throw new EOFException("Reached end of stream before skipping " + n + " bytes");
                }
                skipped = 1;
            }
            remaining -= skipped;
        }
    }

    private static void validateFile(BaseRepositories baseRepositories, File dataFile, List<File> signatureFiles,
                                     List<String> signatureFormats, ProgressCallback progressCallback,
                                     @Nullable X509Certificate publicKey, boolean throwOnValidationFailure) throws IOException {
        // TODO: handle mismatch between number of signatureFiles and number of signatureFormats
        for (int i = 0; i < signatureFiles.size(); i++) {
            validateFileSignature(i, dataFile, signatureFiles.get(i),
                    baseRepositories, signatureFormats.get(i), publicKey,
                    progressCallback, throwOnValidationFailure);
        }
    }

    private static void validateFile(BaseRepositories baseRepositories, String dataFileName, byte[] dataBytes, List<File> signatureFiles,
                                     List<String> signatureFormats, ProgressCallback progressCallback,
                                     @Nullable X509Certificate publicKey, boolean throwOnValidationFailure) throws IOException {
        // TODO: handle mismatch between number of signatureFiles and number of signatureFormats
        for (int i = 0; i < signatureFiles.size(); i++) {
            validateFileSignature(i, dataFileName, dataBytes, signatureFiles.get(i),
                    baseRepositories, signatureFormats.get(i), publicKey,
                    progressCallback, throwOnValidationFailure);
        }
    }

    private static void validateFileSignature(int signatureNumber, File dataFile, File signatureFile,
                                              BaseRepositories baseRepositories, String signatureFormat,
                                              @Nullable X509Certificate publicKey,
                                              ProgressCallback progressCallback, boolean throwOnValidationFailure) throws IOException {
        try (FileInputStream signatureStream = new FileInputStream(signatureFile)) {
            try (FileInputStream dataStream = new FileInputStream(dataFile)) {
                validateFileSignature(signatureNumber, dataFile.getName(), signatureFile.getName(),
                        dataStream, signatureStream,
                        baseRepositories, signatureFormat, publicKey, progressCallback, throwOnValidationFailure);
            }
        }
    }

    private static void validateFileSignature(int signatureNumber, String dataFileName, byte[] dataBytes, File signatureFile,
                                              BaseRepositories baseRepositories, String signatureFormat,
                                              @Nullable X509Certificate publicKey,
                                              ProgressCallback progressCallback, boolean throwOnValidationFailure) throws IOException {
        try (FileInputStream signatureStream = new FileInputStream(signatureFile)) {
            try (ByteArrayInputStream dataStream = new ByteArrayInputStream(dataBytes)) {
                validateFileSignature(signatureNumber, dataFileName, signatureFile.getName(),
                        dataStream, signatureStream,
                        baseRepositories, signatureFormat, publicKey, progressCallback, throwOnValidationFailure);
            }
        }
    }

    private static void validateFileSignature(int signatureNumber,
                                              String dataFileName, @Nullable String signatureFileName,
                                              InputStream dataStream, InputStream signatureStream,
                                              BaseRepositories baseRepositories, String signatureFormat,
                                              @Nullable X509Certificate publicKey,
                                              ProgressCallback progressCallback, boolean throwOnValidationFailure) throws IOException {
        String signatureFileStr = (signatureFileName == null ? "" : "; signature file: " + signatureFileName);
        progressCallback.log("Validating file signature #" + signatureNumber + ": " + dataFileName + signatureFileStr);
        progressCallback.log("File signature format: " + signatureFormat);
        SignerChecker signerChecker = baseRepositories.signatureFormatRepo().get(signatureFormat);
        if (publicKey == null) {
            String errorMessage = "File signature #" + signatureNumber + " validation failed - PublicKey not specified. " +
                    dataFileName + signatureFileStr;
            if (signerChecker == null) {
                warnAndMaybeThrow(errorMessage, progressCallback, false);
            } else {
                warnAndMaybeThrow(errorMessage, progressCallback, throwOnValidationFailure);
                return;
            }
        }

        if (signerChecker == null) {
            warnAndMaybeThrow("File signature #" + signatureNumber + " validation failed - signature format " +
                    signatureFormat + " not supported. " + dataFileName + signatureFileStr,
                    progressCallback, throwOnValidationFailure);
        } else {
            byte[] signature = signatureStream.readAllBytes();
            boolean validationResult = signerChecker.verifySignature(dataStream, signature, checkNotNull(publicKey).getPublicKey());
            if (validationResult) {
                progressCallback.log("File signature #" + signatureNumber + " validation success: " + dataFileName + signatureFileStr);
            } else {
                warnAndMaybeThrow("File signature #" + signatureNumber + " validation failed: " + dataFileName + signatureFileStr,
                        progressCallback, throwOnValidationFailure);
            }
        }
    }

    private static void validateFileProgressiveCrc(int progressiveCrcNumber,
                                              String dataFileName,
                                              InputStream dataStream, InputStream progressiveCrcStream, long progressiveCrcChunkSize,
                                              BaseRepositories baseRepositories, String progressiveCrcFormat,
                                              ProgressCallback progressCallback, boolean throwOnValidationFailure) throws IOException {
        progressCallback.log("Validating file progressive CRC #" + progressiveCrcNumber + ": " + dataFileName);
        progressCallback.log("File progressive CRC format: " + progressiveCrcFormat);
        CrcCalculator crcCalculator = baseRepositories.crcCalculatorsRepo().get(progressiveCrcFormat);
        if (crcCalculator == null) {
            warnAndMaybeThrow("File progressive CRC #" + progressiveCrcNumber + " validation failed - CRC format " +
                            progressiveCrcFormat + " not supported. " + dataFileName,
                    progressCallback, throwOnValidationFailure);
        } else {
            byte[] progressiveCrc = progressiveCrcStream.readAllBytes();
            byte[] calcCrc = ProgressiveCrcCalculator.calculateProgressiveCrc(dataStream, progressiveCrcChunkSize, crcCalculator);

            boolean validationResult = Arrays.equals(progressiveCrc, calcCrc);
            if (validationResult) {
                progressCallback.log("File progressive CRC #" + progressiveCrcNumber + " validation success: " + dataFileName);
            } else {
                warnAndMaybeThrow("File progressive CRC #" + progressiveCrcNumber + " validation failed: " + dataFileName,
                        progressCallback, throwOnValidationFailure);
            }
        }
    }

    private static void validateMetadataContainer(BaseRepositories baseRepositories,
                                                  MetadataContainer metadataContainer,
                                                  ProgressCallback progressCallback,
                                                  @Nullable X509Certificate publicKey,
                                                  boolean throwOnValidationFailure) {
        byte[] metadataBytes = metadataContainer.metadata();
        if (hasSignatures(metadataContainer)) {
            for (int i = 0; i < checkNotNull(metadataContainer.signatures()).size(); i++) {
                FiddSignature fiddSignature = metadataContainer.signatures().get(i);
                String signatureFormat = fiddSignature.format();
                progressCallback.log("Validating signature #" + i + " format " + signatureFormat);
                SignerChecker signerChecker = baseRepositories.signatureFormatRepo().get(signatureFormat);
                if (signerChecker == null) {
                    warnAndMaybeThrow("Signature format not supported: " + signatureFormat,
                            progressCallback, throwOnValidationFailure);
                } else {
                    if (publicKey != null) {
                        boolean result = signerChecker.verifySignature(metadataBytes, fiddSignature.bytes(), publicKey.getPublicKey());
                        if (result) {
                            progressCallback.log("Validation success: signature #" + i);
                        } else {
                            String errorText = "Validation FAILED: signature #" + i;
                            warnAndMaybeThrow(errorText, progressCallback, throwOnValidationFailure);
                        }
                    } else {
                        String errorText = "Validation FAILED: PublicKey not specified";
                        warnAndMaybeThrow(errorText, progressCallback, throwOnValidationFailure);
                    }
                }
            }
        } else {
            progressCallback.warn("MetadataContainer has no signatures!");
        }
    }

    private static boolean hasCrcs(FiddKey.Section section) {
        return section.crcs() != null && !section.crcs().isEmpty();
    }

    private static boolean hasSignatures(MetadataContainer container) {
        return container.signatures() != null && !container.signatures().isEmpty();
    }

    private static void validateCrcs(File fiddFile,
                                       BaseRepositories baseRepositories,
                                       int sectionNumber,
                                       FiddKey.Section section,
                                       ProgressCallback progressCallback,
                                       boolean throwOnValidationFailure) throws IOException {
        if (!hasCrcs(section)) {
            progressCallback.warn("Section #" + sectionNumber + " has no CRCs to validate");
        } else {
            progressCallback.log("Validating CRCs for Section #" + sectionNumber);
            for (int i = 0; i < section.crcs().size(); i++) {
                FiddSignature crc = section.crcs().get(i);

                CrcCalculator crcCalculator = baseRepositories.crcCalculatorsRepo().get(crc.format());
                if (crcCalculator == null) {
                    String errorText = "Section #" + sectionNumber + " contains unsupported CRC Format: CRC #" + i + " / `" + crc.format() + "`. Can't validate!";
                    warnAndMaybeThrow(errorText, progressCallback, throwOnValidationFailure);
                } else {
                    boolean result = validateCrc(fiddFile, crcCalculator, section.sectionOffset(),
                            section.sectionLength(), crc.bytes());
                    if (!result) {
                        String errorText = "CRC Validation Failed: Section #" + sectionNumber + "; CRC #" + i + " / `" + crc.format() + "`";
                        warnAndMaybeThrow(errorText, progressCallback, throwOnValidationFailure);
                    } else {
                        progressCallback.log("Section #" + sectionNumber + "; CRC #" + i + " / `" + crc.format() + "` validated successfully!");
                    }
                }
            }
        }
    }

    private static void warnAndMaybeThrow(String errorText, ProgressCallback progressCallback,
                                          boolean throwException) {
        progressCallback.warn(errorText);
        if (throwException) {
            progressCallback.warn("UNPACK FAILED!");
            throw new RuntimeException(errorText);
        }
    }

    private static boolean validateCrc(File fiddFile,
                                       CrcCalculator crcCalculator,
                                       long sectionOffset,
                                       long sectionLength,
                                       byte[] crc) throws IOException {
        try (SubFileInputStream sectionStream = new SubFileInputStream(fiddFile, sectionOffset, sectionLength)) {
            byte[] calcCrc = crcCalculator.calculateCrc(sectionStream);
            return Arrays.equals(crc, calcCrc);
        }
    }
}
