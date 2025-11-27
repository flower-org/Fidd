package com.fidd.core.metadata;

import javax.annotation.Nullable;

public class NotEnoughBytesException extends Exception {
    @Nullable private final Long neededBytes; // optional

    public NotEnoughBytesException() {
        super("Not enough bytes available");
        this.neededBytes = null;
    }

    public NotEnoughBytesException(String message) {
        super(message);
        this.neededBytes = null;
    }

    public NotEnoughBytesException(long neededBytes) {
        super("Not enough bytes available, needed: " + neededBytes);
        this.neededBytes = neededBytes;
    }

    public NotEnoughBytesException(String message, long neededBytes) {
        super(message);
        this.neededBytes = neededBytes;
    }

    public NotEnoughBytesException(String message, Throwable cause) {
        super(message, cause);
        this.neededBytes = null;
    }

    public NotEnoughBytesException(String message, long neededBytes, Throwable cause) {
        super(message, cause);
        this.neededBytes = neededBytes;
    }

    /**
     * @return how many bytes were needed, or null if not specified
     */
    @Nullable
    public Long getNeededBytes() {
        return neededBytes;
    }
}