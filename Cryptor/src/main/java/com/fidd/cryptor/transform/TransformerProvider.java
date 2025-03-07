package com.fidd.cryptor.transform;

import javax.annotation.Nullable;

public interface TransformerProvider {
    @Nullable Transformer getEncryptTransformer();
    @Nullable Transformer getDecryptTransformer();
    @Nullable Transformer getSignTransformer();
    @Nullable SignatureChecker getSignatureChecker();

    @Nullable FileTransformer getEncryptFileTransformer();
    @Nullable FileTransformer getDecryptFileTransformer();
    @Nullable FileToByteTransformer getSignFileTransformer();
    @Nullable FileSignatureChecker getFileSignatureChecker();

    @Nullable CsrSigner getCsrSigner();
    @Nullable CertificateVerifier getCertificateVerifier();

    static TransformerProvider of(
        @Nullable Transformer encryptTransformer,
        @Nullable Transformer decryptTransformer,
        @Nullable Transformer signTransformer,
        @Nullable SignatureChecker signatureChecker,

        @Nullable FileTransformer encryptFileTransformer,
        @Nullable FileTransformer decryptFileTransformer,
        @Nullable FileToByteTransformer signFileTransformer,
        @Nullable FileSignatureChecker fileSignatureChecker,

        @Nullable CsrSigner csrSigner,
        @Nullable CertificateVerifier certificateVerifier
    ) {
        return new TransformerProvider() {
            @Nullable @Override public Transformer getEncryptTransformer() { return encryptTransformer; }
            @Nullable @Override public Transformer getDecryptTransformer() { return decryptTransformer; }
            @Nullable @Override public Transformer getSignTransformer() { return signTransformer; }
            @Nullable @Override public SignatureChecker getSignatureChecker() { return signatureChecker; }

            @Nullable @Override public FileTransformer getEncryptFileTransformer() { return encryptFileTransformer; }
            @Nullable @Override public FileTransformer getDecryptFileTransformer() { return decryptFileTransformer; }
            @Nullable @Override public FileToByteTransformer getSignFileTransformer() { return signFileTransformer; }
            @Nullable @Override public FileSignatureChecker getFileSignatureChecker() { return fileSignatureChecker; }

            @Nullable @Override public CsrSigner getCsrSigner() { return csrSigner; }
            @Nullable @Override public CertificateVerifier getCertificateVerifier() { return certificateVerifier; }
        };
    }
}
