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
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SignatureException;

public class RsaTransformerProvider implements TransformerProvider {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";

    public enum Mode {
        PUBLIC_KEY_ENCRYPT,
        PRIVATE_KEY_ENCRYPT
    }

    public enum EncryptionFormat {
        RSA,
        RSA_AES_256_HYBRID
    }

    private final PublicKey publicKey;
    private final PrivateKey privateKey;
    private final Mode mode;
    private final EncryptionFormat encryptionFormat;

    public RsaTransformerProvider(PublicKey publicKey, PrivateKey privateKey, Mode mode,
                                  EncryptionFormat encryptionFormat) {
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
}