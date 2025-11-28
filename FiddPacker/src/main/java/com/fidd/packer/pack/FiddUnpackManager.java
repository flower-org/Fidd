package com.fidd.packer.pack;

import com.fidd.base.BaseRepositories;
import com.fidd.base.Repository;
import com.fidd.core.common.FiddSignature;
import com.fidd.core.crc.CrcCalculator;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddUnpackManager {
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
                for (FiddKey.Section logicalFileSection : fiddKey.logicalFiles()) {
                    validateCrcs(fiddFile, baseRepositories, 1, logicalFileSection,
                            progressCallback, throwOnValidationFailure);
                }
            }
        }


        // 3. Load FiddFileMetadata
        // 4. Try to get Public Key from FiddFileMetadata
    }

    private static boolean hasCrcs(FiddKey.Section section) {
        return section.crcs() != null && !section.crcs().isEmpty();
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
        SubFileInputStream sectionStream = new SubFileInputStream(fiddFile, sectionOffset, sectionLength);
        byte[] calcCrc = crcCalculator.calculateCrc(sectionStream);
        return Arrays.equals(crc, calcCrc);
    }
}
