package com.fidd.connectors;

import java.io.InputStream;

public interface FiddCacheConnector extends FiddConnector {
    InputStream getFiddMessageChunk(long messageNumber, long offset, long length, boolean cache);
}
