package com.fidd.cryptor.transform;

import com.fidd.cryptor.utils.Cryptor;
import com.fidd.cryptor.utils.PkiUtil;

import javax.annotation.Nullable;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.DataInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;
import java.security.cert.X509Certificate;

import static com.fidd.cryptor.utils.PkiUtil.AES;
import static com.fidd.cryptor.utils.PkiUtil.AES_CBC;

public class RsaTransformerProvider implements TransformerProvider {
    private static final String ALGORITHM = AES;
    private static final String TRANSFORMATION = AES_CBC;
    public static final int MAX_RSA_2048_PLAINTEXT_SIZE = 245;
    public static final int MAX_RSA_2048_CIPHERTEXT_SIZE = 256;

    public enum Mode {
        PUBLIC_KEY_ENCRYPT,
        PRIVATE_KEY_ENCRYPT
    }

    public enum EncryptionFormat {
        RSA,
        RSA_AES_256_HYBRID
    }

    private final X509Certificate certificate;
    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final Mode mode;
    private final EncryptionFormat encryptionFormat;

    public RsaTransformerProvider(PublicKey publicKey, PrivateKey privateKey, X509Certificate certificate,
                                  Mode mode,
                                  EncryptionFormat encryptionFormat) {
        this.certificate = certificate;
        this.publicKey = publicKey;
        this.privateKey = privateKey;
        this.mode = mode;
        this.encryptionFormat = encryptionFormat;
    }

