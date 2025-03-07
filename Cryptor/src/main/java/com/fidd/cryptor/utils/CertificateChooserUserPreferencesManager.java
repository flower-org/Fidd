package com.fidd.cryptor.utils;

import org.apache.commons.lang3.StringUtils;

import java.util.prefs.Preferences;

public class CertificateChooserUserPreferencesManager {
    final static String PKCS11_LIBRARY_PATH = "flowerCertificateChooserPkcs11LibraryPath";
    final static String PKCS11_CERTIFICATE_ALIAS = "flowerCertificateChooserPkcs11CertificateAlias";
    final static String PKCS11_PRIVATE_KEY_ALIAS = "flowerCertificateChooserPkcs11PrivateKeyAlias";

    final static String FILE_CERTIFICATE = "flowerCertificateChooserFileCertificate";
    final static String FILE_PRIVATE_KEY = "flowerCertificateChooserFilePrivateKey";

    final static String RAW_CERTIFICATE = "flowerCertificateChooserRawCertificate";
    final static String RAW_PRIVATE_KEY = "flowerCertificateChooserRawPrivateKey";

    final static String AES_KEY = "flowerCertificateChooserAesKey";
    final static String AES_IV_CHECKBOX = "flowerCertificateChooserAesIvCheckbox";
    final static String AES_IV = "flowerCertificateChooserAesIv";

    public static void updateUserPreferences(String pkcs11LibraryPath, String pkcs11CertificateAlias,
                                             String pkcs11PrivateKeyAlias,
                                             String fileCertificate, String filePrivateKey,
                                             String rawCertificate, String rawPrivateKey,
                                             String aesKey, String aesIv, String aesIvCheckbox) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, PKCS11_LIBRARY_PATH, pkcs11LibraryPath);
        updateUserPreference(userPreferences, PKCS11_CERTIFICATE_ALIAS, pkcs11CertificateAlias);
        updateUserPreference(userPreferences, PKCS11_PRIVATE_KEY_ALIAS, pkcs11PrivateKeyAlias);

        updateUserPreference(userPreferences, FILE_CERTIFICATE, fileCertificate);
        updateUserPreference(userPreferences, FILE_PRIVATE_KEY, filePrivateKey);

        updateUserPreference(userPreferences, RAW_CERTIFICATE, rawCertificate);
        updateUserPreference(userPreferences, RAW_PRIVATE_KEY, rawPrivateKey);

        updateUserPreference(userPreferences, AES_KEY, aesKey);
        updateUserPreference(userPreferences, AES_IV_CHECKBOX, aesIvCheckbox);
        updateUserPreference(userPreferences, AES_IV, aesIv);
    }

    public static void updateUserPreferences(String rawCertificate, String rawPrivateKey) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, RAW_CERTIFICATE, rawCertificate);
        updateUserPreference(userPreferences, RAW_PRIVATE_KEY, rawPrivateKey);
    }

    public static void updatePkcs11UserPreferences(String pkcs11LibraryPath, String pkcs11CertificateAlias,
                                             String pkcs11PrivateKeyAlias) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, PKCS11_LIBRARY_PATH, pkcs11LibraryPath);
        updateUserPreference(userPreferences, PKCS11_CERTIFICATE_ALIAS, pkcs11CertificateAlias);
        updateUserPreference(userPreferences, PKCS11_PRIVATE_KEY_ALIAS, pkcs11PrivateKeyAlias);
    }

    public static void updateFileUserPreferences(String fileCertificate, String filePrivateKey) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, FILE_CERTIFICATE, fileCertificate);
        updateUserPreference(userPreferences, FILE_PRIVATE_KEY, filePrivateKey);
    }

    public static void updateAesUserPreferences(String aesKey, String aesIv, String aesIvCheckbox) {
        Preferences userPreferences = Preferences.userRoot();

        updateUserPreference(userPreferences, AES_KEY, aesKey);
        updateUserPreference(userPreferences, AES_IV_CHECKBOX, aesIvCheckbox);
        updateUserPreference(userPreferences, AES_IV, aesIv);
    }
    public static void updateUserPreference(Preferences userPreferences, String key, String newValue) {
        String oldValue = userPreferences.get(key, "");
        if (!newValue.equals(oldValue)) {
            userPreferences.put(key, StringUtils.defaultIfBlank(newValue, ""));
        }
    }

    public static String getUserPreference(String key) {
        Preferences userPreferences = Preferences.userRoot();
        return userPreferences.get(key, "");
    }

    public static String pkcs11LibraryPath() { return getUserPreference(PKCS11_LIBRARY_PATH); }
    public static String pkcs11CertificateAlias() { return getUserPreference(PKCS11_CERTIFICATE_ALIAS); }
    public static String pkcs11PrivateKeyAlias() { return getUserPreference(PKCS11_PRIVATE_KEY_ALIAS); }
    public static String fileCertificate() { return getUserPreference(FILE_CERTIFICATE); }
    public static String filePrivateKey() { return getUserPreference(FILE_PRIVATE_KEY); }
    public static String rawCertificate() { return getUserPreference(RAW_CERTIFICATE); }
    public static String rawPrivateKey() { return getUserPreference(RAW_PRIVATE_KEY); }
    public static String aesKey() { return getUserPreference(AES_KEY); }
    public static String aesIvCheckbox() { return getUserPreference(AES_IV_CHECKBOX); }
    public static String aesIv() { return getUserPreference(AES_IV); }
}
