package com.fidd.base;

import com.fidd.core.NamedEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapRepository<T extends NamedEntry> implements Repository<T> {
    private final List<T> entriesInOriginalOrder;
    private final Map<String, T> registry;
    @Nullable String defaultKey;
    @Nullable T nullEntry;

    public MapRepository(@Nullable String defaultKey, List<T> entries) {
        this(defaultKey, entries, null);
    }

    public MapRepository(@Nullable String defaultKey, List<T> entries, @Nullable T nullEntry) {
        this.defaultKey = defaultKey;
        this.entriesInOriginalOrder = entries;
        HashMap<String, T> registry = new HashMap<>();
        for (T entry : entries) {
            registry.put(entry.name(), entry);
        }
        this.registry = Map.copyOf(registry);
        this.nullEntry = nullEntry;
    }

    @Override
    public @Nullable String defaultKey() {
        return defaultKey;
    }

    @Override
    public @Nullable T get(@Nullable String name) {
        if (name == null) {
            return nullEntry;
        }
        return registry.get(name);
    }

    @Override
    public boolean exists(String name) {
        return registry.containsKey(name);
    }

    @Override
    public Collection<String> listEntryNames() {
        return entriesInOriginalOrder.stream().map(NamedEntry::name).toList();
    }
}
