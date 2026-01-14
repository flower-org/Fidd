package com.fidd.core.common;

import com.fidd.base.DefaultBaseRepositories;
import com.fidd.base.Repository;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.fiddkey.FiddKeySerializer;
import com.flower.crypt.HybridAesEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.google.common.base.Preconditions.checkNotNull;

public class FiddKeyUtil {
    public final static Logger LOGGER = LoggerFactory.getLogger(FiddKeyUtil.class);
    public static final Repository<FiddKeySerializer> FIDD_KEY_FORMAT_REPO = new DefaultBaseRepositories().fiddKeyFormatRepo();

    public static @Nullable byte[] loadFiddKeyBytes(long messageNumber, FiddConnector fiddConnector,
                                                X509Certificate userCert, @Nullable PrivateKey privateKey) throws Exception {
        long messageLength = fiddConnector.getFiddMessageSize(messageNumber);
        String footprint = FiddKeyLookup.createLookupFootprint(userCert, messageNumber, messageLength);
        List<byte[]> candidates = fiddConnector.getFiddKeyCandidates(messageNumber, footprint.getBytes(StandardCharsets.UTF_8));
        for (byte[] candidate : candidates) {
            try {
                byte[] encryptedKey = fiddConnector.getFiddKey(messageNumber, candidate);
                ByteArrayInputStream inputStream = new ByteArrayInputStream(encryptedKey);
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

                // TODO: this decryption algo should also be made configurable
                // TODO: also there is an assumption in the code that this decryption can't result in false positives
                //  so if something like XOR is used it should be accompanied with CRC
                // Decrypt using private key
                HybridAesEncryptor.decrypt(inputStream, outputStream, HybridAesEncryptor.Mode.PUBLIC_KEY_ENCRYPT,
                        privateKey, null, null);

                return outputStream.toByteArray();
            } catch (Exception e) {
                LOGGER.error("Failed to decrypt candidate {}", new String(candidate), e);
            }
        }
        return null;
    }

    public static @Nullable FiddKey loadFiddKeyFromBytes(byte[] fiddKeyBytes) {
        LOGGER.info("Detecting FiddKey format.");
        for (String fiddKeyFormat : FIDD_KEY_FORMAT_REPO.listEntryNames()) {
            FiddKeySerializer serializer = FIDD_KEY_FORMAT_REPO.get(fiddKeyFormat);
            try {
                FiddKey fiddKey = checkNotNull(serializer).deserialize(fiddKeyBytes);
                LOGGER.info("FiddKey format: `" + fiddKeyFormat + "` - successfully deserialized FiddKey.");
                return fiddKey;
            } catch (Exception e) {
                LOGGER.error("FiddKey format: `" + fiddKeyFormat + "` - failed to deserialize FiddKey.", e);
            }
        }
        LOGGER.info("Failed to deserialize FiddKey");
        return null;
    }
}
