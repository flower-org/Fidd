package com.fidd.cryptor.keys;

import javax.annotation.Nullable;

public interface KeyProvider {
    //TODO: unmake this Nullable if possible
    @Nullable KeyContext geKeyContext();
}
