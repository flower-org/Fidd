package com.fidd.core.pki.sha256WithRsa;

import com.fidd.core.pki.SignerChecker;
import com.flower.crypt.PkiUtil;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class SHA256WithRSASignerChecker implements SignerChecker {
    @Override
    public byte[] signData(byte[] data, PrivateKey privateKey) {
        try {
            return PkiUtil.signDataQuick(data, privateKey);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verifySignature(byte[] data, byte[] sign, PublicKey publicKey) {
        try {
            return PkiUtil.verifySignature(data, sign, publicKey);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] signData(InputStream data, PrivateKey privateKey) {
        try {
            return PkiUtil.signDataQuick(data, privateKey);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean verifySignature(InputStream data, byte[] sign, PublicKey publicKey) {
        try {
            return PkiUtil.verifySignature(data, sign, publicKey);
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "SHA256WithRSA";
    }
}
