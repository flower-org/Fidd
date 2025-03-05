package com.fidd.cryptor.transform;

import javax.annotation.Nullable;

public interface TransformerProvider {
    @Nullable Transformer getEncryptTransformer();
    @Nullable Transformer getDecryptTransformer();
    @Nullable Transformer getSignTransformer();
    @Nullable SignatureChecker getSignatureChecker();

    static TransformerProvider of(
        @Nullable Transformer encryptTransformer,
        @Nullable Transformer decryptTransformer,
        @Nullable Transformer signTransformer,
        @Nullable SignatureChecker signatureChecker
    ) {
        return new TransformerProvider() {
            @Nullable @Override public Transformer getEncryptTransformer() { return encryptTransformer; }
            @Nullable @Override public Transformer getDecryptTransformer() { return decryptTransformer; }
            @Nullable @Override public Transformer getSignTransformer() { return signTransformer; }
            @Nullable @Override public SignatureChecker getSignatureChecker() { return signatureChecker; }
        };
    }
}
