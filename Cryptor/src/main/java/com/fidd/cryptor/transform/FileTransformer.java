package com.fidd.cryptor.transform;

import java.io.File;

public interface FileTransformer {
    void transform(File input, File output);
}
