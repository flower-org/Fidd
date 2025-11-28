package com.fidd.packer.pack;

import com.fidd.base.BaseRepositories;

import javax.annotation.Nullable;
import java.io.File;
import java.security.cert.X509Certificate;
import java.util.List;

public class FiddUnpackManager {
    public interface ProgressCallback {
        void log(String log);
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

                                      boolean ignoreValidationFailures,
                                      boolean validateFiddFileAndFiddKey,
                                      boolean validateCrcsInFiddKey,
                                      boolean validateFiddFileMetadata,
                                      boolean validateLogicalFileMetadatas,
                                      boolean validateLogicalFiles,

                                      PublicKeySource publicKeySource,
                                      @Nullable X509Certificate currentCert,

                                      ProgressCallback progressCallback) {
        //
    }
}
