package com.fidd.cryptor.transform;

import java.io.File;

public interface FileToByteTransformer {
    byte[] transform(File input);
}
