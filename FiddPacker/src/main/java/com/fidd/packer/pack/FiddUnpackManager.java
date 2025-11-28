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

                                      @Nullable X509Certificate currentCert,

                                      ProgressCallback progressCallback) {
        //
    }
}
