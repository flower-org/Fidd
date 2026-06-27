package com.fidd.data.scraper;

import com.fidd.connectors.FiddConnector;
import com.fidd.data.model.Fidd;
import com.fidd.data.model.StorageAccount;

public class MySqlFiddScraper {
    protected final FiddConnector source;
    protected final StorageAccount storageAccount;

    public MySqlFiddScraper(FiddConnector source, StorageAccount storageAccount) {
        this.source = source;
        this.storageAccount = storageAccount;
    }

    public void scrape() {
    }

    // TODO: fill DB structures:
    //  1) Fidd, FiddTag-s (from fidd.info)
    //  2) FiddKey-s
    //  3) UnencryptedFiddKey
    //  4) Signature-s
    //  ---
    //  5) Message-s, MetadataChunk-s (from fidd.meta)
    //  6) StorageAccount?

    protected Fidd createFidd() {

        Fidd fidd = new Fidd();
    }
}
