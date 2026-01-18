package com.fidd.core.common;

import java.io.*;

public class SubFileInputStream {
    public static InputStream of(File file, long offset, long length) throws IOException {
        FileInputStream fis = new FileInputStream(file);
        return new SubInputStream(fis, offset, length);
    }
}