    @Override
    @Nullable public Transformer getEncryptTransformer() {
        return input -> {
            if (encryptionFormat == EncryptionFormat.RSA) {
                try {
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        return PkiUtil.encrypt(input, publicKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        return PkiUtil.encrypt(input, privateKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                            BadPaddingException | IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }
            } else if (encryptionFormat == EncryptionFormat.RSA_AES_256_HYBRID) {
                try {
                    byte[] key = Cryptor.generateAESKeyRaw();
                    byte[] iv = Cryptor.generateAESIV();
                    byte[] keyAndIv = concatenateArrays(key, iv);

                    byte[] encryptedKeyIv;
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        encryptedKeyIv = PkiUtil.encrypt(keyAndIv, publicKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        encryptedKeyIv = PkiUtil.encrypt(keyAndIv, privateKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }

                    SecretKeySpec aesKey = new SecretKeySpec(key, ALGORITHM);
                    IvParameterSpec aesIv = new IvParameterSpec(iv);
                    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv);
                    byte[] encryptedData = cipher.doFinal(input);
                    return prependWithKey(encryptedKeyIv, encryptedData);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Unsupported Encryption Format " + encryptionFormat);
            }
        };
    }

    public static byte[] prependWithKey(byte[] key, byte[] data) {
        // Create a new array with the length of the key (4 bytes) + key length + key + other length + other
        byte[] result = new byte[4 + key.length + data.length];

        // Prepend the length of the key (4 bytes) - BigEndian order to match ByteBuffer.putInt(keyLength)
        int keyLength = key.length;
        result[0] = (byte) (keyLength >> 24);
        result[1] = (byte) (keyLength >> 16);
        result[2] = (byte) (keyLength >> 8);
        result[3] = (byte) (keyLength);

        // Copy the key into the result array
        System.arraycopy(key, 0, result, 4, key.length);

        // Copy the other array into the result array after the key
        System.arraycopy(data, 0, result, 4 + key.length, data.length);

        return result;
    }

    static byte[] concatenateArrays(byte[] array1, byte[] array2) {
        byte[] result = new byte[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    @Override
    @Nullable public Transformer getDecryptTransformer() {
        return input -> {
            if (encryptionFormat == EncryptionFormat.RSA) {
                try {
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        return PkiUtil.decrypt(input, privateKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        return PkiUtil.decrypt(input, publicKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         BadPaddingException | IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }
            } else if (encryptionFormat == EncryptionFormat.RSA_AES_256_HYBRID) {
                try {
                    // Read the length of the key (first 4 bytes) - BigEndian order to match ByteBuffer.getInt()
                    int keyLength = ((input[0] & 0xFF) << 24) | ((input[1] & 0xFF) << 16) | ((input[2] & 0xFF) << 8) | (input[3] & 0xFF);
                    byte[] encryptedKeyIv = new byte[keyLength];
                    System.arraycopy(input, 4, encryptedKeyIv, 0, keyLength);

                    byte[] keyIv;
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        keyIv = PkiUtil.decrypt(encryptedKeyIv, privateKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        keyIv = PkiUtil.decrypt(encryptedKeyIv, publicKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }

                    // Separate the key and IV
                    byte[] key = new byte[32];
                    byte[] iv = new byte[16];

                    System.arraycopy(keyIv, 0, key, 0, 32);
                    System.arraycopy(keyIv, 32, iv, 0, 16);

                    // get and decrypt data
                    int dataLength = input.length - (4 + keyLength);
                    byte[] data = new byte[dataLength];
                    System.arraycopy(input, 4 + keyLength, data, 0, dataLength);

                    SecretKeySpec aesKey = new SecretKeySpec(key, ALGORITHM);
                    IvParameterSpec aesIv = new IvParameterSpec(iv);

                    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.DECRYPT_MODE, aesKey, aesIv);
                    return cipher.doFinal(data);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Unsupported Encryption Format " + encryptionFormat);
            }
        };
    }

    @Override
    @Nullable public Transformer getSignTransformer() {
        return input -> {
            try {
                return PkiUtil.signData(input, privateKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    @Nullable public SignatureChecker getSignatureChecker() {
        return (data, signature) -> {
            try {
                return PkiUtil.verifySignature(data, signature, publicKey);
            } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable
    @Override
    public FileTransformer getEncryptFileTransformer() {
        return (inputFile, outputFile) -> {
            if (encryptionFormat == EncryptionFormat.RSA) {
                try {
                    byte[] input = Files.readAllBytes(inputFile.toPath());
                    byte[] output;
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        output = PkiUtil.encrypt(input, publicKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        output = PkiUtil.encrypt(input, privateKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }
                    Files.write(outputFile.toPath(), output);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         BadPaddingException | IllegalBlockSizeException | IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (encryptionFormat == EncryptionFormat.RSA_AES_256_HYBRID) {
                try {
                    byte[] key = Cryptor.generateAESKeyRaw();
                    byte[] iv = Cryptor.generateAESIV();
                    byte[] keyAndIv = concatenateArrays(key, iv);

                    byte[] encryptedKeyIv;
                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        encryptedKeyIv = PkiUtil.encrypt(keyAndIv, publicKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        encryptedKeyIv = PkiUtil.encrypt(keyAndIv, privateKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }

                    byte[] encryptedKeyIvWithLength = new byte[4 + encryptedKeyIv.length];

                    // Prepend the length of the key (4 bytes) - BigEndian order to match ByteBuffer.putInt(keyLength)
                    int encryptedKeyIvLength = encryptedKeyIv.length;
                    encryptedKeyIvWithLength[0] = (byte) (encryptedKeyIvLength >> 24);
                    encryptedKeyIvWithLength[1] = (byte) (encryptedKeyIvLength >> 16);
                    encryptedKeyIvWithLength[2] = (byte) (encryptedKeyIvLength >> 8);
                    encryptedKeyIvWithLength[3] = (byte) (encryptedKeyIvLength);

                    // Copy the key into the result array
                    System.arraycopy(encryptedKeyIv, 0, encryptedKeyIvWithLength, 4,
                            encryptedKeyIv.length);

                    SecretKeySpec aesKey = new SecretKeySpec(key, ALGORITHM);
                    IvParameterSpec aesIv = new IvParameterSpec(iv);
                    Cipher cipher = Cipher.getInstance(TRANSFORMATION);
                    cipher.init(Cipher.ENCRYPT_MODE, aesKey, aesIv);

                    try (FileInputStream fis = new FileInputStream(inputFile);
                         FileOutputStream fos = new FileOutputStream(outputFile)) {
                        fos.write(encryptedKeyIvWithLength);
                        PkiUtil.encryptFile(aesKey, aesIv, fis, fos, (int)inputFile.length());
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException |
                         IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Unsupported Encryption Format " + encryptionFormat);
            }
        };
    }

    @Nullable
    @Override
    public FileTransformer getDecryptFileTransformer() {
        return (inputFile, outputFile) -> {
            if (encryptionFormat == EncryptionFormat.RSA) {
                try {
                    byte[] input = Files.readAllBytes(inputFile.toPath());
                    byte[] output;

                    if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                        output = PkiUtil.decrypt(input, privateKey);
                    } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                        output = PkiUtil.decrypt(input, publicKey);
                    } else {
                        throw new RuntimeException("Unsupported mode " + mode);
                    }
                    Files.write(outputFile.toPath(), output);
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         BadPaddingException | IllegalBlockSizeException | IOException e) {
                    throw new RuntimeException(e);
                }
            } else if (encryptionFormat == EncryptionFormat.RSA_AES_256_HYBRID) {
                try {
                    try (FileInputStream fis = new FileInputStream(inputFile);
                         FileOutputStream fos = new FileOutputStream(outputFile);
                         DataInputStream dis = new DataInputStream(fis)) {
                        int keyIvLength = dis.readInt();
                        byte[] encryptedKeyIv = new byte[keyIvLength];
                        int keyIvBytesRead = fis.read(encryptedKeyIv);

                        byte[] keyIv;
                        if (mode == Mode.PUBLIC_KEY_ENCRYPT) {
                            keyIv = PkiUtil.decrypt(encryptedKeyIv, privateKey);
                        } else if (mode == Mode.PRIVATE_KEY_ENCRYPT) {
                            keyIv = PkiUtil.decrypt(encryptedKeyIv, publicKey);
                        } else {
                            throw new RuntimeException("Unsupported mode " + mode);
                        }

                        // Separate the key and IV
                        byte[] key = new byte[32];
                        byte[] iv = new byte[16];

                        System.arraycopy(keyIv, 0, key, 0, 32);
                        System.arraycopy(keyIv, 32, iv, 0, 16);

                        SecretKeySpec aesKey = new SecretKeySpec(key, ALGORITHM);
                        IvParameterSpec aesIv = new IvParameterSpec(iv);

                        int dataLength = (int) (inputFile.length() - (encryptedKeyIv.length + 4));
                        PkiUtil.decryptFile(aesKey, aesIv, fis, fos, dataLength);
                    }
                } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException |
                         InvalidAlgorithmParameterException | BadPaddingException | IllegalBlockSizeException |
                         IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("Unsupported Encryption Format " + encryptionFormat);
            }
        };
    }

    @Nullable
    @Override
    public FileToByteTransformer getSignFileTransformer() {
        return input -> {
            try {
                return PkiUtil.signData(input, privateKey);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Nullable
    @Override
    public FileSignatureChecker getFileSignatureChecker() {
        return new FileSignatureChecker() {
            @Override
            public boolean checkSignature(File text, byte[] signature) {
                try {
                    return PkiUtil.verifySignature(text, signature, publicKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public boolean checkSignature(File text, File signature) {
                try {
                    return PkiUtil.verifySignature(text, signature, publicKey);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Nullable
    @Override
    public CsrSigner getCsrSigner() {
        return csr -> PkiUtil.signCsr(csr, privateKey, certificate);
    }

    @Nullable
    @Override
    public CertificateVerifier getCertificateVerifier() {
        return childCertificate -> PkiUtil.verifyCertificateSignature(childCertificate, certificate);
    }
}