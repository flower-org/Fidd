package com.fidd.cryptor.transform;

import java.io.File;

public interface FileSignatureChecker {
    boolean checkSignature(File text, byte[] signature);
    boolean checkSignature(File text, File signature);
}
