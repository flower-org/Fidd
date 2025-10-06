package com.fidd.base;

import com.fidd.core.NamedEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class MapRepository<T extends NamedEntry> implements Repository<T> {
    private final Map<String, T> registry;

    MapRepository(List<T> entries) {
        HashMap<String, T> registry = new HashMap<>();
        for (T entry : entries) {
            registry.put(entry.name(), entry);
        }
        this.registry = Map.copyOf(registry);
    }

    @Override
    public @Nullable T get(String name) {
        return registry.get(name);
    }

    @Override
    public boolean exists(String name) {
        return registry.containsKey(name);
    }

    @Override
    public Collection<String> listEntryNames() {
        return registry.keySet();
    }
}
