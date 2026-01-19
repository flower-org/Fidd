package com.fidd.view.serviceCache;

import com.fidd.service.FiddContentService;

import javax.annotation.Nullable;

public interface FiddContentServiceCache {
    @Nullable FiddContentService getService(String serviceName);
    boolean addServiceIfAbsent(String serviceName, FiddContentService service);
    void removeService(String serviceName);
    boolean containsService(String serviceName);
}
