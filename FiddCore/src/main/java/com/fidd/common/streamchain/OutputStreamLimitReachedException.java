package com.fidd.common.streamchain;

public class OutputStreamLimitReachedException extends RuntimeException {
    public OutputStreamLimitReachedException(String message) {
        super(message);
    }
}
