package com.fidd.view.serviceCache;

import com.fidd.service.FiddContentService;
import com.fidd.service.FiddContentServiceManager;

import javax.annotation.Nullable;

public interface FiddContentServiceCache extends FiddContentServiceManager {
    @Nullable FiddContentService getService(String serviceName);
    boolean addServiceIfAbsent(String serviceName, FiddContentService service);
    void removeService(String serviceName);
    boolean containsService(String serviceName);
    void clear();
}
