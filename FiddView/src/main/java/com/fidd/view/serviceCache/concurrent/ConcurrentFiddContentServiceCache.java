package com.fidd.view.serviceCache.concurrent;

import com.fidd.service.FiddContentService;
import com.fidd.view.serviceCache.FiddContentServiceCache;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentFiddContentServiceCache implements FiddContentServiceCache {
    protected final Map<String, FiddContentService> serviceMap;

    public ConcurrentFiddContentServiceCache() {
        this.serviceMap = new ConcurrentHashMap<>();
    }

    public @Nullable FiddContentService getService(String serviceName) {
        return serviceMap.get(serviceName);
    }

    public boolean addServiceIfAbsent(String serviceName, FiddContentService service) {
        return serviceMap.putIfAbsent(serviceName, service) == null;
    }

    public void removeService(String serviceName) {
        serviceMap.remove(serviceName);
    }

    @Override
    public boolean containsService(String serviceName) {
        return serviceMap.containsKey(serviceName);
    }

    @Override
    public void clear() {
        serviceMap.clear();
    }
}
