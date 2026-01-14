package com.fidd.service.wrapper;

import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.FiddKeyUtil;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.service.FiddContentService;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.fidd.core.common.FiddFileMetadataUtil.loadFiddFileMetadata;
import static com.google.common.base.Preconditions.checkNotNull;

public class WrapperFiddContentService implements FiddContentService {
    public final static Logger LOGGER = LoggerFactory.getLogger(WrapperFiddContentService.class);

    // TODO: hardcoding this to "BLOBS" for now
    final static String METADATA_CONTAINER_SERIALIZER_FORMAT = "BLOBS";

    protected final FiddConnector fiddConnector;
    protected final X509Certificate userCert;
    @Nullable PrivateKey userPrivateKey;

    public WrapperFiddContentService(FiddConnector fiddConnector,
                                     X509Certificate userCert, @Nullable PrivateKey userPrivateKey) {
        this.fiddConnector = fiddConnector;
        this.userCert = userCert;
        this.userPrivateKey = userPrivateKey;
    }

    @Override
    public List<Long> getMessageNumbersTail(int count) {
        return fiddConnector.getMessageNumbersTail(count);
    }

    @Override
    public List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive) {
        return fiddConnector.getMessageNumbersBefore(messageNumber, count, inclusive);
    }

    @Override
    public List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                               long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest) {
        return fiddConnector.getMessageNumbersBetween(latestMessage, inclusiveLatest, earliestMessage,
                inclusiveEarliest, count, getLatest);
    }

    @Override
    public @Nullable FiddFileMetadata getFiddFileMetadata(long messageNumber) {
        try {
            // 1. Load FiddKey
            byte[] fiddKeyBytes = FiddKeyUtil.loadFiddKeyBytes(messageNumber, fiddConnector, userCert, userPrivateKey);
            if (fiddKeyBytes == null) { return null; }
            FiddKey fiddKey = FiddKeyUtil.loadFiddKeyFromBytes(fiddKeyBytes);
            if (fiddKey == null) { return null; }

            // 2. Load FiddFileMetadata Section
            FiddKey.Section fiddFileMetadataSection = checkNotNull(fiddKey.fiddFileMetadata());
            Pair<FiddFileMetadata, MetadataContainer> fiddFileMetadataAndContainer =
                    loadFiddFileMetadata(fiddConnector, messageNumber,
                        fiddFileMetadataSection, METADATA_CONTAINER_SERIALIZER_FORMAT);

            return fiddFileMetadataAndContainer.getLeft();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable List<LogicalFileMetadata> getLogicalFiles(long messageNumber) {
        return List.of();
    }

    @Override
    public @Nullable InputStream readLogicalFile(long messageNumber, String filePath) {
        return null;
    }

    @Override
    public @Nullable InputStream readLogicalFileChunk(long messageNumber, String filePath, long offset, long length) {
        return null;
    }
}
