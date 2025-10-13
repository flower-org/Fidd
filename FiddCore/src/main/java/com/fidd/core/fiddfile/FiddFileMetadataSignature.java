package com.fidd.core.fiddfile;

import javax.annotation.Nullable;

public interface FiddFileMetadataSignature {
    @Nullable String authorsMetadataSignatureFormat();
    @Nullable byte[] authorsMetadataSignature();
}
