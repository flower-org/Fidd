package com.fidd.base;

import javax.annotation.Nullable;
import java.util.Collection;

public interface Repository<T> {
    /**
     * Returns the entry associated with the given name.
     */
    @Nullable T get(String name);

    /**
     * Checks if an entry with the given name exists.
     */
    boolean exists(String name);

    /**
     * Lists all registered names.
     */
    Collection<String> listEntryNames();
}
