package com.fidd.connectors.ydisk;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.FiddConnectorFactory;

import java.net.URL;

public class YandexDiskConnectorFactory implements FiddConnectorFactory {
    @Override
    public FiddConnector createConnector(URL source) {
        try {
            return new YandexDiskFiddConnector(source);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String name() {
        return "YANDEX_DISK";
    }
}
