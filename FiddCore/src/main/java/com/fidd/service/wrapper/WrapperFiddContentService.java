package com.fidd.service.wrapper;

import com.fidd.base.BaseRepositories;
import com.fidd.connectors.FiddConnector;
import com.fidd.core.common.FiddKeyUtil;
import com.fidd.core.common.LogicalFileMetadataUtil;
import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.fiddkey.FiddKey;
import com.fidd.core.logicalfile.LogicalFileMetadata;
import com.fidd.core.metadata.MetadataContainer;
import com.fidd.core.metadata.MetadataContainerSerializer;
import com.fidd.service.FiddContentService;
import com.fidd.service.LogicalFileInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;

import static com.fidd.core.common.FiddFileMetadataUtil.loadFiddFileMetadata;
import static com.fidd.core.common.LogicalFileUtil.getLogicalFileInputStream;
import static com.fidd.core.common.LogicalFileUtil.getLogicalFileInputStreamChunk;
import static com.google.common.base.Preconditions.checkNotNull;

public class WrapperFiddContentService implements FiddContentService {
    public final static Logger LOGGER = LoggerFactory.getLogger(WrapperFiddContentService.class);

    // TODO: hardcoding this to "BLOBS" for now
    final static String METADATA_CONTAINER_SERIALIZER_FORMAT = "BLOBS";

    protected final BaseRepositories baseRepositories;
    protected final FiddConnector fiddConnector;
    protected final X509Certificate userCert;
    protected final PrivateKey userPrivateKey;

    public WrapperFiddContentService(BaseRepositories baseRepositories, FiddConnector fiddConnector,
                                     X509Certificate userCert, PrivateKey userPrivateKey) {
        this.baseRepositories = baseRepositories;
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

    protected @Nullable FiddKey loadFiddKey(long messageNumber) throws Exception {
        byte[] fiddKeyBytes = FiddKeyUtil.loadFiddKeyBytes(baseRepositories, messageNumber, fiddConnector, userCert, userPrivateKey);
        if (fiddKeyBytes == null) {
            fiddKeyBytes = FiddKeyUtil.loadDefaultFiddKeyBytes(messageNumber, fiddConnector);
        }
        if (fiddKeyBytes == null) { return null; }
        return FiddKeyUtil.loadFiddKeyFromBytes(baseRepositories, fiddKeyBytes);
    }

    @Override
    public @Nullable FiddFileMetadata getFiddFileMetadata(long messageNumber) {
        try {
            // 1. Load FiddKey
            FiddKey fiddKey = loadFiddKey(messageNumber);
            if (fiddKey == null) { return null; }

            // 2. Load FiddFileMetadata Section
            FiddKey.Section fiddFileMetadataSection = fiddKey.fiddFileMetadata();
            Pair<FiddFileMetadata, MetadataContainer> fiddFileMetadataAndContainer =
                    loadFiddFileMetadata(baseRepositories, fiddConnector, messageNumber,
                        fiddFileMetadataSection, METADATA_CONTAINER_SERIALIZER_FORMAT);

            return fiddFileMetadataAndContainer.getLeft();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable List<LogicalFileInfo> getLogicalFileInfos(long messageNumber) {
        try {
            // 1. Load FiddKey
            FiddKey fiddKey = loadFiddKey(messageNumber);
            if (fiddKey == null) { return null; }

            // 2. Load LogicalFileInfo Sections
            List<LogicalFileInfo> logicalFileInfo = new ArrayList<>();
            for (int i = 0; i < fiddKey.logicalFiles().size(); i++) {
                LOGGER.info("Getting LogicalFileMetadata for Section #" + (i+1) + " (Logical File #" + i + ")");
                FiddKey.Section logicalFileSection = fiddKey.logicalFiles().get(i);
                Pair<LogicalFileMetadata, MetadataContainerSerializer.MetadataContainerAndLength> logicalFileMetadataAndContainer =
                     LogicalFileMetadataUtil.getLogicalFileMetadata(baseRepositories,
                            fiddConnector, messageNumber,
                            logicalFileSection);

                logicalFileInfo.add(LogicalFileInfo.of(checkNotNull(logicalFileMetadataAndContainer).getLeft(),
                        logicalFileSection,
                        logicalFileMetadataAndContainer.getRight().lengthBytes()
                    ));
            }

            return logicalFileInfo;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable InputStream readLogicalFile(long messageNumber, LogicalFileInfo LogicalFileInfo) {
        try {
            return getLogicalFileInputStream(baseRepositories, fiddConnector,
                    messageNumber, LogicalFileInfo.section(), LogicalFileInfo.fileOffset());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public @Nullable InputStream readLogicalFileChunk(long messageNumber, LogicalFileInfo LogicalFileInfo, long offset, long length) {
        try {
            return getLogicalFileInputStreamChunk(baseRepositories, fiddConnector,
                    messageNumber, LogicalFileInfo.section(), LogicalFileInfo.fileOffset(), offset, length);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
