package com.fidd.core.crc;

import com.fidd.core.encryption.EncryptionAlgorithm;

import java.util.zip.Checksum;

import static com.fidd.core.crc.CrcCalculator.toBytes;

public class DefaultCrcCallback implements EncryptionAlgorithm.CrcCallback {
    protected final Checksum checksum;

    public DefaultCrcCallback(Checksum checksum) {
        this.checksum = checksum;
    }

    @Override
    public void write(byte[] b) {
        checksum.update(b);
    }

    @Override
    public void write(int b) {
        checksum.update(b);
    }

    @Override
    public byte[] getCrc() {
        return toBytes(checksum.getValue());
    }
}
