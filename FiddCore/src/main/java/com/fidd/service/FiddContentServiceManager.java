package com.fidd.service;

import javax.annotation.Nullable;
import java.util.List;

public interface FiddContentServiceManager {
    @Nullable FiddContentService getService(String serviceName);
    List<String> getServiceIds();
}
