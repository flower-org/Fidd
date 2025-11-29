package com.fidd.packer.pack;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.common.FiddSignature;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.encryption.EncryptionAlgorithm;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddfile.FiddFileMetadataSerializer;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.core.metadata.NotEnoughBytesException;
import com.fidd.core.pki.PublicKeySerializer;
import com.fidd.core.pki.SignerChecker;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddUnpackManager {
    final static Logger LOGGER = LoggerFactory.getLogger(FiddUnpackManager.class);

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

                                      File fiddFile,
                                      File fiddKeyFile,
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
        // TODO: hardcoding this to "BLOBS" for now
        MetadataContainerSerializer metadataContainerSerializer =
                checkNotNull(baseRepositories.metadataContainerFormatRepo().get("BLOBS"));

        progressCallback.log("Unpacking " + fiddFile.getAbsolutePath());

        // 1. Load Fidd.Key file (format detection)
        progressCallback.log("1. Loading FiddKey " + fiddKeyFile.getAbsolutePath());
        byte[] fiddKeyBytes = Files.readAllBytes(fiddKeyFile.toPath());

        FiddKey fiddKey = null;
        Repository<FiddKeySerializer> fiddKeyFormatRepo = baseRepositories.fiddKeyFormatRepo();
        progressCallback.log("Detecting FiddKey format.");
        for (String fiddKeyFormat : fiddKeyFormatRepo.listEntryNames()) {
            FiddKeySerializer serializer = fiddKeyFormatRepo.get(fiddKeyFormat);
            try {
                fiddKey = serializer.deserialize(fiddKeyBytes);
                progressCallback.log("FiddKey format: `" + fiddKeyFormat + "` - successfully deserialized FiddKey.");
                break;
            } catch (Exception e) {
                progressCallback.log("FiddKey format: `" + fiddKeyFormat + "` - failed to deserialize FiddKey.");
            }
        }
        if (fiddKey == null) {
            warnAndMaybeThrow("Failed to deserialize FiddKey - can't proceed.", progressCallback, true);
        }

        // 2. Check CRC for Sections
        if (!validateCrcsInFiddKey) {
            progressCallback.warn("2. Section CRC validation (from FiddKey) not requested");
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

        Pair<FiddFileMetadata, MetadataContainer> fiddFileMetadataAndContainer =
                loadFiddFileMetadata(fiddFile, baseRepositories, checkNotNull(fiddKey).fiddFileMetadata(),
                    metadataContainerSerializer, progressCallback);

        FiddFileMetadata fiddFileMetadata = fiddFileMetadataAndContainer.getLeft();
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
        progressCallback.log("5. Validating FiddFileMetadata signatures");
        validateMetadataContainer(baseRepositories, fiddFileMetadataContainer, progressCallback, currentCert, throwOnValidationFailure);

        // 6. Validate Fidd file signature
        progressCallback.log("6. Validating Fidd file signatures");
        validateFile(baseRepositories, fiddFile, fiddFileSignatures, fiddFileMetadata.authorsFiddFileSignatureFormats(),
                progressCallback, currentCert, throwOnValidationFailure);

        // 7. Validate Fidd.Key signature
        progressCallback.log("7. Validating Fidd.Key file signatures");
        validateFile(baseRepositories, fiddKeyFile, fiddKeyFileSignatures, fiddFileMetadata.authorsFiddKeyFileSignatureFormats(),
                progressCallback, currentCert, throwOnValidationFailure);

        // 8. Load LogicalFile Sections
        progressCallback.log("8. Loading and Materializing Logical Files");
        for (int i = 0; i < fiddKey.logicalFiles().size(); i++) {
            FiddKey.Section logicalFileSection = fiddKey.logicalFiles().get(i);
            //validateAndMaterializeLogicalFile(i, baseRepositories, fiddFile, logicalFileSection, contentFolder, progressCallback);
        }
    }

    private static void validateAndMaterializeLogicalFile(int logicalFileIndex, BaseRepositories baseRepositories,
                                                          File fiddFile, FiddKey.Section logicalFileSection,
                                                          File contentFolder, ProgressCallback progressCallback) throws IOException {
        progressCallback.log("Processing Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");

        String encryptionAlgorithmName = logicalFileSection.encryptionAlgorithm();
        progressCallback.log("Section Encryption Algorithm: " + encryptionAlgorithmName);
        EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(encryptionAlgorithmName);
        if (encryptionAlgorithm == null) {
            progressCallback.log("EncryptionAlgorithm " + encryptionAlgorithmName + " not supported - can't process Section #" +
                    (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");
        } else {
            getLogicalFileMetadata(logicalFileIndex, baseRepositories, encryptionAlgorithm, fiddFile, logicalFileSection,
                    contentFolder, progressCallback);

            //
        }
    }

    private static void getLogicalFileMetadata(int logicalFileIndex, BaseRepositories baseRepositories,
                                               EncryptionAlgorithm encryptionAlgorithm, File fiddFile,
                                               FiddKey.Section logicalFileSection, File contentFolder,
                                               ProgressCallback progressCallback) throws IOException {
        progressCallback.log("Getting LogicalFileMetadata for Section #" + (logicalFileIndex+1) + " (Logical File #" + logicalFileIndex + ")");

        int bufferIncrement = 48;
        byte[] loadBuffer = new byte[bufferIncrement];
        byte[] mdBuffer = new byte[bufferIncrement];
        int mdBufferPos = 0; // current write position

        try (SubFileInputStream sectionInputStream =
                     new SubFileInputStream(fiddFile, logicalFileSection.sectionOffset(), logicalFileSection.sectionLength())) {
            int bytesRead;
            while ((bytesRead = sectionInputStream.read(loadBuffer)) != -1) {
                // Ensure mdBuffer has enough space
                if (mdBufferPos + bytesRead > mdBuffer.length) {
                    // Grow mdBuffer
                    int newSize = Math.max(mdBuffer.length + bufferIncrement, mdBufferPos + bytesRead);
                    byte[] newBuffer = new byte[newSize];
                    System.arraycopy(mdBuffer, 0, newBuffer, 0, mdBufferPos);
                    mdBuffer = newBuffer;
                }

                // Copy from loadBuffer into mdBuffer
                System.arraycopy(loadBuffer, 0, mdBuffer, mdBufferPos, bytesRead);
                mdBufferPos += bytesRead;

            }
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

    private static void validateFileSignature(int signatureNumber, File dataFile, File signatureFile,
                                              BaseRepositories baseRepositories, String signatureFormat,
                                              @Nullable X509Certificate publicKey,
                                              ProgressCallback progressCallback, boolean throwOnValidationFailure) throws IOException {
        progressCallback.log("Validating file signature #" + signatureNumber + ": " + dataFile.getName() + "; signature file: " + signatureFile.getName());
        progressCallback.log("File signature format: " + signatureFormat);
        SignerChecker signerChecker = baseRepositories.signatureFormatRepo().get(signatureFormat);
        if (publicKey == null) {
            String errorMessage = "File signature #" + signatureNumber + " validation failed - PublicKey not specified. " +
                    dataFile.getName() + "; signature file: " + signatureFile.getName();
            if (signerChecker == null) {
                warnAndMaybeThrow(errorMessage, progressCallback, false);
            } else {
                warnAndMaybeThrow(errorMessage, progressCallback, throwOnValidationFailure);
                return;
            }
        }

        if (signerChecker == null) {
            warnAndMaybeThrow("File signature #" + signatureNumber + " validation failed - signature format " +
                    signatureFormat + " not supported. " + dataFile.getName() + "; signature file: " + signatureFile.getName(),
                    progressCallback, throwOnValidationFailure);
        } else {
            try (FileInputStream signatureStream = new FileInputStream(signatureFile)) {
                byte[] signature = signatureStream.readAllBytes();
                try (FileInputStream dataStream = new FileInputStream(dataFile)) {
                    boolean validationResult = signerChecker.verifySignature(dataStream, signature, checkNotNull(publicKey).getPublicKey());
                    if (validationResult) {
                        progressCallback.log("File signature #" + signatureNumber + " validation success: " + dataFile.getName() +
                                "; signature file: " + signatureFile.getName());
                    } else {
                        warnAndMaybeThrow("File signature #" + signatureNumber + " validation failed: " + dataFile.getName() +
                                "; signature file: " + signatureFile.getName(), progressCallback, throwOnValidationFailure);
                    }
                }
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

    private static Pair<FiddFileMetadata, MetadataContainer> loadFiddFileMetadata(File fiddFile,
                                                                                   BaseRepositories baseRepositories,
                                                                                   FiddKey.Section fiddFileMetadataSection,
                                                                                   MetadataContainerSerializer metadataContainerSerializer,
                                                                                   ProgressCallback progressCallback
                                                                         ) throws IOException {
        try (SubFileInputStream metadataSectionStream =
                new SubFileInputStream(fiddFile, fiddFileMetadataSection.sectionOffset(), fiddFileMetadataSection.sectionLength())) {
            String encryptionAlgorithmName = fiddFileMetadataSection.encryptionAlgorithm();
            progressCallback.log("Loading and decrypting FiddFileMetadata Section: Encryption Algorithm " + encryptionAlgorithmName);
            byte[] sectionBytes = metadataSectionStream.readAllBytes();
            EncryptionAlgorithm encryptionAlgorithm = baseRepositories.encryptionAlgorithmRepo().get(encryptionAlgorithmName);
            if (encryptionAlgorithm == null) {
                warnAndMaybeThrow("Can't load FiddFileMetadata Encryption algorithm " + encryptionAlgorithmName +
                        " not supported. Can't Proceed!", progressCallback, true);
            }

            byte[] metadataContainerBytes = checkNotNull(encryptionAlgorithm).decrypt(fiddFileMetadataSection.encryptionKeyData(), sectionBytes);
            progressCallback.log("FiddFileMetadata Section decrypted successfully");

            progressCallback.log("Loading FiddFileMetadataContainer using format: " + metadataContainerSerializer.name());
            MetadataContainerSerializer.MetadataContainerAndLength metadataContainer =
                    metadataContainerSerializer.deserialize(metadataContainerBytes);

            String fiddFileMetadataFormat = metadataContainer.metadataContainer().metadataFormat();
            progressCallback.log("Loading FiddFileMetadata using format: " + fiddFileMetadataFormat);
            FiddFileMetadataSerializer fiddFileMetadataSerializer =
                    baseRepositories.fiddFileMetadataFormatRepo().get(fiddFileMetadataFormat);
            if (fiddFileMetadataSerializer == null) {
                String errorText = "Unsupported Format for FiddFileMetadata: " + fiddFileMetadataFormat + ". Can't proceed!";
                warnAndMaybeThrow(errorText, progressCallback, true);
            }
            FiddFileMetadata fiddFileMetadata =
                    checkNotNull(fiddFileMetadataSerializer).deserialize(metadataContainer.metadataContainer().metadata());

            progressCallback.log("FiddFileMetadata loaded");
            return Pair.of(fiddFileMetadata, metadataContainer.metadataContainer());
        } catch (NotEnoughBytesException ne) {
            // Shouldn't happen
            throw new RuntimeException(ne);
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
