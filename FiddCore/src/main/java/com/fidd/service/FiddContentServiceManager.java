package com.fidd.service;

import javax.annotation.Nullable;

public interface FiddContentServiceManager {
    @Nullable FiddContentService getService(String serviceName);
}
