package com.fidd.data.scraper;

import com.fidd.connectors.FiddConnector;

public class MySqlFiddScraper {
    protected final FiddConnector source;

    public MySqlFiddScraper(FiddConnector source) {
        this.source = source;
    }

    // TODO: fill DB structures:
    //  1) Fidd, FiddTag (from fidd.info)
    //  2) FiddKey
    //  3) UnencryptedFiddKey
    //  4) Signature
    //  ---
    //  5) MetadataChunk (from fidd.meta)
    //  6) Message
    //  7) StorageAccount?
}
