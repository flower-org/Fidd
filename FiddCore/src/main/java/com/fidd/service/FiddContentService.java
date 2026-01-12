package com.fidd.service;

import com.fidd.core.fiddfile.FiddFileMetadata;
import com.fidd.core.logicalfile.LogicalFileMetadata;

import java.io.InputStream;
import java.util.List;

public interface FiddContentService {
    /** Descending order */
    List<Long> getMessageNumbersTail(int count);
    /** Descending order */
    List<Long> getMessageNumbersBefore(long messageNumber, int count, boolean inclusive);
    /** Descending order */
    List<Long> getMessageNumbersBetween(long latestMessage, boolean inclusiveLatest,
                                        long earliestMessage, boolean inclusiveEarliest, int count, boolean getLatest);

    FiddFileMetadata getFiddFileMetadata(long messageNumber);
    List<LogicalFileMetadata> getLogicalFiles(long messageNumber);

    InputStream readLogicalFile(long messageNumber, String filePath);
    InputStream readLogicalFileChunk(long messageNumber, String filePath, long offset, long length);

    // ---------------------------------------------
    //    TODO: Validations; CRC; progressive CRC
    // ---------------------------------------------
}
