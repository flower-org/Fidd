package com.fidd.core.fiddkey;

import javax.annotation.Nullable;
import java.util.List;

public interface FiddKey {
    interface Section {
        long sectionOffset();
        long sectionLength();

        @Nullable String encryptionAlgorithm();
        /** Since for different algorithms this could be a key of varied length, or key + IV, it's algo-specific */
        @Nullable byte[] encryptionKeyData();

        @Nullable String crcAlgorithm();
        @Nullable byte[] crc();
    }

    interface LogicalFileSection extends Section {
        String filePath();
    }

    Section fiddFileMetadata();
    List<LogicalFileSection> logicalFiles();

    // Author's signature of the whole Fidd File (package integrity)
    @Nullable String authorsFiddFileSignatureFormat();
    @Nullable byte[] authorsFiddFileSignature();
}
